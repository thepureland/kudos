package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.DateTime
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.text.ParseException
import java.text.SimpleDateFormat

/**
 * DateTime约束验证器
 *
 * @author K
 * @since 1.0.0
 */
class DateTimeValidator : ConstraintValidator<DateTime, CharSequence?> {
    private lateinit var dateTime: DateTime

    override fun initialize(dateTime: DateTime) {
        this.dateTime = dateTime
    }

    override fun isValid(value: CharSequence?, constraintValidatorContext: ConstraintValidatorContext): Boolean {
        if (value == null) {
            return true
        }
        val format = this.dateTime.format
        var valid = (format.length == value.length)
        if (valid) {
            try {
                SimpleDateFormat(format).parse(value.toString())
            } catch (_: ParseException) {
                valid = false
            }
        }
        return valid
    }
}
