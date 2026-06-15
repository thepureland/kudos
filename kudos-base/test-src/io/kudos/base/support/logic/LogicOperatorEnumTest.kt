package io.kudos.base.support.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * LogicOperatorEnum test cases.
 *
 * Each operator's compare() delegates to the matching OperatorEnum, so this exercises every
 * branch of the when-block (one assertion per operator), plus the code/displayText/flag metadata
 * and enumOf parsing (case-insensitive, blank, and not-found paths).
 *
 * @author K
 * @since 1.0.0
 */
internal class LogicOperatorEnumTest {

    // ============================================================
    // compare() - one true case per operator to cover every branch
    // ============================================================

    @Test fun eq() {
        assertTrue(LogicOperatorEnum.EQ.compare("a", "a"))
        assertFalse(LogicOperatorEnum.EQ.compare("a", "b"))
    }

    @Test fun ieq() {
        assertTrue(LogicOperatorEnum.IEQ.compare("Abc", "ABC"))
        assertFalse(LogicOperatorEnum.IEQ.compare("Abc", "xyz"))
    }

    @Test fun ne() {
        assertTrue(LogicOperatorEnum.NE.compare("a", "b"))
        assertFalse(LogicOperatorEnum.NE.compare("a", "a"))
    }

    @Test fun lg() {
        // LG delegates to OperatorEnum.NE, same semantics as NE
        assertTrue(LogicOperatorEnum.LG.compare("a", "b"))
        assertFalse(LogicOperatorEnum.LG.compare("a", "a"))
    }

    @Test fun ge() {
        assertTrue(LogicOperatorEnum.GE.compare(5, 5))
        assertFalse(LogicOperatorEnum.GE.compare(3, 5))
    }

    @Test fun le() {
        assertTrue(LogicOperatorEnum.LE.compare(3, 5))
        assertFalse(LogicOperatorEnum.LE.compare(5, 3))
    }

    @Test fun gt() {
        assertTrue(LogicOperatorEnum.GT.compare(5, 3))
        assertFalse(LogicOperatorEnum.GT.compare(3, 5))
    }

    @Test fun lt() {
        assertTrue(LogicOperatorEnum.LT.compare(3, 5))
        assertFalse(LogicOperatorEnum.LT.compare(5, 3))
    }

    @Test fun like() {
        assertTrue(LogicOperatorEnum.LIKE.compare("hello world", "lo wo"))
        assertFalse(LogicOperatorEnum.LIKE.compare("hello", "xyz"))
    }

    @Test fun likeS() {
        assertTrue(LogicOperatorEnum.LIKE_S.compare("hello world", "hello"))
        assertFalse(LogicOperatorEnum.LIKE_S.compare("hello", "world"))
    }

    @Test fun likeE() {
        assertTrue(LogicOperatorEnum.LIKE_E.compare("hello world", "world"))
        assertFalse(LogicOperatorEnum.LIKE_E.compare("hello", "abc"))
    }

    @Test fun ilike() {
        assertTrue(LogicOperatorEnum.ILIKE.compare("Hello WORLD", "lo wo"))
        assertFalse(LogicOperatorEnum.ILIKE.compare("Hello", "xyz"))
    }

    @Test fun ilikeS() {
        assertTrue(LogicOperatorEnum.ILIKE_S.compare("HELLO world", "hello"))
        assertFalse(LogicOperatorEnum.ILIKE_S.compare("HELLO world", "world"))
    }

    @Test fun ilikeE() {
        assertTrue(LogicOperatorEnum.ILIKE_E.compare("hello WORLD", "world"))
        assertFalse(LogicOperatorEnum.ILIKE_E.compare("hello WORLD", "hello"))
    }

    @Test fun inOp() {
        assertTrue(LogicOperatorEnum.IN.compare(2, listOf(1, 2, 3)))
        assertFalse(LogicOperatorEnum.IN.compare(4, listOf(1, 2, 3)))
    }

    @Test fun notIn() {
        assertTrue(LogicOperatorEnum.NOT_IN.compare(4, listOf(1, 2, 3)))
        assertFalse(LogicOperatorEnum.NOT_IN.compare(2, listOf(1, 2, 3)))
    }

    @Test fun isNull() {
        assertTrue(LogicOperatorEnum.IS_NULL.compare(null))
        assertFalse(LogicOperatorEnum.IS_NULL.compare("x"))
    }

    @Test fun isNotNull() {
        assertTrue(LogicOperatorEnum.IS_NOT_NULL.compare("x"))
        assertFalse(LogicOperatorEnum.IS_NOT_NULL.compare(null))
    }

    @Test fun isEmpty() {
        assertTrue(LogicOperatorEnum.IS_EMPTY.compare(""))
        assertFalse(LogicOperatorEnum.IS_EMPTY.compare("x"))
    }

    @Test fun isNotEmpty() {
        assertTrue(LogicOperatorEnum.IS_NOT_EMPTY.compare("x"))
        assertFalse(LogicOperatorEnum.IS_NOT_EMPTY.compare(""))
    }

    @Test
    fun testCompareDefaultsSecondArgToNull() {
        // v2 has a default of null; IS_NULL ignores v2 entirely
        assertTrue(LogicOperatorEnum.IS_NULL.compare(null))
    }

    // ============================================================
    // metadata
    // ============================================================

    @Test
    fun testMetadata() {
        assertEquals("=", LogicOperatorEnum.EQ.code)
        assertEquals("Equal", LogicOperatorEnum.EQ.displayText)
        assertTrue(LogicOperatorEnum.IEQ.stringOnly)
        assertFalse(LogicOperatorEnum.EQ.stringOnly)
        assertTrue(LogicOperatorEnum.IS_NULL.acceptNull)
        assertFalse(LogicOperatorEnum.EQ.acceptNull)
    }

    // ============================================================
    // enumOf
    // ============================================================

    @Test
    fun testEnumOfUppercaseCode() {
        assertEquals(LogicOperatorEnum.LIKE, LogicOperatorEnum.enumOf("LIKE"))
        assertEquals(LogicOperatorEnum.EQ, LogicOperatorEnum.enumOf("="))
    }

    @Test
    fun testEnumOfIsCaseInsensitive() {
        assertEquals(LogicOperatorEnum.LIKE, LogicOperatorEnum.enumOf("like"))
        assertEquals(LogicOperatorEnum.ILIKE, LogicOperatorEnum.enumOf("ilike"))
    }

    @Test
    fun testEnumOfUnknownReturnsNull() {
        assertNull(LogicOperatorEnum.enumOf("NOPE"))
    }

    @Test
    fun testEnumOfBlankThrows() {
        // blank skips the uppercase() branch; EnumKit.enumOf then rejects blank codes with IllegalArgumentException
        assertFailsWith<IllegalArgumentException> { LogicOperatorEnum.enumOf("   ") }
    }
}
