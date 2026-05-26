package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.Series
import io.kudos.base.bean.validation.support.SeriesTypeEnum
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.math.BigDecimal

/**
 * Series constraint validator.
 *
 * @author K
 * @since 1.0.0
 */
class SeriesValidator : ConstraintValidator<Series, Any?> {

    /** The [Series] annotation handled by the current instance, injected by [initialize] */
    private lateinit var series: Series

    override fun initialize(series: Series) {
        this.series = series
        if (series.step < 0.0) {
            error("The step of the @Series constraint annotation cannot be negative!")
        }
        if (series.size < 0) {
            error("The size of the @Series constraint annotation cannot be negative!")
        }
    }

    /**
     * Validate whether the value meets the series constraint.
     *
     * Validates whether elements in an array or list match the specified series rule (increasing, decreasing, arithmetic, etc.).
     *
     * Workflow:
     * 1. Null check: if the value is null, return true directly (null values are handled by annotations such as @NotNull)
     * 2. Type check: must be Array or List, otherwise an exception is thrown
     * 3. Length check: if the number of elements is <= 1, return true directly (a single element does not need a series check)
     * 4. Size check: if size is configured and the actual size does not match, return false
     * 5. Null element check: the array must not contain null elements; otherwise an exception is thrown
     * 6. Numeric conversion: convert all elements to BigDecimal for high-precision computation
     * 7. Series validation: call the validate method to verify the series rule
     *
     * Notes:
     * - Uses BigDecimal for high-precision computation to avoid floating-point precision issues
     * - Array elements are first converted to String and then to BigDecimal to ensure type compatibility
     * - Only Array and List types are supported; other types will throw an exception
     *
     * @param value the value to validate, must be Array or List
     * @param context the validation context
     * @return true if validation passes, false otherwise
     */
    override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
        if (value == null) {
            return true
        }
        if (value !is Array<*> && value !is List<*>) {
            return fail(context, "The @Series constraint annotation can only be placed on a getter that returns an Array or List!")
        }
        var values = when (value) {
            is Array<*> -> value.toList()
            is List<*> -> value
            else -> emptyList()
        }
        if (values.size <= 1) {
            return true
        }
        if (series.size != 0 && values.size != series.size) {
            return false
        }
        if (values.any { it == null }) {
            return fail(context, "The @Series constraint annotation requires that every element in the array is non-null! Array: $value")
        }

