package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.Series
import io.kudos.base.bean.validation.kit.ValidationKit
import io.kudos.base.bean.validation.support.SeriesTypeEnum
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * Test cases for series validation.
 *
 * @author K
 * @since 1.0.0
 */
internal class SeriesTest {

    @Test
    fun validateIncDiff() {
        // Int, strictly increasing, all distinct -> pass
        assert(ValidationKit.validateValue(TestSeriesBean::class, "intIncDiff", arrayOf(1, 2, 5, 9, 11)).isEmpty())

        // Int, strictly increasing, all distinct -> fail (contains equal elements)
        assertFalse(ValidationKit.validateValue(TestSeriesBean::class, "intIncDiff", arrayOf(1, 2, 2, 9, 11)).isEmpty())

        // Int, strictly increasing, all distinct -> fail (contains a decrease)
        assertFalse(ValidationKit.validateValue(TestSeriesBean::class, "intIncDiff", arrayOf(1, 2, 1, 9, 11)).isEmpty())

        // Int, strictly increasing, all distinct, with step -> pass
        assert(ValidationKit.validateValue(TestSeriesBean::class, "intIncDiffStep", arrayOf(1, 2, 3, 4, 5)).isEmpty())

        // Int, strictly increasing, all distinct, with step -> fail (contains a decrease)
        assertFalse(
            ValidationKit.validateValue(TestSeriesBean::class, "intIncDiffStep", arrayOf(1, 2, 1, 4, 5)).isEmpty()
        )

        // Int, strictly increasing, all distinct, with step -> fail (violates the step)
        assertFalse(
            ValidationKit.validateValue(TestSeriesBean::class, "intIncDiffStep", arrayOf(1, 2, 3, 4, 6)).isEmpty()
        )


        // Float, strictly decreasing, all distinct -> pass
        assert(
            ValidationKit.validateValue(TestSeriesBean::class, "floatDescDiff", arrayOf(11F, 9F, 5F, 2F, 1F)).isEmpty()
        )

        // Float, strictly decreasing, all distinct -> fail (contains equal elements)
        assertFalse(
            ValidationKit.validateValue(TestSeriesBean::class, "floatDescDiff", arrayOf(11F, 9F, 2F, 2F, 1F)).isEmpty()
        )

        // Float, strictly decreasing, all distinct, with step -> pass
        assert(
            ValidationKit.validateValue(TestSeriesBean::class, "floatDescDiffStep", arrayOf(5F, 4F, 3F, 2F, 1F))
                .isEmpty()
        )

        // Float, strictly decreasing, all distinct, with step -> fail (violates the step)
        assertFalse(
            ValidationKit.validateValue(TestSeriesBean::class, "floatDescDiffStep", arrayOf(6F, 4F, 3F, 2F, 1F))
                .isEmpty()
        )


        // Long, increasing then decreasing, all distinct -> pass
        assert(
            ValidationKit.validateValue(
                TestSeriesBean::class, "longIncDiffDescDiff", arrayOf(1L, 2L, 5L, 9L, 11L, 10L, 7L)
            ).isEmpty()
        )

        // Long, increasing then decreasing, all distinct -> fail (contains equal elements)
        assertFalse(
            ValidationKit.validateValue(
                TestSeriesBean::class, "longIncDiffDescDiff", arrayOf(1L, 2L, 5L, 9L, 11L, 11L, 7L)
            ).isEmpty()
        )

        // Long, increasing then decreasing, all distinct -> fail (more than one inc-or-dec transition)
        assertFalse(
            ValidationKit.validateValue(
                TestSeriesBean::class, "longIncDiffDescDiff", arrayOf(1L, 2L, 10L, 9L, 11L, 10L, 7L)
            ).isEmpty()
        )

        // Long, increasing then decreasing, all distinct, with step -> pass
        assert(
            ValidationKit.validateValue(
                TestSeriesBean::class, "longIncDiffDescDiffStep", arrayOf(1L, 2L, 3L, 4L, 5L, 4L, 3L)
            ).isEmpty()
        )

        // Long, increasing then decreasing, all distinct, with step -> fail (more than one inc-or-dec transition)
        assertFalse(
            ValidationKit.validateValue(
                TestSeriesBean::class, "longIncDiffDescDiffStep", arrayOf(1L, 2L, 3L, 2L, 3L, 4L, 3L)
            ).isEmpty()
        )

        // Long, increasing then decreasing, all distinct, with step -> fail (violates the step)
        assertFalse(
            ValidationKit.validateValue(TestSeriesBean::class, "longIncDiffDescDiffStep", arrayOf(1L, 2L, 3L, 4L, 6L))
                .isEmpty()
        )


        // BigInteger, decreasing then increasing, all distinct -> pass
        assert(
            ValidationKit.validateValue(
                TestSeriesBean::class, "bigIntDescDiffIncDiff",
                arrayOf(BigInteger.valueOf(3), BigInteger.valueOf(2), BigInteger.valueOf(1), BigInteger.valueOf(5))
            ).isEmpty()
        )

        // BigInteger, decreasing then increasing, all distinct -> fail (contains equal elements)
        assertFalse(
            ValidationKit.validateValue(
                TestSeriesBean::class, "bigIntDescDiffIncDiff",
                arrayOf(BigInteger.valueOf(3), BigInteger.valueOf(2), BigInteger.valueOf(2), BigInteger.valueOf(5))
            ).isEmpty()
        )

        // BigInteger, decreasing then increasing, all distinct -> fail (more than one inc-or-dec transition)
        assertFalse(
            ValidationKit.validateValue(
                TestSeriesBean::class, "bigIntDescDiffIncDiff",
                arrayOf(BigInteger.valueOf(3), BigInteger.valueOf(2), BigInteger.valueOf(4), BigInteger.valueOf(3))
            ).isEmpty()
        )

        // BigInteger, decreasing then increasing, all distinct, with step -> pass
        assert(
            ValidationKit.validateValue(
                TestSeriesBean::class, "bigIntDescDiffIncDiffStep",
                arrayOf(BigInteger.valueOf(3), BigInteger.valueOf(2), BigInteger.valueOf(1), BigInteger.valueOf(2))
            ).isEmpty()
        )

        // BigInteger, decreasing then increasing, all distinct, with step -> fail (more than one inc-or-dec transition)
        assertFalse(
            ValidationKit.validateValue(
                TestSeriesBean::class, "bigIntDescDiffIncDiffStep",
                arrayOf(BigInteger.valueOf(3), BigInteger.valueOf(2), BigInteger.valueOf(3), BigInteger.valueOf(2))
            ).isEmpty()
        )

        // BigInteger, decreasing then increasing, all distinct, with step -> fail (violates the step)
        assertFalse(
            ValidationKit.validateValue(
                TestSeriesBean::class, "bigIntDescDiffIncDiffStep",
                arrayOf(BigInteger.valueOf(3), BigInteger.valueOf(1), BigInteger.valueOf(3), BigInteger.valueOf(5))
            ).isEmpty()
        )


        // BigDecimal, all distinct -> pass
        assert(
            ValidationKit.validateValue(
                TestSeriesBean::class, "bigDecimalDiff",
                arrayOf(BigDecimal(1), BigDecimal(5), BigDecimal(3), BigDecimal(7))
            ).isEmpty()
        )

        // BigDecimal, all distinct -> fail (contains equal elements)
        assertFalse(
            ValidationKit.validateValue(
                TestSeriesBean::class, "bigDecimalDiff",
                arrayOf(BigDecimal(1), BigDecimal(5), BigDecimal(5), BigDecimal(7))
            ).isEmpty()
        )

        // Int, all distinct, with step -> pass
        assert(
            ValidationKit.validateValue(
                TestSeriesBean::class, "bigDecimalDiffStep",
                arrayOf(BigDecimal(1), BigDecimal(2), BigDecimal(3), BigDecimal(4))
            ).isEmpty()
        )

        // Int, all distinct, with step -> fail (violates the step)
        assertFalse(
            ValidationKit.validateValue(
                TestSeriesBean::class, "bigDecimalDiffStep",
                arrayOf(BigDecimal(1), BigDecimal(2), BigDecimal(3), BigDecimal(5))
            ).isEmpty()
        )


        // Double, non-decreasing (equality allowed) -> pass
        assert(
            ValidationKit.validateValue(TestSeriesBean::class, "doubleIncEq", arrayOf(1.0, 2.0, 5.0, 9.0, 11.0))
                .isEmpty()
        )

        // Double, non-decreasing (equality allowed) -> pass
        assert(
            ValidationKit.validateValue(TestSeriesBean::class, "doubleIncEq", arrayOf(1.0, 2.0, 2.0, 9.0, 11.0))
                .isEmpty()
        )

        // Double, non-decreasing (equality allowed) -> fail (contains a decrease)
        assertFalse(
            ValidationKit.validateValue(TestSeriesBean::class, "doubleIncEq", arrayOf(1.0, 2.0, 1.0, 9.0, 11.0))
                .isEmpty()
        )

        // Double, non-decreasing (equality allowed), with step -> pass
        assert(
            ValidationKit.validateValue(TestSeriesBean::class, "doubleIncEqStep", arrayOf(1.0, 2.0, 3.0, 4.0, 5.0))
                .isEmpty()
        )

        // Double, non-decreasing (equality allowed), with step -> fail (contains a decrease)
        assertFalse(
            ValidationKit.validateValue(TestSeriesBean::class, "doubleIncEqStep", arrayOf(1.0, 2.0, 1.0, 3.0, 4.0, 5.0))
                .isEmpty()
        )

        // Double, non-decreasing (equality allowed), with step -> fail (violates the step)
        assertFalse(
            ValidationKit.validateValue(TestSeriesBean::class, "doubleIncEqStep", arrayOf(1.0, 2.0, 3.0, 4.0, 6.0))
                .isEmpty()
        )

        // String, non-increasing (equality allowed) -> pass
        assert(
            ValidationKit.validateValue(TestSeriesBean::class, "stringDescEq", arrayOf("11", "9", "5", "2", "1"))
                .isEmpty()
        )

        // String, non-increasing (equality allowed) -> pass
        assert(
            ValidationKit.validateValue(TestSeriesBean::class, "stringDescEq", arrayOf("11", "9", "2", "2", "1"))
                .isEmpty()
        )

        // String, non-increasing (equality allowed) -> fail (contains an increase)
        assertFalse(
            ValidationKit.validateValue(
                TestSeriesBean::class,
                "stringDescEq",
                arrayOf("11", "9", "5", "6", "1")
            ).isEmpty()
        )

        // String, non-increasing (equality allowed), with step -> pass
        assert(
            ValidationKit.validateValue(TestSeriesBean::class, "stringDescEqStep", arrayOf("5", "4", "3", "2", "1"))
                .isEmpty()
        )

        // String, non-increasing (equality allowed), with step -> pass
        assert(
            ValidationKit.validateValue(TestSeriesBean::class, "stringDescEqStep", arrayOf("5", "4", "3", "2", "2"))
                .isEmpty()
        )

        // String, non-increasing (equality allowed), with step -> fail (contains an increase)
        assertFalse(
            ValidationKit.validateValue(TestSeriesBean::class, "stringDescEqStep", arrayOf("5", "4", "3", "4", "1"))
                .isEmpty()
        )

        // String, non-increasing (equality allowed), with step -> fail (violates the step)
        assertFalse(
            ValidationKit.validateValue(TestSeriesBean::class, "stringDescEqStep", arrayOf("6", "4", "3", "2", "1"))
                .isEmpty()
        )


        // Byte, non-decreasing then non-increasing (equality allowed) -> pass
        assert(
            ValidationKit.validateValue(
                TestSeriesBean::class, "byteIncDiffDescDiff",
                arrayOf(1.toByte(), 2.toByte(), 5.toByte(), 9.toByte(), 11.toByte(), 10.toByte(), 7.toByte())
            ).isEmpty()
        )

        // Byte, non-decreasing then non-increasing (equality allowed) -> pass
        assert(
            ValidationKit.validateValue(
                TestSeriesBean::class, "byteIncDiffDescDiff",
                arrayOf(1.toByte(), 2.toByte(), 5.toByte(), 9.toByte(), 11.toByte(), 11.toByte(), 7.toByte())
            ).isEmpty()
        )

        // Byte, non-decreasing then non-increasing (equality allowed) -> fail (more than one inc-or-dec transition)
        assertFalse(
            ValidationKit.validateValue(
                TestSeriesBean::class, "byteIncDiffDescDiff",
                arrayOf(1.toByte(), 2.toByte(), 10.toByte(), 9.toByte(), 11.toByte(), 10.toByte(), 7.toByte())
            ).isEmpty()
        )

        // Byte, non-decreasing then non-increasing (equality allowed), with step -> pass
        assert(
            ValidationKit.validateValue(
                TestSeriesBean::class, "byteIncDiffDescDiffStep",
                arrayOf(1.toByte(), 2.toByte(), 3.toByte(), 4.toByte(), 5.toByte(), 4.toByte(), 3.toByte())
            ).isEmpty()
        )

        // Byte, non-decreasing then non-increasing (equality allowed), with step -> fail (more than one inc-or-dec transition)
        assertFalse(
            ValidationKit.validateValue(
                TestSeriesBean::class, "byteIncDiffDescDiffStep",
                arrayOf(1.toByte(), 2.toByte(), 3.toByte(), 2.toByte(), 3.toByte(), 4.toByte(), 3.toByte())
            ).isEmpty()
        )

        // Byte, non-decreasing then non-increasing (equality allowed), with step -> fail (violates the step)
        assertFalse(
            ValidationKit.validateValue(
                TestSeriesBean::class, "byteIncDiffDescDiffStep",
                arrayOf(1.toByte(), 2.toByte(), 3.toByte(), 4.toByte(), 6.toByte())
            )
                .isEmpty()
        )


        // Short, non-increasing then non-decreasing (equality allowed) -> pass
        assert(
            ValidationKit.validateValue(
                TestSeriesBean::class, "shortDescDiffIncDiff",
                arrayOf(3.toShort(), 2.toShort(), 1.toShort(), 5.toShort())
            ).isEmpty()
        )

        // Short, non-increasing then non-decreasing (equality allowed) -> pass
        assert(
            ValidationKit.validateValue(
                TestSeriesBean::class, "shortDescDiffIncDiff",
                arrayOf(3.toShort(), 2.toShort(), 2.toShort(), 5.toShort())
            ).isEmpty()
        )

        // Short, non-increasing then non-decreasing (equality allowed) -> fail (more than one inc-or-dec transition)
        assertFalse(
            ValidationKit.validateValue(
                TestSeriesBean::class, "shortDescDiffIncDiff",
                arrayOf(3.toShort(), 2.toShort(), 4.toShort(), 3.toShort())
            ).isEmpty()
        )

        // Short, non-increasing then non-decreasing (equality allowed), with step -> pass
        assert(
            ValidationKit.validateValue(
                TestSeriesBean::class, "shortDescDiffIncDiffStep",
                arrayOf(3.toShort(), 2.toShort(), 1.toShort(), 2.toShort())
            ).isEmpty()
        )

        // Short, non-increasing then non-decreasing (equality allowed), with step -> fail (more than one inc-or-dec transition)
        assertFalse(
            ValidationKit.validateValue(
                TestSeriesBean::class, "shortDescDiffIncDiffStep",
                arrayOf(3.toShort(), 2.toShort(), 3.toShort(), 2.toShort())
            ).isEmpty()
        )

        // Short, non-increasing then non-decreasing (equality allowed), with step -> fail (violates the step)
        assertFalse(
            ValidationKit.validateValue(
                TestSeriesBean::class, "shortDescDiffIncDiffStep",
                arrayOf(3.toShort(), 1.toShort(), 3.toShort(), 5.toShort())
            ).isEmpty()
        )


        // Int, all equal -> pass
        assert(ValidationKit.validateValue(TestSeriesBean::class, "intEq", arrayOf(1, 1, 1, 1)).isEmpty())

        // Int, all equal -> fail (contains an unequal element)
        assertFalse(ValidationKit.validateValue(TestSeriesBean::class, "intEq", arrayOf(1, 1, 2, 1)).isEmpty())

        // Int, all equal -> fail (series size does not match)
        assertFalse(ValidationKit.validateValue(TestSeriesBean::class, "intEq", arrayOf(1, 1, 1, 1, 1)).isEmpty())
    }

