package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.BeanKit
import io.kudos.base.bean.validation.constraint.annotations.Compare
import io.kudos.base.bean.validation.support.DependsValidator
import io.kudos.base.bean.validation.support.ValidationContext
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/**
 * Validator for the Compare constraint.
 *
 * @author K
 * @since 1.0.0
 */
class CompareValidator : ConstraintValidator<Compare, Any?> {
    private lateinit var compare: Compare

    override fun initialize(compare: Compare) {
        this.compare = compare
    }

    override fun isValid(value: Any?, constraintValidatorContext: ConstraintValidatorContext): Boolean {
        val bean = requireNotNull(ValidationContext.get(constraintValidatorContext)) {
            "CompareValidator requires a bean to be present in ValidationContext"
        }

        // When the dependency precondition is not met, the compare constraint is skipped and considered valid.
        val depends = compare.depends
        if (depends.properties.isNotEmpty()) {
            if (!DependsValidator.validate(depends, bean)) {
                return true
            }
        }

        // Comparison
        val anotherValue = BeanKit.getProperty(bean, compare.anotherProperty)
        if (value == null && anotherValue == null) {
            return true
        }
        if (value == null || anotherValue == null) {
            return false
        }
        if (value::class != anotherValue::class) {
            throw IllegalArgumentException(
                "The two properties validated by [Compare] must be of the same type! " +
                        "(${compare.anotherProperty}: ${anotherValue::class.qualifiedName}, " +
                        "current property: ${value::class.qualifiedName})"
            )
        }
        if (value is Array<*> && anotherValue is Array<*>) {
            // Array length mismatch is a runtime data issue (e.g. the user entered passwords of different lengths twice);
            // treat it as a validation failure so callers receive a normal ConstraintViolation instead of an exception.
            if (value.size != anotherValue.size) {
                return false
            }
            value.forEachIndexed { index, v ->
                if (v !is Comparable<*> || anotherValue[index] !is Comparable<*>) {
                    throw IllegalArgumentException(
                        "Every element in both arrays validated by [Compare] must implement the [Comparable] interface!"
                    )
                }
                val result = compare.logic.compare(v, anotherValue[index])
                if (!result) { // If any pair of elements fails the check, treat the entire validation as failed.
                    return false
                }
            }
            return true
        } else {
            // Handle the non-array case
            if (value !is Comparable<*>) {
                throw IllegalArgumentException(
                    "Both properties validated by [Compare] must implement the [Comparable] interface! " +
                            "(actual type: ${value::class.qualifiedName})"
                )
            }
            return compare.logic.compare(value, anotherValue)
        }
    }
}
