package io.kudos.base.bean.validation.kit

import io.kudos.base.bean.validation.support.ValidationContext
import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.hibernate.validator.HibernateValidator
import kotlin.reflect.KClass

/**
 * Bean验证工具类
 *
 * @author K
 * @since 1.0.0
 */
object ValidationKit {

    /**
     * 校验Bean对象
     *
     * @param T bean类型
     * @param bean 要校验的bean对象
     * @param groups 标识分组的Class数组，不为空将只校验指定分组的约束
     * @param failFast 是否为快速失败模式
     * @return Set(ConstraintViolation(Bean))
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> validateBean(
        bean: T,
        vararg groups: KClass<*> = arrayOf(),
        failFast: Boolean = true
    ): Set<ConstraintViolation<T>> {
        val classes = groups.map { it.java }.toTypedArray()
        val validator = getValidator(failFast)
        ValidationContext.set(validator, bean)
        return validator.validate(bean, *classes)
    }

    /**
     * 校验Bean对象的单个属性
     *
     * @param T bean类型
     * @param bean 要校验的bean对象
     * @param property 要校验的属性
     * @param groups 标识分组的Class数组，不为空将只校验指定分组的约束
     * @param failFast 是否为快速失败模式
     * @return Set(ConstraintViolation(Bean))
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> validateProperty(
        bean: T,
        property: String,
        vararg groups: KClass<*> = arrayOf(),
        failFast: Boolean = true
    ): Set<ConstraintViolation<T>> {
        val classes = groups.map { it.java }.toTypedArray()
        val validator = getValidator(failFast)
        ValidationContext.set(validator, bean)
        return validator.validateProperty(bean, property, *classes)
    }

    /**
     * 校验Bean类的单个属性
     *
     * @param T bean类型
     * @param beanClass 要校验的bean类
     * @param property 要校验的属性
     * @param value 要校验的属性值
     * @param groups 标识分组的Class数组，不为空将只校验指定分组的约束
     * @param failFast 是否为快速失败模式
     * @return Set(ConstraintViolation(Bean))
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> validateValue(
        beanClass: KClass<T>,
        property: String,
        value: Any?,
        vararg groups: KClass<*> = arrayOf(),
        failFast: Boolean = true
    ): Set<ConstraintViolation<T>> {
        val classes = groups.map { it.java }.toTypedArray()
        return getValidator(failFast).validateValue(beanClass.java, property, value, *classes)
    }

    /**
     * 得到验证器
     *
     * @param failFast 是否为快速失败模式
     * @return 验证器
     * @author K
     * @since 1.0.0
     */
    fun getValidator(failFast: Boolean = true): Validator {
        ValidationContext.setFailFast(failFast)
        if (ValidationContext.validator == null) {
            val validatorFactory = Validation.byProvider(HibernateValidator::class.java)
                .configure()
                .failFast(failFast)
//            .addProperty( "hibernate.validator.fail_fast", "true" )
                .buildValidatorFactory()
            ValidationContext.validator = validatorFactory.validator
        }
        return ValidationContext.validator!!
    }

}