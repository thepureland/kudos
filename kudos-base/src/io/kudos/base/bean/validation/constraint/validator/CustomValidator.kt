package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.Custom
import io.kudos.base.bean.validation.support.IBeanValidator
import io.kudos.base.bean.validation.support.ValidationContext
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import kotlin.reflect.KClass

/**
 * Validator for the Custom constraint.
 *
 * @author K
 * @since 1.0.0
 */
class CustomValidator : ConstraintValidator<Custom, Any?> {
    private lateinit var custom: Custom

    override fun initialize(custom: Custom) {
        this.custom = custom
    }

    override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
        return validate(custom.checkClass, value, context)
    }

    companion object {
        fun validate(
            checkClass: KClass<out IBeanValidator<*>>, value: Any?, context: ConstraintValidatorContext
        ): Boolean {
            if (value == null) {
                return true
            }

            val bean = requireNotNull(ValidationContext.get(context)) {
                "CustomValidator requires a bean to be present in ValidationContext"
            }
            val validator = checkClass.java.getDeclaredConstructor().newInstance()
            val validateMethod = validator.javaClass.methods.firstOrNull {
                it.name == "validate" && it.parameterCount == 1
            } ?: error("Validator [${checkClass.qualifiedName}] is missing a validate(bean) method")
            val result = validateMethod.invoke(validator, bean)
            return result as? Boolean
                ?: error("Validator [${checkClass.qualifiedName}] validate return value must be Boolean")
        }
    }

}
