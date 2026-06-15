package io.kudos.base.bean.validation.support

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test cases for [AssertLogicEnum.compare], covering every enum branch and its null / String /
 * collection / array / map / other-object handling.
 *
 * @author K
 * @since 1.0.0
 */
internal class AssertLogicEnumTest {

    @Test
    fun isNull() {
        assertTrue(AssertLogicEnum.IS_NULL.compare(null))
        assertFalse(AssertLogicEnum.IS_NULL.compare("x"))
    }

    @Test
    fun isNotNull() {
        assertTrue(AssertLogicEnum.IS_NOT_NULL.compare("x"))
        assertFalse(AssertLogicEnum.IS_NOT_NULL.compare(null))
    }

    @Test
    fun isEmpty() {
        assertTrue(AssertLogicEnum.IS_EMPTY.compare(""))
        assertTrue(AssertLogicEnum.IS_EMPTY.compare(emptyList<String>()))
        assertTrue(AssertLogicEnum.IS_EMPTY.compare(emptyMap<String, String>()))
        assertFalse(AssertLogicEnum.IS_EMPTY.compare("a"))
        assertFalse(AssertLogicEnum.IS_EMPTY.compare(listOf("a")))
    }

    @Test
    fun isNotEmpty() {
        assertTrue(AssertLogicEnum.IS_NOT_EMPTY.compare("a"))
        assertTrue(AssertLogicEnum.IS_NOT_EMPTY.compare(listOf("a")))
        assertFalse(AssertLogicEnum.IS_NOT_EMPTY.compare(""))
        assertFalse(AssertLogicEnum.IS_NOT_EMPTY.compare(emptyList<String>()))
    }

    @Test
    fun isBlank_withNullIsTrue() {
        assertTrue(AssertLogicEnum.IS_BLANK.compare(null))
    }

    @Test
    fun isBlank_withStringUsesIsBlank() {
        assertTrue(AssertLogicEnum.IS_BLANK.compare(""))
        assertTrue(AssertLogicEnum.IS_BLANK.compare("   "))
        assertFalse(AssertLogicEnum.IS_BLANK.compare("a"))
    }

    @Test
    fun isBlank_withNonStringFallsBackToNullOrEmpty() {
        assertTrue(AssertLogicEnum.IS_BLANK.compare(emptyList<String>()))
        assertFalse(AssertLogicEnum.IS_BLANK.compare(listOf("a")))
    }

    @Test
    fun isNotBlank_withNullIsFalse() {
        assertFalse(AssertLogicEnum.IS_NOT_BLANK.compare(null))
    }

    @Test
    fun isNotBlank_withStringUsesIsNotBlank() {
        assertTrue(AssertLogicEnum.IS_NOT_BLANK.compare("a"))
        assertFalse(AssertLogicEnum.IS_NOT_BLANK.compare("   "))
        assertFalse(AssertLogicEnum.IS_NOT_BLANK.compare(""))
    }

    @Test
    fun isNotBlank_withNonStringFallsBackToNotNullAndNotEmpty() {
        assertTrue(AssertLogicEnum.IS_NOT_BLANK.compare(listOf("a")))
        assertFalse(AssertLogicEnum.IS_NOT_BLANK.compare(emptyList<String>()))
    }

    @Test
    fun allEnumValuesPresent() {
        assertEquals(6, AssertLogicEnum.entries.size)
    }
}
