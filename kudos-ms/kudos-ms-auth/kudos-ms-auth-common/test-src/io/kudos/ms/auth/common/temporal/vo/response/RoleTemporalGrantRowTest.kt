package io.kudos.ms.auth.common.temporal.vo.response

import java.io.Serializable
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for RoleTemporalGrantRow
 *
 * Covers default null start/end times, the required active flag, full construction,
 * Serializable marker and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class RoleTemporalGrantRowTest {

    @Test
    fun defaultTimesAreNull() {
        val row = RoleTemporalGrantRow(id = "g-1", userId = "u-1", active = true)
        assertEquals("g-1", row.id)
        assertEquals("u-1", row.userId)
        assertNull(row.startTime)
        assertNull(row.endTime)
        assertTrue(row.active)
    }

    @Test
    fun fullArgs() {
        val start = LocalDateTime.of(2026, 6, 15, 0, 0)
        val end = LocalDateTime.of(2026, 7, 15, 0, 0)
        val row = RoleTemporalGrantRow(id = "g-1", userId = "u-1", startTime = start, endTime = end, active = false)
        assertEquals(start, row.startTime)
        assertEquals(end, row.endTime)
        assertFalse(row.active)
    }

    @Test
    fun isSerializable() {
        assertTrue(RoleTemporalGrantRow(id = "g-1", userId = "u-1", active = true) is Serializable)
    }

    @Test
    fun dataClassContract() {
        val a = RoleTemporalGrantRow(id = "g-1", userId = "u-1", active = true)
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = false))
        assertTrue(a.toString().contains("g-1"))
    }
}
