package io.kudos.ms.sys.common.tenant.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.sys.common.tenant.vo.response.SysTenantRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysTenantQuery
 *
 * Covers default values (name/subSystemCode null, active true), full-args construction,
 * ListSearchPayload overrides (ILIKE on name, LIKE on subSystemCode) and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantQueryTest {

    @Test
    fun defaults() {
        val q = SysTenantQuery()
        assertNull(q.name)
        assertNull(q.subSystemCode)
        assertEquals(true, q.active)
    }

    @Test
    fun fullArgs() {
        val q = SysTenantQuery(name = "Tenant", subSystemCode = "sys", active = false)
        assertEquals("Tenant", q.name)
        assertEquals("sys", q.subSystemCode)
        assertEquals(false, q.active)
    }

    @Test
    fun payloadOverrides() {
        val q = SysTenantQuery()
        assertEquals(SysTenantRow::class, q.getReturnEntityClass())
        assertTrue(q.isUnpagedSearchAllowed())

        val byName = q.getOperators().mapKeys { it.key.name }
        assertEquals(2, byName.size)
        assertEquals(OperatorEnum.ILIKE, byName["name"])
        assertEquals(OperatorEnum.LIKE, byName["subSystemCode"])
    }

    @Test
    fun dataClassContract() {
        val a = SysTenantQuery(name = "Tenant")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "Other"))
        assertTrue(a.toString().contains("Tenant"))
    }

}
