package io.kudos.ms.auth.common.grant.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthRoleGrantRequestRow
 *
 * Covers all-default construction, full construction, IIdEntity id and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleGrantRequestRowTest {

    @Test
    fun defaults() {
        val r = AuthRoleGrantRequestRow()
        assertTrue(r is IIdEntity<*>)
        assertEquals("", r.id)
        assertNull(r.roleId)
        assertNull(r.userId)
        assertNull(r.tenantId)
        assertNull(r.status)
        assertNull(r.reason)
        assertNull(r.requesterId)
        assertNull(r.requestTime)
        assertNull(r.approverId)
        assertNull(r.decisionComment)
        assertNull(r.decisionTime)
    }

    @Test
    fun fullArgs() {
        val t = LocalDateTime.of(2026, 6, 15, 9, 30)
        val r = AuthRoleGrantRequestRow(
            id = "g-1", roleId = "r-1", userId = "u-1", tenantId = "t-1", status = "PENDING",
            reason = "need", requesterId = "req-1", requestTime = t, approverId = "ap-1",
            decisionComment = "ok", decisionTime = t.plusHours(1),
        )
        assertEquals("g-1", r.id)
        assertEquals("r-1", r.roleId)
        assertEquals("u-1", r.userId)
        assertEquals("t-1", r.tenantId)
        assertEquals("PENDING", r.status)
        assertEquals("need", r.reason)
        assertEquals("req-1", r.requesterId)
        assertEquals(t, r.requestTime)
        assertEquals("ap-1", r.approverId)
        assertEquals("ok", r.decisionComment)
        assertEquals(t.plusHours(1), r.decisionTime)
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleGrantRequestRow(id = "g-1", status = "PENDING")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(status = "CANCELLED"))
        assertTrue(a.toString().contains("g-1"))
    }
}
