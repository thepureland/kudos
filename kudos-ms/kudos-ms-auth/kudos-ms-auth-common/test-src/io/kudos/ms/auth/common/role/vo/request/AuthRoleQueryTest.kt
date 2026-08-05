package io.kudos.ms.auth.common.role.vo.request

import io.kudos.ms.auth.common.role.vo.response.AuthRoleRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthRoleQuery
 *
 * Covers default null fields, full construction, ListSearchPayload overrides and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleQueryTest {

    @Test
    fun defaults() {
        val q = AuthRoleQuery()
        assertNull(q.code)
        assertNull(q.name)
        assertNull(q.tenantId)
        assertNull(q.subsysCode)
        assertNull(q.parentId)
        assertNull(q.dataScope)
        assertNull(q.remark)
        assertNull(q.active)
        assertNull(q.builtIn)
    }

    @Test
    fun fullArgs() {
        val q = AuthRoleQuery(
            code = "ROLE1", name = "Role 1", tenantId = "t-1", subsysCode = "sys",
            parentId = "p-1", dataScope = "ORG", remark = "r", active = true, builtIn = false,
        )
        assertEquals("ROLE1", q.code)
        assertEquals("Role 1", q.name)
        assertEquals("t-1", q.tenantId)
        assertEquals("sys", q.subsysCode)
        assertEquals("p-1", q.parentId)
        assertEquals("ORG", q.dataScope)
        assertEquals("r", q.remark)
        assertEquals(true, q.active)
        assertEquals(false, q.builtIn)
    }

    @Test
    fun payloadOverrides() {
        val q = AuthRoleQuery()
        assertEquals(AuthRoleRow::class, q.getReturnEntityClass())
        assertFalse(q.isUnpagedSearchAllowed())
        assertNull(q.getOperators())
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleQuery(code = "ROLE1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(dataScope = "ALL"))
        assertTrue(a.toString().contains("ROLE1"))
    }
}
