package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.Series
import io.kudos.base.bean.validation.kit.ValidationKit
import io.kudos.base.bean.validation.support.SeriesTypeEnum
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Regression tests for [SeriesValidator].
 *
 * Focus: BigDecimal precision/scale handling.
 * Historically the validator used the `BigDecimal(double)` constructor (which carries the binary
 * representation error of values such as 0.1) together with `BigDecimal.equals` (which also compares
 * scale, so "2" != "2.0"). Both caused valid series to be wrongly rejected. The validator now uses
 * `BigDecimal.valueOf(step)` and `compareTo(...) == 0`, so the cases below must all be valid.
 *
 * @author K
 * @since 1.0.0
 */
internal class SeriesValidatorTest {

    private fun isValid(property: String, value: Any?): Boolean =
        ValidationKit.validateValue(TestBean::class, property, value).isEmpty()

    @Test
    fun incDiffWithFractionalStepIsValid() {
        // [0, 0.1, 0.2, 0.3] with step 0.1 must be valid (binary error of 0.1 must not break it)
        assertTrue(isValid("incStep01", listOf("0", "0.1", "0.2", "0.3")))
        assertTrue(isValid("incStep01", arrayOf("0", "0.1", "0.2", "0.3")))
    }

    @Test
    fun incDiffWithMixedPrecisionIsValid() {
        // step result "1.0" vs element "1" differ only in scale -> must be treated as equal value
        assertTrue(isValid("incStep01", listOf("0.9", "1", "1.1")))
    }

    @Test
    fun diffWithFractionalStepIsValid() {
        // adjacent absolute differences all equal 0.1 despite binary error
        assertTrue(isValid("diffStep01", listOf("0", "0.1", "0.2", "0.3")))
    }

    @Test
    fun incEqWithFractionalStepAndScaleDiffersIsValid() {
        // equal-by-value (different scale) and step 0.1 increments must both pass
        assertTrue(isValid("incEqStep01", listOf("0.0", "0", "0.1", "0.2")))
    }

    @Test
    fun eqTreatsDifferentScaleAsEqualValue() {
        // "2" and "2.0" are equal in value; previously rejected because of scale-sensitive equals
        assertTrue(isValid("eq", listOf("2", "2.0", "2.00")))
    }

    internal data class TestBean(

        @get:Series(type = SeriesTypeEnum.INC_DIFF, step = 0.1)
        val incStep01: Any? = null,

        @get:Series(type = SeriesTypeEnum.DIFF, step = 0.1)
        val diffStep01: Any? = null,

        @get:Series(type = SeriesTypeEnum.INC_EQ, step = 0.1)
        val incEqStep01: Any? = null,

        @get:Series(type = SeriesTypeEnum.EQ)
        val eq: Any? = null

    )

}
