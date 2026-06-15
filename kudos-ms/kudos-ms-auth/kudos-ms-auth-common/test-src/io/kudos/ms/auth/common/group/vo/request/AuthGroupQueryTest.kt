package io.kudos.ms.auth.common.group.vo.request

import io.kudos.ms.auth.common.group.vo.response.AuthGroupRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthGroupQuery
 *
 * Covers default null fields, full construction, ListSearchPayload overrides and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthGroupQueryTest {

    @Test
    fun defaults() {
        val q = AuthGroupQuery()
        assertNull(q.code)
        assertNull(q.name)
        assertNull(q.tenantId)
        assertNull(q.subsysCode)
        assertNull(q.active)
        assertNull(q.builtIn)
    }

    @Test
    fun fullArgs() {
        val q = AuthGroupQuery(
            code = "G1", name = "Group 1", tenantId = "t-1", subsysCode = "sys", active = true, builtIn = false,
        )
        assertEquals("G1", q.code)
        assertEquals("Group 1", q.name)
        assertEquals("t-1", q.tenantId)
        assertEquals("sys", q.subsysCode)
        assertEquals(true, q.active)
        assertEquals(false, q.builtIn)
    }

    @Test
    fun payloadOverrides() {
        val q = AuthGroupQuery()
        assertEquals(AuthGroupRow::class, q.getReturnEntityClass())
        assertFalse(q.isUnpagedSearchAllowed())
        assertNull(q.getOperators())
    }

    @Test
    fun dataClassContract() {
        val a = AuthGroupQuery(code = "G1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = true))
        assertTrue(a.toString().contains("G1"))
    }
}
