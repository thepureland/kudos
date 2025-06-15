package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.NotNullOn
import io.kudos.base.bean.validation.support.DependsValidator
import io.kudos.base.bean.validation.support.ValidationContext
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/**
 * NotNullOn约束注解验证器
 *
 * @author K
 * @since 1.0.0
 */
class NotNullOnValidator : ConstraintValidator<NotNullOn, Any?> {
    private lateinit var notNullOn: NotNullOn

    override fun initialize(notNullOn: NotNullOn) {
        this.notNullOn = notNullOn
    }

    override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
        val bean = ValidationContext.get(context)
        val depends = notNullOn.depends
        return !DependsValidator.validate(depends, bean!!) || value != null
    }
}
