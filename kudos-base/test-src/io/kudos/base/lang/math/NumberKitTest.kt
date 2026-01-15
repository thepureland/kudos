package io.kudos.base.lang.math

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.*

/**
 * test for NumberKit
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
internal class NumberKitTest {

    //===================
    // Tests for createNumber
    //===================
    @Test
    fun createNumber_NullInput_ReturnsNull() {
        assertNull(NumberKit.createNumber(null))
    }

    @Test
    fun createNumber_EmptyOrWhitespace_ThrowsNumberFormatException() {
        assertFailsWith<NumberFormatException> { NumberKit.createNumber("") }
        assertFailsWith<NumberFormatException> { NumberKit.createNumber("   ") }
    }

    @Test
    fun createNumber_HexadecimalPositive_Works() {
        val n = NumberKit.createNumber("0xFF")
        assertTrue(n is BigInteger)
        assertEquals(BigInteger("FF", 16), n)
    }

    @Test
    fun createNumber_HexadecimalNegative_Works() {
        val n = NumberKit.createNumber("-0x1A")
        assertTrue(n is BigInteger)
        assertEquals(BigInteger("1A", 16).negate(), n)
    }

    @Test
    fun createNumber_HexadecimalInvalid_ThrowsNumberFormatException() {
        // “0x” with no digits
        assertFailsWith<NumberFormatException> { NumberKit.createNumber("0x") }
        // Invalid hex character “G”
        assertFailsWith<NumberFormatException> { NumberKit.createNumber("0x1G") }
    }

    @Test
    fun createNumber_IntegerDecimal_Works() {
        val n1 = NumberKit.createNumber("12345")
        assertTrue(n1 is BigInteger)
        assertEquals(BigInteger("12345"), n1)

        val n2 = NumberKit.createNumber("00123")
        assertTrue(n2 is BigInteger)
        // Leading zero is allowed → BigInteger("00123") == 123
        assertEquals(BigInteger("123"), n2)
    }

    @Test
    fun createNumber_IntegerInvalid_ThrowsNumberFormatException() {
        assertFailsWith<NumberFormatException> { NumberKit.createNumber("12AB") }
        assertFailsWith<NumberFormatException> { NumberKit.createNumber("123 456") }
    }

    @Test
    fun createNumber_DecimalWithPoint_Works() {
        val n = NumberKit.createNumber("123.45")
        assertTrue(n is BigDecimal)
        assertEquals(BigDecimal("123.45"), n)
    }

    @Test
    fun createNumber_ScientificNotation_Works() {
        val n1 = NumberKit.createNumber("1e3")
        assertTrue(n1 is BigDecimal)
        assertEquals(BigDecimal("1e3"), n1)

        val n2 = NumberKit.createNumber("-2.5E-2")
        assertTrue(n2 is BigDecimal)
        assertEquals(BigDecimal("-2.5E-2"), n2)
    }

    @Test
    fun createNumber_DecimalInvalid_ThrowsNumberFormatException() {
        assertFailsWith<NumberFormatException> { NumberKit.createNumber("12.3.4") }
        assertFailsWith<NumberFormatException> { NumberKit.createNumber("1e") }
    }

    //===================
    // Tests for isDigits
    //===================
    @Test
    fun isDigits_NullOrEmpty_ReturnsFalse() {
        assertFalse(NumberKit.isDigits(null))
        assertFalse(NumberKit.isDigits(""))
    }

    @Test
    fun isDigits_ValidDigitStrings_ReturnsTrue() {
        assertTrue(NumberKit.isDigits("0"))
        assertTrue(NumberKit.isDigits("0123456789"))
        assertTrue(NumberKit.isDigits("1234567890"))
    }

    @Test
    fun isDigits_InvalidDigitStrings_ReturnsFalse() {
        assertFalse(NumberKit.isDigits("123a"))
        assertFalse(NumberKit.isDigits("12 3"))
        assertFalse(NumberKit.isDigits("12.3"))
        assertFalse(NumberKit.isDigits("-123"))
    }

    //===================
    // Tests for isNumber
    //===================
    @Test
    fun isNumber_NullOrEmptyOrWhitespace_ReturnsFalse() {
        assertFalse(NumberKit.isNumber(null))
        assertFalse(NumberKit.isNumber(""))
        assertFalse(NumberKit.isNumber("   "))
    }

    @Test
    fun isNumber_ValidHexadecimal_ReturnsTrue() {
        assertTrue(NumberKit.isNumber("0x1F"))
        assertTrue(NumberKit.isNumber("-0xAbCd"))
        // Leading/trailing whitespace should be trimmed internally
        assertTrue(NumberKit.isNumber("  0x10  "))
    }

    @Test
    fun isNumber_InvalidHexadecimal_ReturnsFalse() {
        assertFalse(NumberKit.isNumber("0x"))
        assertFalse(NumberKit.isNumber("0xG1"))
        assertFalse(NumberKit.isNumber("-0x"))
        assertFalse(NumberKit.isNumber("-0xZ"))
    }

    @Test
    fun isNumber_ValidIntegerOrDecimalOrScientific_ReturnsTrue() {
        assertTrue(NumberKit.isNumber("123"))
        assertTrue(NumberKit.isNumber("  456  "))          // whitespace trimmed
        assertTrue(NumberKit.isNumber("0"))                // single zero
        assertTrue(NumberKit.isNumber("123.45"))
        assertTrue(NumberKit.isNumber("-123.45"))
        assertTrue(NumberKit.isNumber("1E3"))
        assertTrue(NumberKit.isNumber("2.5e-2"))
        assertTrue(NumberKit.isNumber("0.0"))
    }

    @Test
    fun isNumber_InvalidFormats_ReturnsFalse() {
        assertFalse(NumberKit.isNumber("abc"))
        assertFalse(NumberKit.isNumber("123L"))            // suffix “L” not recognized
        assertFalse(NumberKit.isNumber("12.3.4"))
        assertFalse(NumberKit.isNumber("1e"))
        assertFalse(NumberKit.isNumber("-."))
        assertFalse(NumberKit.isNumber("--5"))
    }

}