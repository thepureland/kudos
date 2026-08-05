package io.kudos.ms.sys.common.cache.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.sys.common.cache.vo.response.SysCacheRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysCacheQuery
 *
 * Covers default values (active defaults to true, others null), full-args construction,
 * ListSearchPayload overrides (single ILIKE operator on name) and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysCacheQueryTest {

    @Test
    fun defaults() {
        val q = SysCacheQuery()
        assertNull(q.name)
        assertNull(q.atomicServiceCode)
        assertNull(q.strategyDictCode)
        assertNull(q.hash)
        assertEquals(true, q.active)
    }

    @Test
    fun fullArgs() {
        val q = SysCacheQuery(
            name = "userCache",
            atomicServiceCode = "sys",
            strategyDictCode = "LOCAL",
            hash = true,
            active = false,
        )
        assertEquals("userCache", q.name)
        assertEquals("sys", q.atomicServiceCode)
        assertEquals("LOCAL", q.strategyDictCode)
        assertEquals(true, q.hash)
        assertEquals(false, q.active)
    }

    @Test
    fun payloadOverrides() {
        val q = SysCacheQuery()
        assertEquals(SysCacheRow::class, q.getReturnEntityClass())
        assertTrue(q.isUnpagedSearchAllowed())

        val byName = q.getOperators().mapKeys { it.key.name }
        assertEquals(1, byName.size)
        assertEquals(OperatorEnum.ILIKE, byName["name"])
    }

    @Test
    fun dataClassContract() {
        val a = SysCacheQuery(name = "userCache")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(hash = true))
        assertTrue(a.toString().contains("userCache"))
    }

}
