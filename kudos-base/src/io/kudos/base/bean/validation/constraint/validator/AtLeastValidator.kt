package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.BeanKit.getProperty
import io.kudos.base.bean.validation.constraint.annotations.AtLeast
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/**
 * Validator for the AtLeast constraint.
 *
 * @author K
 * @since 1.0.0
 */
class AtLeastValidator : ConstraintValidator<AtLeast, Any> {
    private lateinit var atLeast: AtLeast

    override fun initialize(atLeast: AtLeast) {
        this.atLeast = atLeast
        check(atLeast.count >= 0) { "The count specified by @AtLeast must not be negative!" }
        check(atLeast.count <= atLeast.properties.size) {
            "The count [${atLeast.count}] specified by @AtLeast must not exceed the number of properties [${atLeast.properties.size}]!"
        }
    }

    override fun isValid(bean: Any, constraintValidatorContext: ConstraintValidatorContext?): Boolean {
        var count = 0
        for (prop in atLeast.properties) {
            val value = getProperty(bean, prop)
            if (atLeast.logic.compare(value)) {
                count++
            }
        }
        return count >= atLeast.count
    }
}