        return try {
            // Convert all array elements to String so they can be handled by BigDecimal for high-precision computation
            values = values.map { BigDecimal(it.toString()) }
            validate(series.type, series.step, *values.toTypedArray())
        } catch (_: RuntimeException) {
            fail(context, "The @Series constraint annotation only supports element types convertible to a number. Array: $value")
        }
    }

    /**
     * Replace the default violation with a custom message and return false.
     * Used on early-exit paths where the value is so invalid that validation cannot proceed (wrong type, contains null elements).
     *
     * @param context the Bean Validation context
     * @param message the custom error message template
     * @return always returns false
     * @author K
     * @since 1.0.0
     */
    private fun fail(context: ConstraintValidatorContext, message: String): Boolean {
        context.disableDefaultConstraintViolation()
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation()
        return false
    }

    /**
     * Validate series rules.
     *
     * Validates whether a numeric series matches the rule for the given series type; supports a variety of series patterns.
     *
     * Supported series types:
     * - INC_DIFF: strictly increasing; the difference between adjacent elements equals step (only requires increasing when step=0)
     * - DESC_DIFF: strictly decreasing; the difference between adjacent elements equals step (only requires decreasing when step=0)
     * - INC_DIFF_DESC_DIFF: first increasing then decreasing; find the maximum then validate the two halves
     * - DESC_DIFF_INC_DIFF: first decreasing then increasing; find the minimum then validate the two halves
     * - DIFF: all elements are distinct, and the difference between adjacent elements equals step (only requires distinctness when step=0)
     * - INC_EQ: non-strictly increasing; equal values allowed; the difference between adjacent elements equals step or 0 (only requires non-decreasing when step=0)
     * - DESC_EQ: non-strictly decreasing; equal values allowed; the difference between adjacent elements equals -step or 0 (only requires non-increasing when step=0)
     * - INC_EQ_DESC_EQ: first non-strictly increasing then non-strictly decreasing; locate the maximum interval and validate
     * - DESC_EQ_INC_EQ: first non-strictly decreasing then non-strictly increasing; locate the minimum interval and validate
     * - EQ: all elements are equal
     *
     * Computation notes:
     * - Uses BigDecimal for high-precision computation to avoid floating-point precision issues
     * - step=0.0 means no step is applied; only the increasing/decreasing relationship is validated
     * - For composite series (e.g., INC_DIFF_DESC_DIFF), the turning point is located first, then each half is validated
     *
     * Notes:
     * - The turning point of a composite series cannot be the first or last element; otherwise validation fails
     * - For series types allowing equality, consecutive equal values are handled
     * - All computations use BigDecimal to ensure precision
     *
     * @param type the series type enum
     * @param step the step value; 0.0 means no step is applied
     * @param values the numeric series to validate
     * @return true if the series matches the rule, false otherwise
     */
    private fun validate(type: SeriesTypeEnum, step: Double, vararg values: BigDecimal): Boolean {
        return when (type) {
            SeriesTypeEnum.INC_DIFF -> values.toList().zipWithNext().all { (prev, curr) ->
                if (step == 0.0) prev < curr else prev + BigDecimal(step) == curr
            }
            SeriesTypeEnum.DESC_DIFF -> {
                validate(SeriesTypeEnum.INC_DIFF, step, *values.reversed().toTypedArray())
            }
            SeriesTypeEnum.INC_DIFF_DESC_DIFF -> {
                val maxValueIndex = values.indexOf(values.maxOrNull())
                if (maxValueIndex == values.lastIndex) {
                    return false
                }
                val incDiffValues = values.copyOfRange(0, maxValueIndex + 1)
                val incDiffPass = validate(SeriesTypeEnum.INC_DIFF, step, *incDiffValues)
                if (incDiffPass) {
                    val descDiffValues = values.copyOfRange(maxValueIndex, values.size)
                    validate(SeriesTypeEnum.DESC_DIFF, step, *descDiffValues)
                } else {
                    false
                }
            }
            SeriesTypeEnum.DESC_DIFF_INC_DIFF -> {
                val minValueIndex = values.indexOf(values.minOrNull())
                if (minValueIndex == values.lastIndex) {
                    return false
                }
                val descDiffValues = values.copyOfRange(0, minValueIndex + 1)
                val descDiffPass = validate(SeriesTypeEnum.DESC_DIFF, step, *descDiffValues)
                if (descDiffPass) {
                    val incDiffValues = values.copyOfRange(minValueIndex, values.size)
                    validate(SeriesTypeEnum.INC_DIFF, step, *incDiffValues)
                } else {
                    false
                }
            }
            SeriesTypeEnum.DIFF -> when {
                values.toSet().size != values.size -> false
                step == 0.0 -> true
                else -> values.toList().zipWithNext().all { (prev, curr) ->
                    (prev - curr).abs() == BigDecimal(step)
                }
            }
            SeriesTypeEnum.INC_EQ -> values.toList().zipWithNext().all { (prev, curr) ->
                if (step == 0.0) prev <= curr
                else prev == curr || prev + BigDecimal(step) == curr
            }
            SeriesTypeEnum.DESC_EQ -> {
                validate(SeriesTypeEnum.INC_EQ, step, *values.reversed().toTypedArray())
            }
            SeriesTypeEnum.INC_EQ_DESC_EQ -> {
                val maxValue = values.maxOrNull()
                val maxValueStartIndex = values.indexOf(maxValue)
                if (maxValueStartIndex == 0 || maxValueStartIndex == values.lastIndex) {
                    return false
                }
                var maxValueEndIndex = maxValueStartIndex
                for (index in maxValueStartIndex until values.lastIndex) {
                    if (values[index] == maxValue) {
                        maxValueEndIndex = index
                    } else {
                        break
                    }
                }
                val incEqValues = values.copyOfRange(0, maxValueStartIndex + 1)
                val incEqPass = validate(SeriesTypeEnum.INC_EQ, step, *incEqValues)
                if (incEqPass) {
                    val descEqValues = values.copyOfRange(maxValueEndIndex, values.size)
                    validate(SeriesTypeEnum.DESC_EQ, step, *descEqValues)
                } else {
                    false
                }
            }
            SeriesTypeEnum.DESC_EQ_INC_EQ -> {
                val minValue = values.minOrNull()
                val minValueStartIndex = values.indexOf(minValue)
                if (minValueStartIndex == 0 || minValueStartIndex == values.lastIndex) {
                    return false
                }
                var minValueEndIndex = minValueStartIndex
                for (index in minValueStartIndex until values.lastIndex) {
                    if (values[index] == minValue) {
                        minValueEndIndex = index
                    } else {
                        break
                    }
                }
                val descEqValues = values.copyOfRange(0, minValueStartIndex + 1)
                val descEqPass = validate(SeriesTypeEnum.DESC_EQ, step, *descEqValues)
                if (descEqPass) {
                    val incEqValues = values.copyOfRange(minValueEndIndex, values.size)
                    validate(SeriesTypeEnum.INC_EQ, step, *incEqValues)
                } else {
                    false
                }
            }
            SeriesTypeEnum.EQ -> {
                values.toSet().size == 1
            }
        }
    }

}
