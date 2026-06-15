package io.kudos.ms.sys.common.datasource.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.sys.common.datasource.vo.response.SysDataSourceRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDataSourceQuery
 *
 * Covers default values (all null), full-args construction, ListSearchPayload overrides
 * and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDataSourceQueryTest {

    @Test
    fun defaults() {
        val q = SysDataSourceQuery()
        assertNull(q.name)
        assertNull(q.subSystemCode)
        assertNull(q.microServiceCode)
        assertNull(q.tenantId)
        assertNull(q.active)
    }

    @Test
    fun fullArgs() {
        val q = SysDataSourceQuery(
            name = "ds1",
            subSystemCode = "sys",
            microServiceCode = "ms",
            tenantId = "t-1",
            active = true,
        )
        assertEquals("ds1", q.name)
        assertEquals("sys", q.subSystemCode)
        assertEquals("ms", q.microServiceCode)
        assertEquals("t-1", q.tenantId)
        assertEquals(true, q.active)
    }

    @Test
    fun payloadOverrides() {
        val q = SysDataSourceQuery()
        assertEquals(SysDataSourceRow::class, q.getReturnEntityClass())
        assertTrue(q.isUnpagedSearchAllowed())

        val byName = q.getOperators().mapKeys { it.key.name }
        assertEquals(1, byName.size)
        assertEquals(OperatorEnum.ILIKE, byName["name"])
    }

    @Test
    fun dataClassContract() {
        val a = SysDataSourceQuery(name = "ds1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = true))
        assertTrue(a.toString().contains("ds1"))
    }

}
