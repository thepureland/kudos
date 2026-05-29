package io.kudos.ability.data.docdb.mongo.convert

import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

/**
 * Unit tests for [BigIntegerConverters].
 *
 * Locks in the two invariants the Mongo integration relies on:
 *  - Writing converter produces a String that is reversible via Reading converter.
 *  - Arbitrary-precision values (well past Long.MAX_VALUE) survive the round trip — that is the
 *    entire point of storing as String over BSON's `Long` / `Decimal128`.
 *
 * Plus one negative case to lock in the intentional behaviour drift from soul: a malformed
 * stored String must throw [NumberFormatException] rather than silently turning into null.
 *
 * @author AI: Claude
 * @since 1.0.0
 */
internal class BigIntegerConvertersTest {

    @Test
    fun writeRead_roundTripsPositive() {
        val original = BigInteger("12345678901234567890")
        val written = BigIntegerConverters.BigIntegerToString.convert(original)
        val read = BigIntegerConverters.StringToBigInteger.convert(written)
        assertEquals(original, read)
    }

    @Test
    fun writeRead_roundTripsNegative() {
        val original = BigInteger("-99999999999999999999")
        val written = BigIntegerConverters.BigIntegerToString.convert(original)
        val read = BigIntegerConverters.StringToBigInteger.convert(written)
        assertEquals(original, read)
    }

    @Test
    fun writeRead_roundTripsValuesWellPastLongRange() {
        // 100-digit BigInteger — utterly impossible to represent as either Long (capped at 2^63)
        // or BSON Decimal128 (capped at 34 decimal digits). String storage is the only option.
        val huge = BigInteger("1" + "9".repeat(99))
        val written = BigIntegerConverters.BigIntegerToString.convert(huge)
        val read = BigIntegerConverters.StringToBigInteger.convert(written)
        assertEquals(huge, read, "100-digit value must survive the round trip — that's why we use String storage")
        assertEquals(100, written.length, "writing converter must not truncate or scientific-notation the value")
    }

    @Test
    fun writeRead_roundTripsZero() {
        val written = BigIntegerConverters.BigIntegerToString.convert(BigInteger.ZERO)
        assertEquals("0", written)
        assertEquals(BigInteger.ZERO, BigIntegerConverters.StringToBigInteger.convert(written))
    }

    @Test
    fun reading_failsLoudlyOnMalformedString() {
        // Deliberate divergence from soul: soul swallowed blank-input as null, which masked data
        // corruption. The kudos port lets NumberFormatException propagate so a stray empty or
        // non-numeric stored value surfaces during the read rather than turning into a silent null
        // downstream.
        assertFails { BigIntegerConverters.StringToBigInteger.convert("") }
        assertFails { BigIntegerConverters.StringToBigInteger.convert("not-a-number") }
        assertFails { BigIntegerConverters.StringToBigInteger.convert("123abc") }
    }
}
