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
            // Class.methods has no guaranteed ordering, and a generic IBeanValidator<T>
            // implementation exposes BOTH the real validate(T) and a synthetic bridge
            // validate(Object). Skip bridge/synthetic methods and deterministically pick
            // the single real overload whose parameter type accepts the actual bean.
            val candidates = validator.javaClass.methods.filter {
                it.name == "validate" && it.parameterCount == 1 && !it.isBridge && !it.isSynthetic
            }
            val validateMethod = candidates.firstOrNull {
                it.parameterTypes[0].isInstance(bean)
            } ?: candidates.singleOrNull()
                ?: error("Validator [${checkClass.qualifiedName}] is missing a validate(bean) method")
            val result = validateMethod.invoke(validator, bean)
            return result as? Boolean
                ?: error("Validator [${checkClass.qualifiedName}] validate return value must be Boolean")
        }
    }

}
