package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.AtLeast
import io.kudos.base.bean.validation.constraint.annotations.Constraints
import io.kudos.base.bean.validation.support.ValidationContext
import io.kudos.base.bean.validation.support.ValidatorFactory
import io.kudos.base.lang.reflect.getMemberProperty
import io.kudos.base.lang.reflect.getMemberPropertyValue
import io.kudos.base.support.logic.AndOrEnum
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Null
import jakarta.validation.metadata.ConstraintDescriptor
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidator
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.WeakHashMap
import kotlin.reflect.full.declaredMemberProperties

/**
 * Constraints约束验证器
 *
 * @author K
 * @since 1.0.0
 */
class ConstraintsValidator : ConstraintValidator<Constraints, Any?> {

    private lateinit var constraints: Constraints

    override fun initialize(constraints: Constraints) {
        this.constraints = constraints
    }

    override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
        val annotations = getAnnotations(constraints)
        val bean = ValidationContext.get(context)
        if (constraints.andOr == AndOrEnum.AND) {
            val failFast = ValidationContext.isFailFast()
            var pass = true
            annotations.forEach {
                pass = validate(it, value, bean, context)
                if (!pass) {
                    addViolation(context, it)
                    if (failFast) {
                        return false
                    }
                }
            }
            return pass
        } else { // 有一个约束成功就算通过，并且不受failFast影响
            annotations.forEach {
                val pass = validate(it, value, bean, context)
                if (pass) {
                    return true
                }
            }
            return false
        }
    }

    companion object {
        private val hvInitMethodCache = ConcurrentHashMap<Class<*>, Method>()
        private val initMethodCache = ConcurrentHashMap<Class<*>, Method>()
        private val isValidMethodCache = ConcurrentHashMap<Class<*>, Method>()
        private val annotationAttributeMethodsCache = ConcurrentHashMap<Class<out Annotation>, List<Method>>()
        private val annotationMessageMethodCache = ConcurrentHashMap<Class<out Annotation>, Method>()
        private val constraintDescriptorCache = Collections.synchronizedMap(WeakHashMap<Annotation, ConstraintDescriptor<*>>())
        private val excludedConstraintPropertyNames = setOf("order", "andOr", "message", "groups", "payload")
        private val emptyComposingConstraints: Set<ConstraintDescriptor<*>> = emptySet()
        private val emptyValidatorClasses: List<Class<*>> = emptyList()
        private val priorityConstraintTypes = setOf(
            NotBlank::class,
            NotEmpty::class,
            NotNull::class,
            Null::class
        )

        fun getAnnotations(constraints: Constraints): List<Annotation> {
            // 获取定义的子约束
            val annotations = mutableListOf<Annotation>()
            var priorityAnnotation: Annotation? = null // 强制优先NotBlank、NotEmpty、NotNull、Null之一
            constraints.annotationClass.declaredMemberProperties.forEach {
                if (it.name !in excludedConstraintPropertyNames) {
                    val annotation = it.call(constraints) as? Annotation
                        ?: error("Constraints 子约束属性【${it.name}】不是注解类型")
                    val message = getAnnotationMessage(annotation)
                    if (message != Constraints.MESSAGE) {
                        annotations.add(annotation)
                        if (annotation.annotationClass in priorityConstraintTypes) {
                            priorityAnnotation = annotation
                        }
                    }
                }
            }

            // 根据指定的顺序排序子约束
            val result = if (constraints.order.isNotEmpty()) {
                val sequenceAnnotations = linkedSetOf<Annotation>() // 有指定顺序的子约束
                constraints.order.forEach { clazz ->
                    val annotation = annotations.firstOrNull { it.annotationClass == clazz }
                    if (annotation == null) {
                        error("Constraints约束sequence中指定的【$clazz】子约束未定义规则！")
                    } else {
                        sequenceAnnotations.add(annotation)
                    }
                }
                sequenceAnnotations.addAll(annotations) // 合并未指定顺序的子约束
                sequenceAnnotations.toList()
            } else {
                annotations
            }

            // 强制优先NotBlank、NotEmpty、NotNull、Null
            return if (priorityAnnotation != null) {
                val sequenceAnnotations = linkedSetOf<Annotation>()
                sequenceAnnotations.add(priorityAnnotation)
                sequenceAnnotations.addAll(result)
                sequenceAnnotations.toList()
            } else {
                result
            }
        }

        private fun getAnnotationMessage(annotation: Annotation): String {
            val annotationClass = annotation.annotationClass.java
            val method = annotationMessageMethodCache.getOrPut(annotationClass) {
                annotationClass.getDeclaredMethod("message").apply { isAccessible = true }
            }
            return method.invoke(annotation) as? String
                ?: error("注解【${annotation.annotationClass}】的 message 返回值不是 String")
        }
    }

    private fun validate(
        annotation: Annotation,
        value: Any?,
        bean: Any?,
        context: ConstraintValidatorContext
    ): Boolean {
        if (value == null) {
            return annotation.annotationClass != NotNull::class && annotation.annotationClass != NotEmpty::class
                    && annotation.annotationClass != NotBlank::class
        }

        val v = if (annotation is AtLeast) {
            requireNotNull(bean) { "AtLeast 子约束需要从 ValidationContext 获取 bean" }
        } else {
            value
        }
        val validators = ValidatorFactory.getValidator(annotation, v)
        if (validators.isEmpty()) {
            error("Constraints约束不支持【${annotation.annotationClass}】作为其子约束！")
        } else {
            var pass = true
            validators.forEach {
                pass = pass && doValidate(it, annotation, v, context)
            }
            return pass
        }
    }


    private fun doValidate(
        constraintValidator: ConstraintValidator<*, *>,
        annotation: Annotation,
        value: Any?,
        context: ConstraintValidatorContext
    ): Boolean {
        val validatorClass = constraintValidator.javaClass
        if (constraintValidator is HibernateConstraintValidator<*, *>) {
            val initCtx = ValidationContext.getHvInitCtx()
            val descriptor = proxyConstraintDescriptor(annotation)
            val initMethod = hvInitMethodCache.getOrPut(validatorClass) {
                validatorClass.methods.firstOrNull {
                    it.name == "initialize" && it.parameterCount == 2
                } ?: error("无法找到HibernateConstraintValidator.initialize(descriptor, initCtx)方法")
            }
            initMethod.invoke(constraintValidator, descriptor, initCtx)
        } else {
            val initMethod = initMethodCache.getOrPut(validatorClass) {
                validatorClass.methods.firstOrNull {
                    it.name == "initialize" && it.parameterCount == 1
                } ?: error("无法找到ConstraintValidator.initialize(annotation)方法")
            }
            initMethod.invoke(constraintValidator, annotation)
        }

        val isValidMethod = isValidMethodCache.getOrPut(validatorClass) {
            validatorClass.methods.firstOrNull {
                it.name == "isValid" && it.parameterCount == 2
            } ?: error("无法找到ConstraintValidator.isValid(value, context)方法")
        }
        val result = isValidMethod.invoke(constraintValidator, value, context)
        return result as? Boolean
            ?: error("ConstraintValidator.isValid必须返回Boolean")
    }

    private fun addViolation(context: ConstraintValidatorContext, annotation: Annotation) {
        context.disableDefaultConstraintViolation()
        val message = getAnnotationMessage(annotation)
        context.buildConstraintViolationWithTemplate(
            message
        ).addConstraintViolation()
    }

    /**
     * 构造一个“够用”的 ConstraintDescriptor 代理对象，用于手动初始化 HibernateConstraintValidator。
     *
     * 为什么需要它？
     * - 在 HV 9.1 之后，部分内置校验器（例如 @Pattern 对应的 PatternValidator）实现了 HibernateConstraintValidator。
     * - 对于实现了 HibernateConstraintValidator 的校验器，HV 引擎初始化时会调用：
     *      HibernateConstraintValidator#initialize(ConstraintDescriptor, HibernateConstraintValidatorInitializationContext)
     *   而不是（或不仅仅是）ConstraintValidator#initialize(Annotation)。
     * - 你现在的 @Constraints 是“手动组合约束”：自己创建子 ConstraintValidator 然后直接调用 isValid()。
     *   这样会绕开 HV 引擎的初始化流程，导致校验器内部状态（比如编译好的正则 Pattern）未建立，从而触发 NPE。
     * - 因此你需要补齐 initialize(...) 的参数，其中第一个参数就是 ConstraintDescriptor。
     *
     * 为什么用 Proxy？
     * - ConstraintDescriptor 是一个接口，HV 的真实实现类（ConstraintDescriptorImpl）位于 internal 包，
     *   直接 new/强依赖它会让你在 HV 小版本升级时更容易炸。
     * - 但大多数内置校验器初始化时只用到 ConstraintDescriptor 的“少数方法”（annotation、attributes、message、groups 等）。
     * - 所以这里用 JDK Proxy 提供一个“最小可用”的实现即可，避免强绑 internal 实现类。
     *
     * 限制与注意事项：
     * - 这是“最小实现”，只覆盖常用的几类方法：getAnnotation、getAttributes、getMessageTemplate、getGroups、getPayload 等。
     * - 如果未来某个校验器初始化依赖更多信息（如 composing constraints、validator classes、clock provider 等），
     *   你可能需要扩展这里的实现。
     *
     * @author AI: ChatGPT
     * @since 1.0.0
     */
    private fun proxyConstraintDescriptor(annotation: Annotation): ConstraintDescriptor<*> {
        synchronized(constraintDescriptorCache) {
            constraintDescriptorCache[annotation]?.let { return it }
        }

        val attributeMethods = annotationAttributeMethodsCache.getOrPut(annotation.annotationClass.java) {
            annotation.annotationClass.java.declaredMethods
                .filter { it.parameterCount == 0 && it.name != "annotationType" }
        }
        val attributes: Map<String, Any?> = attributeMethods.associate { it.name to it.invoke(annotation) }
        val messageTemplate = (attributes["message"] ?: "").toString()
        val groups = ((attributes["groups"] as? Array<*>)?.filterIsInstance<Class<*>>()?.toSet())
            ?: emptySet<Class<*>>()
        val payload = ((attributes["payload"] as? Array<*>)?.filterIsInstance<Class<out Payload>>()?.toSet())
            ?: emptySet<Class<out Payload>>()

        val handler = InvocationHandler { _, method, _ ->
            when (method.name) {
                "getAnnotation" -> annotation
                "getAnnotationType" -> annotation.annotationClass.java
                "getAttributes" -> attributes
                "getMessageTemplate" -> messageTemplate
                "getGroups" -> groups
                "getPayload" -> payload

                "getComposingConstraints" -> emptyComposingConstraints
                "isReportAsSingleViolation" -> false
                "getConstraintValidatorClasses" -> emptyValidatorClasses // 一般 PatternValidator 初始化不靠这个
                "getValidationAppliesTo" -> attributes["validationAppliesTo"] // 可能为 null
                else -> defaultReturn(method.returnType)
            }
        }

        val proxy = Proxy.newProxyInstance(
            annotation.annotationClass.java.classLoader,
            arrayOf(ConstraintDescriptor::class.java),
            handler
        )
        if (proxy is ConstraintDescriptor<*>) {
            synchronized(constraintDescriptorCache) {
                val cached = constraintDescriptorCache[annotation]
                if (cached != null) {
                    return cached
                }
                constraintDescriptorCache[annotation] = proxy
                return proxy
            }
        }
        error("构造 ConstraintDescriptor 代理失败")
    }

    private fun defaultReturn(type: Class<*>): Any? = when {
        type == java.lang.Boolean.TYPE -> false
        type == Integer.TYPE -> 0
        type == java.lang.Long.TYPE -> 0L
        type == java.lang.Short.TYPE -> 0.toShort()
        type == java.lang.Byte.TYPE -> 0.toByte()
        type == Character.TYPE -> 0.toChar()
        type == java.lang.Float.TYPE -> 0f
        type == java.lang.Double.TYPE -> 0.0
        type.isAssignableFrom(Set::class.java) -> emptySet<Any>()
        type.isAssignableFrom(List::class.java) -> emptyList<Any>()
        else -> null
    }

}
