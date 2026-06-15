package io.kudos.ms.auth.common.grant.vo.request

import io.kudos.ms.auth.common.grant.vo.response.AuthRoleGrantRequestRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthRoleGrantRequestQuery
 *
 * Covers default null fields, full construction, ListSearchPayload overrides and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleGrantRequestQueryTest {

    @Test
    fun defaults() {
        val q = AuthRoleGrantRequestQuery()
        assertNull(q.tenantId)
        assertNull(q.status)
        assertNull(q.userId)
        assertNull(q.roleId)
        assertNull(q.requesterId)
    }

    @Test
    fun fullArgs() {
        val q = AuthRoleGrantRequestQuery(
            tenantId = "t-1", status = "PENDING", userId = "u-1", roleId = "r-1", requesterId = "req-1",
        )
        assertEquals("t-1", q.tenantId)
        assertEquals("PENDING", q.status)
        assertEquals("u-1", q.userId)
        assertEquals("r-1", q.roleId)
        assertEquals("req-1", q.requesterId)
    }

    @Test
    fun payloadOverrides() {
        val q = AuthRoleGrantRequestQuery()
        assertEquals(AuthRoleGrantRequestRow::class, q.getReturnEntityClass())
        assertFalse(q.isUnpagedSearchAllowed())
        assertNull(q.getOperators())
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleGrantRequestQuery(status = "PENDING")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(status = "APPROVED"))
        assertTrue(a.toString().contains("PENDING"))
    }
}
