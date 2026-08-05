package io.kudos.ms.sys.common.domain.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.sys.common.domain.vo.response.SysDomainRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDomainQuery
 *
 * Covers default values (all null), full-args construction, ListSearchPayload overrides
 * (single ILIKE operator on domain) and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDomainQueryTest {

    @Test
    fun defaults() {
        val q = SysDomainQuery()
        assertNull(q.domain)
        assertNull(q.systemCode)
        assertNull(q.tenantId)
        assertNull(q.active)
    }

    @Test
    fun fullArgs() {
        val q = SysDomainQuery(
            domain = "example.com",
            systemCode = "sys",
            tenantId = "t-1",
            active = true,
        )
        assertEquals("example.com", q.domain)
        assertEquals("sys", q.systemCode)
        assertEquals("t-1", q.tenantId)
        assertEquals(true, q.active)
    }

    @Test
    fun payloadOverrides() {
        val q = SysDomainQuery()
        assertEquals(SysDomainRow::class, q.getReturnEntityClass())
        assertTrue(q.isUnpagedSearchAllowed())

        val byName = q.getOperators().mapKeys { it.key.name }
        assertEquals(1, byName.size)
        assertEquals(OperatorEnum.ILIKE, byName["domain"])
    }

    @Test
    fun dataClassContract() {
        val a = SysDomainQuery(domain = "example.com")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(domain = "other.com"))
        assertTrue(a.toString().contains("example.com"))
    }

}
