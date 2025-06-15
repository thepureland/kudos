package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.Remote
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/**
 * Remote约束验证器
 *
 * @author K
 * @since 1.0.0
 */
class RemoteValidator : ConstraintValidator<Remote, Any?> {
    private lateinit var remote: Remote

    override fun initialize(remote: Remote) {
        this.remote = remote
    }

    override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
        return CustomValidator.validate(this.remote.checkClass, value, context)
    }

}
