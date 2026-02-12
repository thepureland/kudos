package io.kudos.base.bean.validation.support

import io.kudos.base.bean.BeanKit
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Validator
import jakarta.validation.metadata.ConstraintDescriptor
import jakarta.validation.metadata.PropertyDescriptor
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorInitializationContext
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

/**
 * Bean校验的上下文
 *
 * @author K
 * @since 1.0.0
 */
object ValidationContext {


    /** 缓存 HV 的 initializationContext */
    private var hvInitCtx: HibernateConstraintValidatorInitializationContext? = null
    private const val jakartaAnnotationPrefix = "jakarta"
    private const val hibernateAnnotationPrefix = "org.hibernate"
    private val scopedContextMethodCache = ConcurrentHashMap<Class<*>, Method>()
    private val scopedContextFieldCache = ConcurrentHashMap<Class<*>, Field>()
    private val initCtxMethodCache = ConcurrentHashMap<Class<*>, Method>()
    private val businessConstraintAnnotationCache = ConcurrentHashMap<Class<out Annotation>, Boolean>()

    fun setFactory(factory: jakarta.validation.ValidatorFactory) {
        hvInitCtx = extractHvInitCtx(factory)
    }

    fun getHvInitCtx(): HibernateConstraintValidatorInitializationContext {
        return hvInitCtx ?: error("HibernateConstraintValidatorInitializationContext 尚未初始化：请确保先调用 ValidationKit.getValidator() 构建 ValidatorFactory")
    }

    /**
     * 从 Hibernate Validator 的 ValidatorFactory 中提取 HibernateConstraintValidatorInitializationContext。
     *
     * 为什么要做这件事？
     * - 你在自定义组合约束（@Constraints）里会“手动”创建并调用内置 ConstraintValidator（如 @Pattern 对应的 PatternValidator）。
     * - HV 9.1 起，部分内置校验器实现了 HibernateConstraintValidator，
     *   它们期望通过 HibernateConstraintValidator#initialize(ConstraintDescriptor, HibernateConstraintValidatorInitializationContext)
     *   进行初始化（例如编译正则 Pattern、构建内部状态等）。
     * - 但你手动调用时绕开了 Hibernate Validator 引擎的初始化流程，所以必须自己拿到该 initCtx 再初始化一次，
     *   否则可能出现内部字段未初始化导致 NPE（你遇到的就是 PatternValidator.pattern 为 null）。
     *
     * ⚠️ 注意：
     * - 这段实现依赖 HV internal API（org.hibernate.validator.internal.*），版本升级可能改方法名/字段名。
     * - 因此这里写成“多策略尝试 + 兜底”，尽可能在小版本变动下仍可工作。
     *
     * @author AI: ChatGPT
     * @since 1.0.0
     */
    private fun extractHvInitCtx(factory: jakarta.validation.ValidatorFactory): HibernateConstraintValidatorInitializationContext {
        // 1) 先走方法：factory.getValidatorFactoryScopedContext()
        val scopedContext = runCatching {
            val method = scopedContextMethodCache.getOrPut(factory.javaClass) {
                factory.javaClass.getDeclaredMethod("getValidatorFactoryScopedContext").apply { isAccessible = true }
            }
            method.invoke(factory)
        }.getOrNull()
            ?: runCatching {
                // 2) 再走字段：factory.validatorFactoryScopedContext
                val field = scopedContextFieldCache.getOrPut(factory.javaClass) {
                    factory.javaClass.getDeclaredField("validatorFactoryScopedContext").apply { isAccessible = true }
                }
                field.get(factory)
            }.getOrNull()
            ?: error("无法从 ${factory.javaClass.name} 获取 ValidatorFactoryScopedContext（HV 版本/实现可能变化）")

        // 3) scopedContext.getConstraintValidatorInitializationContext()
        val initCtx = runCatching {
            val method = initCtxMethodCache.getOrPut(scopedContext.javaClass) {
                scopedContext.javaClass.getDeclaredMethod("getConstraintValidatorInitializationContext")
                    .apply { isAccessible = true }
            }
            method.invoke(scopedContext)
        }.getOrNull()
            ?: error("无法从 ${scopedContext.javaClass.name} 获取 ConstraintValidatorInitializationContext（HV 版本/实现可能变化）")

        return initCtx as? HibernateConstraintValidatorInitializationContext
            ?: error("ConstraintValidatorInitializationContext 类型不匹配: ${initCtx.javaClass.name}")
    }

