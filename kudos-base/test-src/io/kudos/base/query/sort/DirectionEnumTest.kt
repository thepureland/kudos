package io.kudos.base.query.sort

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * DirectionEnum test cases.
 *
 * Covers: entries, fromString case-insensitivity, and the illegal-value error path
 * (message content and exception type).
 *
 * @author K
 * @since 1.0.0
 */
internal class DirectionEnumTest {

    @Test
    fun testEntries() {
        assertEquals(listOf(DirectionEnum.ASC, DirectionEnum.DESC), DirectionEnum.entries)
    }

    @Test
    fun testFromStringExactCase() {
        assertEquals(DirectionEnum.ASC, DirectionEnum.fromString("ASC"))
        assertEquals(DirectionEnum.DESC, DirectionEnum.fromString("DESC"))
    }

    @Test
    fun testFromStringIsCaseInsensitive() {
        assertEquals(DirectionEnum.ASC, DirectionEnum.fromString("asc"))
        assertEquals(DirectionEnum.DESC, DirectionEnum.fromString("desc"))
        assertEquals(DirectionEnum.ASC, DirectionEnum.fromString("Asc"))
        assertEquals(DirectionEnum.DESC, DirectionEnum.fromString("dEsC"))
    }

    @Test
    fun testFromStringIllegalValueThrows() {
        val ex = assertFailsWith<IllegalStateException> {
            DirectionEnum.fromString("sideways")
        }
        val msg = ex.message ?: ""
        assertTrue(msg.contains("sideways"), "error message should echo the bad value: $msg")
        assertTrue(msg.contains("desc") && msg.contains("asc"), "error message should mention valid values: $msg")
    }

    @Test
    fun testFromStringEmptyThrows() {
        assertFailsWith<IllegalStateException> {
            DirectionEnum.fromString("")
        }
    }
}
