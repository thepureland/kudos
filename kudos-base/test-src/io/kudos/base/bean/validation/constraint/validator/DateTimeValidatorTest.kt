package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.DateTime
import io.kudos.base.bean.validation.kit.ValidationKit
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [DateTimeValidator].
 *
 * Coverage:
 * - null value short-circuits to valid.
 * - a well-formed, real date passes.
 * - a value whose length differs from the format is rejected.
 * - illegal dates that have the right length but invalid month/day components are rejected
 *   (regression for the lenient-parsing bug where SimpleDateFormat rolled them over).
 *
 * @author K
 * @since 1.0.0
 */
internal class DateTimeValidatorTest {

    @Test
    fun nullValueIsValid() {
        assertTrue(ValidationKit.validateValue(TestDateBean::class, "date", null).isEmpty())
    }

    @Test
    fun validDatePasses() {
        assertTrue(ValidationKit.validateValue(TestDateBean::class, "date", "2020-02-29").isEmpty())
        assertTrue(ValidationKit.validateValue(TestDateBean::class, "date", "2021-12-31").isEmpty())
    }

    @Test
    fun wrongLengthIsRejected() {
        assertFalse(ValidationKit.validateValue(TestDateBean::class, "date", "2020-1-1").isEmpty())
    }

    @Test
    fun illegalDatesAreRejected() {
        // Right length (10 chars) but illegal components. Before the lenient-parsing fix,
        // SimpleDateFormat would roll these over and accept them.
        assertFalse(ValidationKit.validateValue(TestDateBean::class, "date", "2020-13-01").isEmpty())
        assertFalse(ValidationKit.validateValue(TestDateBean::class, "date", "2020-02-30").isEmpty())
        assertFalse(ValidationKit.validateValue(TestDateBean::class, "date", "2020-99-99").isEmpty())
    }

    // ---------------------------------------------------------------------
    // fixtures
    // ---------------------------------------------------------------------

    internal data class TestDateBean(

        @get:DateTime(format = "yyyy-MM-dd")
        val date: String? = null

    )

}
