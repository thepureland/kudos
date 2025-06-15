package io.kudos.base.lang

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse


/**
 * test for XChar.kt
 *
 * @author K
 * @since 1.0.0
 */
class XCharTest {

    @Test
    fun unicodeEscaped() {
        assertEquals("\\u0020", ' '.unicodeEscaped())
        assertEquals("\\u0041", 'A'.unicodeEscaped())
    }

    @Test
    fun isAscii() {
        assert('a'.isAscii())
        assert('A'.isAscii())
        assert('3'.isAscii())
        assert('-'.isAscii())
        assert('\n'.isAscii())
    }

    @Test
    fun isAsciiPrintable() {
        assert('a'.isAsciiPrintable())
        assert('A'.isAsciiPrintable())
        assert('3'.isAsciiPrintable())
        assert('-'.isAsciiPrintable())
        assertFalse('\n'.isAsciiPrintable())
    }

    @Test
    fun isAsciiControl() {
        assertFalse('a'.isAsciiControl())
        assertFalse('A'.isAsciiControl())
        assertFalse('3'.isAsciiControl())
        assertFalse('-'.isAsciiControl())
        assert('\n'.isAsciiControl())
    }

    @Test
    fun isAsciiAlpha() {
        assert('a'.isAsciiAlpha())
        assert('A'.isAsciiAlpha())
        assertFalse('3'.isAsciiAlpha())
        assertFalse('-'.isAsciiAlpha())
        assertFalse('\n'.isAsciiAlpha())
    }

    @Test
    fun isAsciiAlphaUpper() {
        assertFalse('a'.isAsciiAlphaUpper())
        assert('A'.isAsciiAlphaUpper())
        assertFalse('3'.isAsciiAlphaUpper())
        assertFalse('-'.isAsciiAlphaUpper())
        assertFalse('\n'.isAsciiAlphaUpper())
    }

    @Test
    fun isAsciiAlphaLower() {
        assert('a'.isAsciiAlphaLower())
        assertFalse('A'.isAsciiAlphaLower())
        assertFalse('3'.isAsciiAlphaLower())
        assertFalse('-'.isAsciiAlphaLower())
        assertFalse('\n'.isAsciiAlphaLower())
    }

    @Test
    fun isAsciiNumeric() {
        assertFalse('a'.isAsciiNumeric())
        assertFalse('A'.isAsciiNumeric())
        assert('3'.isAsciiNumeric())
        assertFalse('-'.isAsciiNumeric())
        assertFalse('\n'.isAsciiNumeric())
    }

    @Test
    fun isAsciiAlphanumeric() {
        assert('a'.isAsciiAlphanumeric())
        assert('A'.isAsciiAlphanumeric())
        assert('3'.isAsciiAlphanumeric())
        assertFalse('-'.isAsciiAlphanumeric())
        assertFalse('\n'.isAsciiAlphanumeric())
    }

}