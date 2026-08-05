package io.kudos.ms.sys.common.system.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.sys.common.system.vo.response.SysSystemRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysSystemQuery
 *
 * Covers default values (code/name/subSystem null, active true), full-args construction,
 * ListSearchPayload overrides (ILIKE on code and name) and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysSystemQueryTest {

    @Test
    fun defaults() {
        val q = SysSystemQuery()
        assertNull(q.code)
        assertNull(q.name)
        assertNull(q.subSystem)
        assertEquals(true, q.active)
    }

    @Test
    fun fullArgs() {
        val q = SysSystemQuery(code = "sys", name = "System", subSystem = true, active = false)
        assertEquals("sys", q.code)
        assertEquals("System", q.name)
        assertEquals(true, q.subSystem)
        assertEquals(false, q.active)
    }

    @Test
    fun payloadOverrides() {
        val q = SysSystemQuery()
        assertEquals(SysSystemRow::class, q.getReturnEntityClass())
        assertTrue(q.isUnpagedSearchAllowed())

        val byName = q.getOperators().mapKeys { it.key.name }
        assertEquals(2, byName.size)
        assertEquals(OperatorEnum.ILIKE, byName["code"])
        assertEquals(OperatorEnum.ILIKE, byName["name"])
    }

    @Test
    fun dataClassContract() {
        val a = SysSystemQuery(code = "sys")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(code = "other"))
        assertTrue(a.toString().contains("sys"))
    }

}
