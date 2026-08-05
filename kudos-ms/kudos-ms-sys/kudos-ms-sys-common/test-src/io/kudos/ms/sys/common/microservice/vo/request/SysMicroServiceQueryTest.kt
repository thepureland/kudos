package io.kudos.ms.sys.common.microservice.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.sys.common.microservice.vo.response.SysMicroServiceRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysMicroServiceQuery
 *
 * Covers default values (active defaults to true, others null), full-args construction,
 * ListSearchPayload overrides (two ILIKE operators) and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysMicroServiceQueryTest {

    @Test
    fun defaults() {
        val q = SysMicroServiceQuery()
        assertNull(q.code)
        assertNull(q.name)
        assertNull(q.atomicService)
        assertEquals(true, q.active)
    }

    @Test
    fun fullArgs() {
        val q = SysMicroServiceQuery(
            code = "ms_a",
            name = "Service A",
            atomicService = false,
            active = false,
        )
        assertEquals("ms_a", q.code)
        assertEquals("Service A", q.name)
        assertEquals(false, q.atomicService)
        assertEquals(false, q.active)
    }

    @Test
    fun payloadOverrides() {
        val q = SysMicroServiceQuery()
        assertEquals(SysMicroServiceRow::class, q.getReturnEntityClass())
        assertTrue(q.isUnpagedSearchAllowed())

        val byName = q.getOperators().mapKeys { it.key.name }
        assertEquals(2, byName.size)
        assertEquals(OperatorEnum.ILIKE, byName["code"])
        assertEquals(OperatorEnum.ILIKE, byName["name"])
    }

    @Test
    fun dataClassContract() {
        val a = SysMicroServiceQuery(code = "ms_a")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(code = "ms_b"))
        assertTrue(a.toString().contains("ms_a"))
    }

}
