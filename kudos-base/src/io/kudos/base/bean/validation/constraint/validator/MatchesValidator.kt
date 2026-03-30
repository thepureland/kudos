package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.Matches
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.util.regex.Pattern

/**
 * [Matches] 校验器
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
class MatchesValidator : ConstraintValidator<Matches, CharSequence?> {

    private lateinit var pattern: Pattern
    private lateinit var messageTemplate: String

    override fun initialize(constraintAnnotation: Matches) {
        val kind = constraintAnnotation.value
        pattern = Pattern.compile(kind.regex)
        messageTemplate = constraintAnnotation.message.ifBlank { kind.defaultMessageKey }
    }

    override fun isValid(value: CharSequence?, context: ConstraintValidatorContext): Boolean {
        if (value == null) {
            return true
        }
        if (pattern.matcher(value).matches()) {
            return true
        }
        context.disableDefaultConstraintViolation()
        context.buildConstraintViolationWithTemplate(messageTemplate).addConstraintViolation()
        return false
    }
}
