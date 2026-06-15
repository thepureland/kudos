package io.kudos.ms.auth.common.temporal.vo.request

import java.io.Serializable
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthRoleUserTemporalBindRequest
 *
 * Covers default null start/end times, full construction, Serializable marker and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleUserTemporalBindRequestTest {

    @Test
    fun defaultTimesAreNull() {
        val req = AuthRoleUserTemporalBindRequest(roleId = "r-1", userId = "u-1")
        assertEquals("r-1", req.roleId)
        assertEquals("u-1", req.userId)
        assertNull(req.startTime)
        assertNull(req.endTime)
    }

    @Test
    fun fullArgs() {
        val start = LocalDateTime.of(2026, 6, 15, 0, 0)
        val end = LocalDateTime.of(2026, 7, 15, 0, 0)
        val req = AuthRoleUserTemporalBindRequest(roleId = "r-1", userId = "u-1", startTime = start, endTime = end)
        assertEquals(start, req.startTime)
        assertEquals(end, req.endTime)
    }

    @Test
    fun isSerializable() {
        assertTrue(AuthRoleUserTemporalBindRequest("r-1", "u-1") is Serializable)
    }

    @Test
    fun dataClassContract() {
        val start = LocalDateTime.of(2026, 6, 15, 0, 0)
        val a = AuthRoleUserTemporalBindRequest(roleId = "r-1", userId = "u-1", startTime = start)
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(endTime = start.plusDays(1)))
        assertTrue(a.toString().contains("r-1"))
    }
}
