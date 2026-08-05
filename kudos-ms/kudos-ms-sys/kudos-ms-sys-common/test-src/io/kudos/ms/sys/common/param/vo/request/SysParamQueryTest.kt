package io.kudos.ms.sys.common.param.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.sys.common.param.vo.response.SysParamRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysParamQuery
 *
 * Covers default values (active defaults to true, others null), full-args construction,
 * ListSearchPayload overrides and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysParamQueryTest {

    @Test
    fun defaults() {
        val q = SysParamQuery()
        assertNull(q.paramName)
        assertNull(q.paramValue)
        assertNull(q.defaultValue)
        assertNull(q.atomicServiceCode)
        assertEquals(true, q.active)
    }

    @Test
    fun fullArgs() {
        val q = SysParamQuery(
            paramName = "max.size",
            paramValue = "100",
            defaultValue = "50",
            atomicServiceCode = "sys",
            active = false,
        )
        assertEquals("max.size", q.paramName)
        assertEquals("100", q.paramValue)
        assertEquals("50", q.defaultValue)
        assertEquals("sys", q.atomicServiceCode)
        assertEquals(false, q.active)
    }

    @Test
    fun payloadOverrides() {
        val q = SysParamQuery()
        assertEquals(SysParamRow::class, q.getReturnEntityClass())
        assertTrue(q.isUnpagedSearchAllowed())

        val byName = q.getOperators().mapKeys { it.key.name }
        assertEquals(2, byName.size)
        assertEquals(OperatorEnum.ILIKE, byName["paramName"])
        assertEquals(OperatorEnum.ILIKE, byName["paramValue"])
    }

    @Test
    fun dataClassContract() {
        val a = SysParamQuery(paramName = "max.size")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = false))
        assertTrue(a.toString().contains("max.size"))
    }

}
