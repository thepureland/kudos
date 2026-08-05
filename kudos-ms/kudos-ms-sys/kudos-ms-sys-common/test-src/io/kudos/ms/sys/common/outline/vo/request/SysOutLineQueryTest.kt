package io.kudos.ms.sys.common.outline.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.sys.common.outline.vo.response.SysOutLineRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysOutLineQuery
 *
 * Covers default values (all null), full-args construction, ListSearchPayload overrides
 * (two ILIKE operators) and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysOutLineQueryTest {

    @Test
    fun defaults() {
        val q = SysOutLineQuery()
        assertNull(q.name)
        assertNull(q.host)
        assertNull(q.protocol)
        assertNull(q.systemCode)
        assertNull(q.tenantId)
        assertNull(q.active)
    }

    @Test
    fun fullArgs() {
        val q = SysOutLineQuery(
            name = "wl",
            host = "example.com",
            protocol = "https",
            systemCode = "sys",
            tenantId = "t-1",
            active = true,
        )
        assertEquals("wl", q.name)
        assertEquals("example.com", q.host)
        assertEquals("https", q.protocol)
        assertEquals("sys", q.systemCode)
        assertEquals("t-1", q.tenantId)
        assertEquals(true, q.active)
    }

    @Test
    fun payloadOverrides() {
        val q = SysOutLineQuery()
        assertEquals(SysOutLineRow::class, q.getReturnEntityClass())
        assertTrue(q.isUnpagedSearchAllowed())

        val byName = q.getOperators().mapKeys { it.key.name }
        assertEquals(2, byName.size)
        assertEquals(OperatorEnum.ILIKE, byName["name"])
        assertEquals(OperatorEnum.ILIKE, byName["host"])
    }

    @Test
    fun dataClassContract() {
        val a = SysOutLineQuery(name = "wl")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "other"))
        assertTrue(a.toString().contains("wl"))
    }

}
