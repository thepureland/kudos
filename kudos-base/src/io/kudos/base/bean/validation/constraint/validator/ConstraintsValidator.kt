package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.AtLeast
import io.kudos.base.bean.validation.constraint.annotations.Constraints
import io.kudos.base.bean.validation.support.ValidationContext
import io.kudos.base.bean.validation.support.ValidatorFactory
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.declaredMemberProperties

/**
 * Constraints约束验证器
 *
 * @author K
 * @since 1.0.0
 */
class ConstraintsValidator : ConstraintValidator<Constraints, Any?> {

    /** 当前实例处理的 [Constraints] 组合约束注解，由 [initialize] 注入 */
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
        /** Hibernate Validator 初始化方法（双参签名）的反射缓存 */
        private val hvInitMethodCache = ConcurrentHashMap<Class<*>, Method>()
        /** 标准 [ConstraintValidator.initialize] 方法的反射缓存 */
        private val initMethodCache = ConcurrentHashMap<Class<*>, Method>()
        /** [ConstraintValidator.isValid] 方法的反射缓存 */
        private val isValidMethodCache = ConcurrentHashMap<Class<*>, Method>()
        /** 注解属性方法列表缓存：避免每次都遍历 declaredMethods */
        private val annotationAttributeMethodsCache = ConcurrentHashMap<Class<out Annotation>, List<Method>>()
        /** 注解 `message` 属性方法的反射缓存 */
        private val annotationMessageMethodCache = ConcurrentHashMap<Class<out Annotation>, Method>()
        // 用 ConcurrentHashMap 取代了原先 `Collections.synchronizedMap(WeakHashMap)`：
        // - 原实现下所有 get/put 都串行在同一把锁上，是校验热路径上的真实争用点
        // - 弱引用在典型 Spring 应用里没有实际收益（注解被宿主类元数据强引用，生命周期 = JVM）
        // - 配合下面 proxyConstraintDescriptor 用 computeIfAbsent，构造 + 写入是原子的
        private val constraintDescriptorCache = ConcurrentHashMap<Annotation, ConstraintDescriptor<*>>()

        /**
         * 记录哪些 validator 实例已完成 initialize，避免每次 isValid 都重做。
         * 配合 ValidatorFactory 的实例缓存使用：同一个 (annotation, valueClass)
         * 命中的 validator 是同一实例，第二次起跳过 init。
         *
         * 用 `computeIfAbsent` 保证"init 与对其它线程可见"的原子化——并发场景下
         * 同一 key 上只有一个线程跑 init，其它线程等待结果。
         */
        private val initializedValidators: MutableMap<ConstraintValidator<*, *>, Boolean> =
            ConcurrentHashMap()
        /** [Constraints] 自身的元属性名，遍历子约束时需要排除 */
        private val excludedConstraintPropertyNames = setOf("order", "andOr", "message", "groups", "payload")
        /** 复用的“空 composing constraints”集合，避免每次构造代理都新建 */
        private val emptyComposingConstraints: Set<ConstraintDescriptor<*>> = emptySet()
        /** 复用的“空 validator classes”列表，避免每次构造代理都新建 */
        private val emptyValidatorClasses: List<Class<*>> = emptyList()
        /** 强制优先校验的约束类型集合：null/blank/empty 一类必须先短路，提升错误信息可读性 */
        private val priorityConstraintTypes = setOf(
            NotBlank::class,
            NotEmpty::class,
            NotNull::class,
            Null::class
        )

