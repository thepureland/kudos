package io.kudos.ms.sys.common.dict.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.sys.common.dict.vo.response.SysDictRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDictQuery
 *
 * Covers default values, full-args construction, ListSearchPayload overrides
 * (getReturnEntityClass / getOperators / isUnpagedSearchAllowed) and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictQueryTest {

    @Test
    fun defaultsAreAllNull() {
        val q = SysDictQuery()
        assertNull(q.id)
        assertNull(q.dictType)
        assertNull(q.dictName)
        assertNull(q.atomicServiceCode)
        assertNull(q.active)
        assertNull(q.builtIn)
    }

    @Test
    fun fullArgsConstruction() {
        val q = SysDictQuery(
            id = "d-1",
            dictType = "gender",
            dictName = "Gender",
            atomicServiceCode = "sys",
            active = true,
            builtIn = false,
        )
        assertEquals("d-1", q.id)
        assertEquals("gender", q.dictType)
        assertEquals("Gender", q.dictName)
        assertEquals("sys", q.atomicServiceCode)
        assertEquals(true, q.active)
        assertEquals(false, q.builtIn)
    }

    @Test
    fun payloadOverrides() {
        val q = SysDictQuery(dictType = "gender", dictName = "Gender")
        assertEquals(SysDictRow::class, q.getReturnEntityClass())
        assertTrue(q.isUnpagedSearchAllowed())

        val operators = q.getOperators()
        assertEquals(2, operators.size)
        val byName = operators.mapKeys { it.key.name }
        assertEquals(OperatorEnum.ILIKE, byName["dictType"])
        assertEquals(OperatorEnum.ILIKE, byName["dictName"])
    }

    @Test
    fun dataClassContract() {
        val a = SysDictQuery(dictType = "gender")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = true))
        assertTrue(a.toString().contains("gender"))
    }

}
