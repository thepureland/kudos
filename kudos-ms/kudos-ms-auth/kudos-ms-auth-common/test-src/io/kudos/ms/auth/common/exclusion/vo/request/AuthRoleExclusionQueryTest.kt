package io.kudos.ms.auth.common.exclusion.vo.request

import io.kudos.ms.auth.common.exclusion.vo.response.AuthRoleExclusionRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthRoleExclusionQuery
 *
 * Covers default null fields, full construction, the ListSearchPayload overrides
 * (getReturnEntityClass / defaults) and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleExclusionQueryTest {

    @Test
    fun defaults() {
        val q = AuthRoleExclusionQuery()
        assertNull(q.tenantId)
        assertNull(q.roleId)
    }

    @Test
    fun fullArgs() {
        val q = AuthRoleExclusionQuery(tenantId = "t-1", roleId = "r-1")
        assertEquals("t-1", q.tenantId)
        assertEquals("r-1", q.roleId)
    }

    @Test
    fun payloadOverrides() {
        val q = AuthRoleExclusionQuery()
        assertEquals(AuthRoleExclusionRow::class, q.getReturnEntityClass())
        // inherited defaults
        assertFalse(q.isUnpagedSearchAllowed())
        assertEquals(100, q.getMaxPageSize())
        assertNull(q.getOperators())
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleExclusionQuery(tenantId = "t-1", roleId = "r-1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(roleId = "r-2"))
        assertTrue(a.toString().contains("t-1"))
    }
}
