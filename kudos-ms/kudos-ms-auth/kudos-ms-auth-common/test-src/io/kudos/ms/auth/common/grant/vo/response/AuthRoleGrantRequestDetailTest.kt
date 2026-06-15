package io.kudos.ms.auth.common.grant.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthRoleGrantRequestDetail
 *
 * Covers all-default construction, full construction, IIdEntity id and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleGrantRequestDetailTest {

    @Test
    fun defaults() {
        val d = AuthRoleGrantRequestDetail()
        assertTrue(d is IIdEntity<*>)
        assertEquals("", d.id)
        assertNull(d.roleId)
        assertNull(d.userId)
        assertNull(d.tenantId)
        assertNull(d.status)
        assertNull(d.reason)
        assertNull(d.requesterId)
        assertNull(d.requestTime)
        assertNull(d.approverId)
        assertNull(d.decisionComment)
        assertNull(d.decisionTime)
        assertNull(d.createUserId)
        assertNull(d.createUserName)
        assertNull(d.createTime)
        assertNull(d.updateUserId)
        assertNull(d.updateUserName)
        assertNull(d.updateTime)
    }

    @Test
    fun fullArgs() {
        val t = LocalDateTime.of(2026, 6, 15, 9, 30)
        val d = AuthRoleGrantRequestDetail(
            id = "g-1", roleId = "r-1", userId = "u-1", tenantId = "t-1", status = "APPROVED",
            reason = "need", requesterId = "req-1", requestTime = t, approverId = "ap-1",
            decisionComment = "ok", decisionTime = t.plusHours(1),
            createUserId = "cu", createUserName = "cn", createTime = t,
            updateUserId = "uu", updateUserName = "un", updateTime = t.plusHours(2),
        )
        assertEquals("g-1", d.id)
        assertEquals("r-1", d.roleId)
        assertEquals("u-1", d.userId)
        assertEquals("t-1", d.tenantId)
        assertEquals("APPROVED", d.status)
        assertEquals("need", d.reason)
        assertEquals("req-1", d.requesterId)
        assertEquals(t, d.requestTime)
        assertEquals("ap-1", d.approverId)
        assertEquals("ok", d.decisionComment)
        assertEquals(t.plusHours(1), d.decisionTime)
        assertEquals("cu", d.createUserId)
        assertEquals("cn", d.createUserName)
        assertEquals("uu", d.updateUserId)
        assertEquals("un", d.updateUserName)
        assertEquals(t.plusHours(2), d.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleGrantRequestDetail(id = "g-1", status = "PENDING")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(status = "REJECTED"))
        assertTrue(a.toString().contains("g-1"))
    }
}
