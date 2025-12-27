package io.kudos.base.bean.validation.support

import io.kudos.base.bean.BeanKit
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Validator
import jakarta.validation.metadata.ConstraintDescriptor
import jakarta.validation.metadata.PropertyDescriptor
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl
import org.hibernate.validator.internal.metadata.descriptor.ConstraintDescriptorImpl
import java.util.function.Consumer

/**
 * Bean校验的上下文
 *
 * @author K
 * @since 1.0.0
 */
object ValidationContext {

    /** 用于传递Bean给ConstraintValidator，因为hibernate validation的ConstraintValidatorContext取不到Bean */
    private val beanMap = mutableMapOf<Int, Any>() // Map<ConstraintDescriptor对象的hashcode，Bean对象>

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
        set(validator, bean, null)
    }

    private fun set(validator: Validator, bean: Any, parentPath: String?) {
        // 获取 bean 的描述符
        val beanDescriptor = validator.getConstraintsForClass(bean.javaClass)

        // 遍历该 bean 中所有有约束的属性
        beanDescriptor.constrainedProperties.forEach(Consumer { it: PropertyDescriptor? ->
            val propertyName = it!!.propertyName
            // 拼接路径时确保 parentPath 为空时不会导致前面多一个 "."
            val fullPath =
                if (parentPath.isNullOrEmpty()) propertyName else "$parentPath.$propertyName"

            // 对每个属性的约束进行检查
            it.constraintDescriptors.forEach(Consumer { des: ConstraintDescriptor<*>? ->
                val annoClassName = (des as ConstraintDescriptorImpl<*>).getAnnotationDescriptor().getType().getName()
                // 过滤掉 Jakarta 和 Hibernate 的注解
                if (!annoClassName.startsWith("jakarta") && !annoClassName.startsWith("org.hibernate")) {
                    beanMap[des.hashCode()] = bean
                }
            })

            // 判断是否为嵌套对象，即该属性是否有其他受约束的属性（嵌套校验）
            if (it.isCascaded) {
                // 通过 bean 描述符获取嵌套对象的类型
                val nestedBean = BeanKit.getProperty(bean, propertyName)
                // 处理列表对象的情况
                if (nestedBean is MutableList<*>) {
                    for (i in nestedBean.indices) {
                        val listElement: Any? = nestedBean[i]
                        if (listElement != null) {
                            // 针对每个列表元素，递归校验并拼接索引到路径中
                            set(validator, listElement, "$fullPath[$i]")
                        }
                    }
                } else {
                    // 处理普通嵌套对象
                    set(validator, nestedBean!!, fullPath)
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
        val descriptor = (constraintValidatorContext as ConstraintValidatorContextImpl).constraintDescriptor
        return beanMap.remove(descriptor.hashCode())
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
    fun isFailFast(): Boolean = failFastThreadLocal.get()

}
