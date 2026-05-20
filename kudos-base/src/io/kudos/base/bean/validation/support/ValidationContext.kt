package io.kudos.base.bean.validation.support

import io.kudos.base.bean.BeanKit
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Validator
import jakarta.validation.metadata.ConstraintDescriptor
import jakarta.validation.metadata.PropertyDescriptor
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorInitializationContext
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * Bean校验的上下文
 *
 * @author K
 * @since 1.0.0
 */
object ValidationContext {


    /** 缓存 HV 的 initializationContext */
    private var hvInitCtx: HibernateConstraintValidatorInitializationContext? = null
    /** Jakarta 标准约束注解的包名前缀，用于剔除非业务注解 */
    private const val jakartaAnnotationPrefix = "jakarta"
    /** Hibernate Validator 内置约束注解的包名前缀，用于剔除非业务注解 */
    private const val hibernateAnnotationPrefix = "org.hibernate"
    /** 反射查 `getValidatorFactoryScopedContext` 方法的缓存 */
    private val scopedContextMethodCache = ConcurrentHashMap<Class<*>, Method>()
    /** 反射查 `validatorFactoryScopedContext` 字段的缓存（方法路径失败时退回字段） */
    private val scopedContextFieldCache = ConcurrentHashMap<Class<*>, Field>()
    /** 反射查 `getConstraintValidatorInitializationContext` 方法的缓存 */
    private val initCtxMethodCache = ConcurrentHashMap<Class<*>, Method>()
    /** 注解类是否“业务自定义约束”的判定缓存：包名既不属 jakarta 也不属 org.hibernate */
    private val businessConstraintAnnotationCache = ConcurrentHashMap<Class<out Annotation>, Boolean>()

    /**
     * 把 ConstraintValidatorContext 的运行时类（HV 的 ConstraintValidatorContextImpl 等）映射到
     * 一个能从中提取 ConstraintDescriptor 的函数。通过反射的方式做到不硬绑 HV internal class
     * （之前直接 `as? ConstraintValidatorContextImpl`，HV 改名就编译失败）。
     */
    private val descriptorAccessorCache = ConcurrentHashMap<Class<*>, (Any) -> ConstraintDescriptor<*>?>()

    /**
     * 注入构建好的 [jakarta.validation.ValidatorFactory]，
     * 立即从中提取 Hibernate Validator 的初始化上下文并缓存。
     * 应用启动时调用一次。
     *
     * @param factory 已经构建好的 ValidatorFactory
     * @author K
     * @since 1.0.0
     */
    fun setFactory(factory: jakarta.validation.ValidatorFactory) {
        hvInitCtx = extractHvInitCtx(factory)
    }

    /**
     * 获取已缓存的 [HibernateConstraintValidatorInitializationContext]。
     *
     * @return HV 初始化上下文
     * @throws IllegalStateException 未先调用 [setFactory] 注入 ValidatorFactory 时
     * @author K
     * @since 1.0.0
     */
    fun getHvInitCtx(): HibernateConstraintValidatorInitializationContext =
        hvInitCtx ?: error("HibernateConstraintValidatorInitializationContext 尚未初始化：请确保先调用 ValidationKit.getValidator() 构建 ValidatorFactory")

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

