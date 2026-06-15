package io.kudos.base.model.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * ErrorDetail test cases.
 *
 * Covers: default-value behavior for the optional fields, required message,
 * data class equals/hashCode/copy and rejectedValue holding arbitrary types.
 *
 * @author K
 * @since 1.0.0
 */
internal class ErrorDetailTest {

    @Test
    fun testOnlyMessageRequiredOthersDefaultToNull() {
        val d = ErrorDetail(message = "boom")
        assertEquals("boom", d.message)
        assertNull(d.code)
        assertNull(d.field)
        assertNull(d.target)
        assertNull(d.rejectedValue)
    }

    @Test
    fun testAllFieldsPopulated() {
        val d = ErrorDetail(
            code = "REQUIRED",
            field = "name",
            target = "row[3]",
            message = "name cannot be empty",
            rejectedValue = ""
        )
        assertEquals("REQUIRED", d.code)
        assertEquals("name", d.field)
        assertEquals("row[3]", d.target)
        assertEquals("name cannot be empty", d.message)
        assertEquals("", d.rejectedValue)
    }

    @Test
    fun testRejectedValueHoldsArbitraryType() {
        val list = listOf(1, 2, 3)
        val d = ErrorDetail(message = "bad list", rejectedValue = list)
        assertEquals(list, d.rejectedValue)

        val numeric = ErrorDetail(message = "bad number", rejectedValue = -1)
        assertEquals(-1, numeric.rejectedValue)
    }

    @Test
    fun testEqualsAndHashCode() {
        val a = ErrorDetail(code = "C", field = "f", message = "m", rejectedValue = 1)
        val b = ErrorDetail(code = "C", field = "f", message = "m", rejectedValue = 1)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testEqualsDistinguishesMessage() {
        assertNotEquals(
            ErrorDetail(message = "a"),
            ErrorDetail(message = "b")
        )
    }

    @Test
    fun testCopy() {
        val original = ErrorDetail(code = "C", message = "m")
        val copied = original.copy(message = "m2")
        assertEquals("C", copied.code)
        assertEquals("m2", copied.message)
        assertEquals("m", original.message)
    }
}