    @Test
    fun validateInvalidRuntimeInputShouldFailNotThrow() {
        assertFalse(ValidationKit.validateValue(TestSeriesBean::class, "intIncDiff", "not-a-list-or-array").isEmpty())

        assertFalse(
            ValidationKit.validateValue(
                TestSeriesBean::class,
                "stringDescEq",
                arrayOf<String?>("11", null, "1")
            ).isEmpty()
        )

        assertFalse(
            ValidationKit.validateValue(
                TestSeriesBean::class,
                "stringDescEq",
                arrayOf("11", "not-number", "1")
            ).isEmpty()
        )
    }

    internal data class TestSeriesBean(

        @get:Series(type = SeriesTypeEnum.INC_DIFF, message = "must be strictly increasing and all distinct")
        val intIncDiff: Array<Int>,

        @get:Series(type = SeriesTypeEnum.INC_DIFF, step = 1.0, message = "must be strictly increasing, all distinct, with step of 1")
        val intIncDiffStep: Array<Int>,

        @get:Series(type = SeriesTypeEnum.DESC_DIFF, message = "must be strictly decreasing and all distinct")
        val floatDescDiff: Array<Float>,

        @get:Series(type = SeriesTypeEnum.DESC_DIFF, step = 1.0, message = "must be strictly decreasing, all distinct, with step of 1")
        val floatDescDiffStep: Array<Float>,

        @get:Series(type = SeriesTypeEnum.INC_DIFF_DESC_DIFF, message = "must be increasing then decreasing and all distinct")
        val longIncDiffDescDiff: Array<Long>,

        @get:Series(type = SeriesTypeEnum.INC_DIFF_DESC_DIFF, step = 1.0, message = "must be increasing then decreasing, all distinct, with step of 1")
        val longIncDiffDescDiffStep: Array<Long>,

        @get:Series(type = SeriesTypeEnum.DESC_DIFF_INC_DIFF, message = "must be decreasing then increasing and all distinct")
        val bigIntDescDiffIncDiff: Array<BigInteger>,

        @get:Series(type = SeriesTypeEnum.DESC_DIFF_INC_DIFF, step = 1.0, message = "must be decreasing then increasing, all distinct, with step of 1")
        val bigIntDescDiffIncDiffStep: Array<BigInteger>,

        @get:Series(type = SeriesTypeEnum.DIFF, message = "must all be distinct")
        val bigDecimalDiff: Array<BigDecimal>,

        @get:Series(type = SeriesTypeEnum.DIFF, step = 1.0, message = "must all be distinct with step of 1")
        val bigDecimalDiffStep: Array<BigDecimal>,


        @get:Series(type = SeriesTypeEnum.INC_EQ, message = "must be non-decreasing (equality allowed)")
        val doubleIncEq: Array<Double>,

        @get:Series(type = SeriesTypeEnum.INC_EQ, step = 1.0, message = "must be non-decreasing (equality allowed); step of 1 when unequal")
        val doubleIncEqStep: Array<Double>,

        @get:Series(type = SeriesTypeEnum.DESC_EQ, message = "must be non-increasing (equality allowed)")
        val stringDescEq: Array<String>,

        @get:Series(type = SeriesTypeEnum.DESC_EQ, step = 1.0, message = "must be non-increasing (equality allowed); step of 1 when unequal")
        val stringDescEqStep: Array<String>,

        @get:Series(type = SeriesTypeEnum.INC_EQ_DESC_EQ, message = "must be non-decreasing then non-increasing (equality allowed)")
        val byteIncDiffDescDiff: Array<Byte>,

        @get:Series(type = SeriesTypeEnum.INC_EQ_DESC_EQ, step = 1.0, message = "must be non-decreasing then non-increasing (equality allowed); step of 1 when unequal")
        val byteIncDiffDescDiffStep: Array<Byte>,

        @get:Series(type = SeriesTypeEnum.DESC_EQ_INC_EQ, message = "must be non-increasing then non-decreasing (equality allowed)")
        val shortDescDiffIncDiff: Array<Short>,

        @get:Series(type = SeriesTypeEnum.DESC_EQ_INC_EQ, step = 1.0, message = "must be non-increasing then non-decreasing (equality allowed); step of 1 when unequal")
        val shortDescDiffIncDiffStep: Array<Short>,

        @get:Series(type = SeriesTypeEnum.EQ, size=4, message = "must all be equal and the series size must be 4")
        val intEq: List<Int>

    ) {
        override fun equals(other: Any?): Boolean = super.equals(other)
        override fun hashCode(): Int = super.hashCode()
    }

}