    /**
     * 递归遍历 bean 的所有约束属性，把每个非 Jakarta/HV 约束的 [ConstraintDescriptor] hashcode
     * 与 bean 自身关联存入 [beanStore]，使自定义约束校验器后续能取回 bean。
     *
     * 嵌套 `@Valid` 属性也会递归处理；List 元素会按 `[i]` 拼接路径。
     *
     * @param validator 当前使用的 [Validator]
     * @param bean 待存入上下文的 bean
     * @param parentPath 父级路径（用于嵌套属性的展示，目前未对外暴露）
     * @param beanStore 线程局部存储
     * @author K
     * @since 1.0.0
     */
    private fun set(
        validator: Validator,
        bean: Any,
        parentPath: String?,
        beanStore: MutableMap<Int, Any>
    ) {
        // 获取 bean 的描述符
        val beanDescriptor = validator.getConstraintsForClass(bean.javaClass)

        // 遍历该 bean 中所有有约束的属性
        beanDescriptor.constrainedProperties.forEach { descriptor: PropertyDescriptor ->
            val propertyName = descriptor.propertyName
            // 拼接路径时确保 parentPath 为空时不会导致前面多一个 "."
            val fullPath =
                if (parentPath.isNullOrEmpty()) propertyName else "$parentPath.$propertyName"

            // 对每个属性的约束进行检查
            descriptor.constraintDescriptors.forEach { des: ConstraintDescriptor<*> ->
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
            }

            // 判断是否为嵌套对象，即该属性是否有其他受约束的属性（嵌套校验）
            if (descriptor.isCascaded) {
                when (val nestedBean = BeanKit.getProperty(bean, propertyName)) {
                    null -> {}
                    is MutableList<*> -> nestedBean.forEachIndexed { i, el ->
                        // 针对每个列表元素，递归校验并拼接索引到路径中
                        el?.let { set(validator, it, "$fullPath[$i]", beanStore) }
                    }
                    else -> set(validator, nestedBean, fullPath, beanStore)
                }
            }
        }
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
        val descriptor = extractConstraintDescriptor(constraintValidatorContext) ?: return null
        return beanMapThreadLocal.get().remove(descriptor.hashCode())
    }

    /**
     * 从任意 [ConstraintValidatorContext] 运行时实现里反射提取 [ConstraintDescriptor]，
     * 不依赖 HV 的具体实现类名（之前是 `as? ConstraintValidatorContextImpl`，HV 改包/改类名
     * 会编译失败）。优先找 `getConstraintDescriptor()` 方法，兜底找 `constraintDescriptor` 字段。
     *
     * 每个具体 ctx 类的反射 accessor 只构建一次，缓存在 [descriptorAccessorCache] 里。
     */
    private fun extractConstraintDescriptor(ctx: ConstraintValidatorContext): ConstraintDescriptor<*>? {
        val accessor = descriptorAccessorCache.getOrPut(ctx.javaClass) { buildDescriptorAccessor(ctx.javaClass) }
        return accessor(ctx)
    }

    /**
     * 为指定的 ConstraintValidatorContext 运行时类构造一个 descriptor 读取闭包。
     * 优先使用 `getConstraintDescriptor()` 方法，方法找不到时退回 `constraintDescriptor` 字段，
     * 兜底返回常量 null 闭包（适用 mock 或非 HV 实现）。
     *
     * @param clazz ConstraintValidatorContext 的具体运行时类
     * @return 把 ctx 实例映射到 [ConstraintDescriptor] 的闭包
     * @author K
     * @since 1.0.0
     */
    private fun buildDescriptorAccessor(clazz: Class<*>): (Any) -> ConstraintDescriptor<*>? {
        findInHierarchy(clazz) { it.getDeclaredMethod("getConstraintDescriptor") }?.let { method ->
            method.isAccessible = true
            return { runCatching { method.invoke(it) as? ConstraintDescriptor<*> }.getOrNull() }
        }
        findInHierarchy(clazz) { it.getDeclaredField("constraintDescriptor") }?.let { field ->
            field.isAccessible = true
            return { runCatching { field.get(it) as? ConstraintDescriptor<*> }.getOrNull() }
        }
        // 走到这里通常是 mock 或非 HV 实现——返回 null 让 get() 走兜底
        return { _ -> null }
    }

    /**
     * 沿继承链向上查找，把 [finder] 应用到每一层。
     * 任意一层返回非 null 即停止；finder 抛异常视为该层不命中。
     *
     * @param R 查找目标类型（[Method] 或 [Field]）
     * @param clazz 起始类
     * @param finder 在单个类上执行查找的闭包
     * @return 首个命中的结果；都查不到返回 null
     * @author K
     * @since 1.0.0
     */
    private inline fun <R : Any> findInHierarchy(clazz: Class<*>, finder: (Class<*>) -> R): R? {
        var current: Class<*>? = clazz
        while (current != null) {
            runCatching { return finder(current) }
            current = current.superclass
        }
        return null
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