        /**
         * 提取 [Constraints] 中所有实际启用的子约束，并按以下规则排序：
         * 1) 显式 [Constraints.order] 中列出的约束优先按用户指定顺序；
         * 2) 其余约束按反射遍历顺序追加；
         * 3) 如果其中存在 NotBlank/NotEmpty/NotNull/Null，强制把它提到最前。
         *
         * 通过 `message != [Constraints.MESSAGE]` 判断用户是否真的启用了该子约束。
         *
         * @param constraints 组合约束注解
         * @return 排好序的子约束列表
         * @throws IllegalStateException 当 order 中引用了未启用的约束类，或反射出非注解属性时
         * @author K
         * @since 1.0.0
         */
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
                // 有指定顺序的子约束
                val sequenceAnnotations = linkedSetOf<Annotation>()
                constraints.order.forEach { clazz ->
                    val annotation = annotations.firstOrNull { it.annotationClass == clazz }
                        ?: error("Constraints约束sequence中指定的【$clazz】子约束未定义规则！")
                    sequenceAnnotations.add(annotation)
                }
                sequenceAnnotations.addAll(annotations) // 合并未指定顺序的子约束
                sequenceAnnotations.toList()
            } else {
                annotations
            }

            // 强制优先NotBlank、NotEmpty、NotNull、Null
            return priorityAnnotation?.let { listOf(it) + result.filter { a -> a != it } } ?: result
        }

        /**
         * 通过反射读取注解的 `message` 属性值。
         * 方法本身按注解类缓存，第二次读取同一类注解时不再走 [Class.getDeclaredMethod]。
         *
         * @param annotation 任意约束注解
         * @return 注解的 message 值
         * @throws IllegalStateException 当注解的 `message` 返回值不是 String 时
         * @author K
         * @since 1.0.0
         */
        private fun getAnnotationMessage(annotation: Annotation): String {
            val annotationClass = annotation.annotationClass.java
            val method = annotationMessageMethodCache.getOrPut(annotationClass) {
                annotationClass.getDeclaredMethod("message").apply { isAccessible = true }
            }
            return method.invoke(annotation) as? String
                ?: error("注解【${annotation.annotationClass}】的 message 返回值不是 String")
        }
    }

    /**
     * 单个子约束的校验入口：
     * - null 值除 NotNull/NotEmpty/NotBlank 外一律视为通过；
     * - [AtLeast] 子约束语义上作用在 bean 本身而非属性值，从 [ValidationContext] 取出 bean；
     * - 通过 [ValidatorFactory] 拿到该注解对应的所有 [ConstraintValidator] 并依次执行，全通过才算通过。
     *
     * @param annotation 待执行的子约束
     * @param value 被校验的属性值
     * @param bean 当前正在校验的目标对象（[AtLeast] 等需要）
     * @param context Bean Validation 上下文
     * @return 该子约束是否通过
     * @throws IllegalStateException 当找不到对应的 ConstraintValidator 时
     * @author K
     * @since 1.0.0
     */
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


    /**
     * 反射调用单个 [ConstraintValidator] 的 initialize + isValid。
     *
     * 对实现了 [HibernateConstraintValidator] 的校验器走双参 initialize（descriptor + initCtx），
     * 否则走标准 initialize(annotation)。同一 validator 实例只初始化一次，由 [initializedValidators] 兜底。
     *
     * @param constraintValidator 已实例化的校验器
     * @param annotation 当前子约束
     * @param value 被校验的值
     * @param context Bean Validation 上下文
     * @return [ConstraintValidator.isValid] 返回值
     * @throws IllegalStateException 反射查不到 initialize/isValid，或 isValid 返回值不是 Boolean 时
     * @author K
     * @since 1.0.0
     */
    private fun doValidate(
        constraintValidator: ConstraintValidator<*, *>,
        annotation: Annotation,
        value: Any?,
        context: ConstraintValidatorContext
    ): Boolean {
        val validatorClass = constraintValidator.javaClass

        // 同一 validator 实例只 initialize 一次。后续命中直接跳过。
        // ValidatorFactory 的实例缓存保证了：相同 (annotation, valueClass) 拿到的是同一实例。
        initializedValidators.computeIfAbsent(constraintValidator) {
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
            true
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

    /**
     * 用子约束自身的 message 追加一条 violation，并禁用默认 violation。
     * 关闭默认 violation 是为了避免组合约束之外又额外冒出 Constraints 自己的默认错误信息。
     *
     * @param context Bean Validation 上下文
     * @param annotation 失败的子约束
     * @author K
     * @since 1.0.0
     */
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
    private fun proxyConstraintDescriptor(annotation: Annotation): ConstraintDescriptor<*> =
        constraintDescriptorCache.computeIfAbsent(annotation) { buildConstraintDescriptorProxy(it) }

    /**
     * 实际构造 [ConstraintDescriptor] 代理对象的实现。
     * 反射读取注解的所有属性方法（按注解类缓存），用 JDK [Proxy] 暴露最小集合的接口方法：
     * getAnnotation / getAttributes / getMessageTemplate / getGroups / getPayload 等。
     *
     * @param annotation 当前子约束
     * @return ConstraintDescriptor 代理实例
     * @throws IllegalStateException Proxy 实例化失败时
     * @author K
     * @since 1.0.0
     */
    private fun buildConstraintDescriptorProxy(annotation: Annotation): ConstraintDescriptor<*> {
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
        return proxy as? ConstraintDescriptor<*>
            ?: error("构造 ConstraintDescriptor 代理失败")
    }

    /**
     * 给 ConstraintDescriptor 代理上未列出的方法返回类型安全的零值。
     * 基本类型按规范返回 0/false/' '；Set / List 返回空集合；其它对象返回 null。
     *
     * @param type 方法返回类型
     * @return 类型安全的“默认值”
     * @author K
     * @since 1.0.0
     */
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