    /** 用于传递Bean给ConstraintValidator，因为hibernate validation的ConstraintValidatorContext取不到Bean */
    private val beanMapThreadLocal = ThreadLocal.withInitial { mutableMapOf<Int, Any>() } // Map<ConstraintDescriptor对象的hashcode，Bean对象>

    /** 是否快速失败模式 */
    private val failFastThreadLocal = InheritableThreadLocal<Boolean>()

    /** 验证器 */
    var validator: Validator? = null

    /**
     * 存放ConstraintDescriptor对象hashcode关联的Bean
     *
     * @param validator 验证器
     * @param bean 待校验的Bean
     * @author K
     * @since 1.0.0
     */
    fun set(validator: Validator, bean: Any) {
        set(validator, bean, null, beanMapThreadLocal.get())
    }

    private fun set(
        validator: Validator,
        bean: Any,
        parentPath: String?,
        beanStore: MutableMap<Int, Any>
    ) {
        // 获取 bean 的描述符
        val beanDescriptor = validator.getConstraintsForClass(bean.javaClass)

        // 遍历该 bean 中所有有约束的属性
        beanDescriptor.constrainedProperties.forEach(Consumer { descriptor: PropertyDescriptor ->
            val propertyName = descriptor.propertyName
            // 拼接路径时确保 parentPath 为空时不会导致前面多一个 "."
            val fullPath =
                if (parentPath.isNullOrEmpty()) propertyName else "$parentPath.$propertyName"

            // 对每个属性的约束进行检查
            descriptor.constraintDescriptors.forEach(Consumer { des: ConstraintDescriptor<*> ->
                val annotationClass = des.annotation.annotationClass.java
                val isBusinessConstraint = businessConstraintAnnotationCache.getOrPut(annotationClass) {
                    val annoClassName = annotationClass.name
                    !annoClassName.startsWith(jakartaAnnotationPrefix) &&
                        !annoClassName.startsWith(hibernateAnnotationPrefix)
                }
                // 过滤掉 Jakarta 和 Hibernate 的注解
                if (isBusinessConstraint) {
                    beanStore[des.hashCode()] = bean
                }
            })

            // 判断是否为嵌套对象，即该属性是否有其他受约束的属性（嵌套校验）
            if (descriptor.isCascaded) {
                // 通过 bean 描述符获取嵌套对象的类型
                val nestedBean = BeanKit.getProperty(bean, propertyName)
                // 处理列表对象的情况
                if (nestedBean is MutableList<*>) {
                    for (i in nestedBean.indices) {
                        val listElement: Any? = nestedBean[i]
                        if (listElement != null) {
                            // 针对每个列表元素，递归校验并拼接索引到路径中
                            set(validator, listElement, "$fullPath[$i]", beanStore)
                        }
                    }
                } else {
                    // 处理普通嵌套对象
                    if (nestedBean != null) {
                        set(validator, nestedBean, fullPath, beanStore)
                    }
                }
            }
        })
    }

    /**
     * 获取ConstraintDescriptor对象hashcode关联的Bean，并从上下文中移除
     *
     * @param constraintValidatorContext 约束验证器上下文
     * @return 待校验的Bean
     * @author K
     * @since 1.0.0
     */
    fun get(constraintValidatorContext: ConstraintValidatorContext): Any? {
        val descriptor = (constraintValidatorContext as? ConstraintValidatorContextImpl)?.constraintDescriptor
            ?: return null
        return beanMapThreadLocal.get().remove(descriptor.hashCode())
    }

    /**
     * 清理当前缓存的Bean映射，避免跨次校验残留。
     */
    fun clearBeans() {
        beanMapThreadLocal.remove()
    }

    /**
     * 设置快速失败模式
     *
     * @param failFast true：快速失败模式, false: 非快速失败模式
     * @author K
     * @since 1.0.0
     */
    fun setFailFast(failFast: Boolean) = failFastThreadLocal.set(failFast)

    /**
     * 返回快速失败模式
     *
     * @return true：快速失败模式, false: 非快速失败模式
     * @author K
     * @since 1.0.0
     */
    fun isFailFast(): Boolean = failFastThreadLocal.get() ?: true

}
