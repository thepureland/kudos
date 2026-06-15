package io.kudos.ms.sys.common.dict.vo.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDictItemRow
 *
 * Covers default values, full-args construction, mutable parentCode/parentIds and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictItemRowTest {

    @Test
    fun defaults() {
        val r = SysDictItemRow()
        assertEquals("", r.id)
        assertEquals("", r.atomicServiceCode)
        assertEquals("", r.dictId)
        assertEquals("", r.dictType)
        assertEquals("", r.dictName)
        assertEquals("", r.itemCode)
        assertNull(r.parentId)
        assertEquals("", r.itemName)
        assertNull(r.orderNum)
        assertTrue(r.active)
        assertNull(r.remark)
        assertNull(r.parentCode)
        assertNull(r.parentIds)
    }

    @Test
    fun fullArgsConstruction() {
        val r = SysDictItemRow(
            id = "i-1",
            atomicServiceCode = "sys",
            dictId = "d-1",
            dictType = "gender",
            dictName = "Gender",
            itemCode = "male",
            parentId = "p-1",
            itemName = "Male",
            orderNum = 1,
            active = false,
            remark = "r",
            parentCode = "root",
            parentIds = listOf("p-0", "p-1"),
        )
        assertEquals("i-1", r.id)
        assertEquals("sys", r.atomicServiceCode)
        assertEquals("d-1", r.dictId)
        assertEquals("gender", r.dictType)
        assertEquals("Gender", r.dictName)
        assertEquals("male", r.itemCode)
        assertEquals("p-1", r.parentId)
        assertEquals("Male", r.itemName)
        assertEquals(1, r.orderNum)
        assertEquals(false, r.active)
        assertEquals("r", r.remark)
        assertEquals("root", r.parentCode)
        assertEquals(listOf("p-0", "p-1"), r.parentIds)
    }

    @Test
    fun mutablePropertiesCanBeReassigned() {
        val r = SysDictItemRow()
        r.parentCode = "new-parent"
        r.parentIds = listOf("a", "b")
        assertEquals("new-parent", r.parentCode)
        assertEquals(listOf("a", "b"), r.parentIds)
    }

    @Test
    fun dataClassContract() {
        val a = SysDictItemRow(id = "i-1", itemCode = "male")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(itemCode = "female"))
        assertTrue(a.toString().contains("male"))
    }

}
