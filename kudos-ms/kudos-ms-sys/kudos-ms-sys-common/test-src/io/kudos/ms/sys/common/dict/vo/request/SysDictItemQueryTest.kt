package io.kudos.ms.sys.common.dict.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.sys.common.dict.vo.response.SysDictItemRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDictItemQuery
 *
 * Covers default values, full-args construction, mutable var properties
 * (parentId/active/atomicServiceCode), ListSearchPayload overrides
 * (getReturnEntityClass / getOperators / isUnpagedSearchAllowed) and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictItemQueryTest {

    @Test
    fun defaultsAreAllNull() {
        val q = SysDictItemQuery()
        assertNull(q.id)
        assertNull(q.itemCode)
        assertNull(q.itemName)
        assertNull(q.dictId)
        assertNull(q.orderNum)
        assertNull(q.parentId)
        assertNull(q.remark)
        assertNull(q.active)
        assertNull(q.builtIn)
        assertNull(q.dictType)
        assertNull(q.dictName)
        assertNull(q.atomicServiceCode)
        assertNull(q.dictActive)
    }

    @Test
    fun fullArgsConstruction() {
        val q = SysDictItemQuery(
            id = "i-1",
            itemCode = "male",
            itemName = "Male",
            dictId = "d-1",
            orderNum = 1,
            parentId = "p-1",
            remark = "r",
            active = true,
            builtIn = false,
            dictType = "gender",
            dictName = "Gender",
            atomicServiceCode = "sys",
            dictActive = true,
        )
        assertEquals("i-1", q.id)
        assertEquals("male", q.itemCode)
        assertEquals("Male", q.itemName)
        assertEquals("d-1", q.dictId)
        assertEquals(1, q.orderNum)
        assertEquals("p-1", q.parentId)
        assertEquals("r", q.remark)
        assertEquals(true, q.active)
        assertEquals(false, q.builtIn)
        assertEquals("gender", q.dictType)
        assertEquals("Gender", q.dictName)
        assertEquals("sys", q.atomicServiceCode)
        assertEquals(true, q.dictActive)
    }

    @Test
    fun mutablePropertiesCanBeReassigned() {
        val q = SysDictItemQuery()
        q.parentId = "p-2"
        q.active = false
        q.atomicServiceCode = "other"
        assertEquals("p-2", q.parentId)
        assertEquals(false, q.active)
        assertEquals("other", q.atomicServiceCode)
    }

    @Test
    fun payloadOverrides() {
        val q = SysDictItemQuery(itemCode = "male", itemName = "Male", dictType = "gender", dictName = "Gender")
        assertEquals(SysDictItemRow::class, q.getReturnEntityClass())
        assertTrue(q.isUnpagedSearchAllowed())

        val operators = q.getOperators()
        assertEquals(4, operators.size)
        val byName = operators.mapKeys { it.key.name }
        assertEquals(OperatorEnum.ILIKE, byName["dictType"])
        assertEquals(OperatorEnum.ILIKE, byName["dictName"])
        assertEquals(OperatorEnum.ILIKE, byName["itemCode"])
        assertEquals(OperatorEnum.ILIKE, byName["itemName"])
    }

    @Test
    fun dataClassContract() {
        val a = SysDictItemQuery(itemCode = "male")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(itemCode = "female"))
        assertTrue(a.toString().contains("male"))
    }

}
