package io.kudos.base.bean.validation.kit

import io.kudos.base.bean.validation.support.ValidationContext
import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.hibernate.validator.HibernateValidator
import kotlin.reflect.KClass

/**
 * Bean validation utility.
 *
 * @author K
 * @since 1.0.0
 */
object ValidationKit {

    /**
     * Validate a bean object.
     *
     * @param T the bean type
     * @param bean the bean to validate
     * @param groups the array of group identifier classes; when non-empty, only constraints for the specified groups are validated
     * @param failFast whether to use fail-fast mode
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
        return try {
            validator.validate(bean, *classes)
        } finally {
            ValidationContext.clearBeans()
        }
    }

    /**
     * Validate a single property of a bean object.
     *
     * @param T the bean type
     * @param bean the bean to validate
     * @param property the property to validate
     * @param groups the array of group identifier classes; when non-empty, only constraints for the specified groups are validated
     * @param failFast whether to use fail-fast mode
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
        return try {
            validator.validateProperty(bean, property, *classes)
        } finally {
            ValidationContext.clearBeans()
        }
    }

    /**
     * Validate a single property of a bean class.
     *
     * @param T the bean type
     * @param beanClass the bean class to validate
     * @param property the property to validate
     * @param value the property value to validate
     * @param groups the array of group identifier classes; when non-empty, only constraints for the specified groups are validated
     * @param failFast whether to use fail-fast mode
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
        return try {
            getValidator(failFast).validateValue(beanClass.java, property, value, *classes)
        } finally {
            ValidationContext.clearBeans()
        }
    }

    /**
     * Get the validator.
     *
     * @param failFast whether to use fail-fast mode
     * @return the validator
     * @author K
     * @since 1.0.0
     */
    fun getValidator(failFast: Boolean = true): Validator {
        ValidationContext.setFailFast(failFast)

        if (ValidationContext.validator == null) {
            val validatorFactory = Validation.byProvider(HibernateValidator::class.java)
                .configure()
                .failFast(failFast)
                .buildValidatorFactory()
            ValidationContext.setFactory(validatorFactory)
            ValidationContext.validator = validatorFactory.validator
        }
        return requireNotNull(ValidationContext.validator) { "Validator has not been initialized" }
    }


}
