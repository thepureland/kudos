package io.kudos.ms.sys.common.dict.vo.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDictRow
 *
 * Covers default values, full-args construction and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictRowTest {

    @Test
    fun defaults() {
        val r = SysDictRow()
        assertEquals("", r.id)
        assertEquals("", r.dictType)
        assertEquals("", r.dictName)
        assertEquals("", r.atomicServiceCode)
        assertNull(r.parentId)
        assertNull(r.parentCode)
        assertNull(r.parentIds)
        assertNull(r.remark)
        assertTrue(r.active)
        assertTrue(r.builtIn)
        assertNull(r.itemId)
        assertNull(r.itemCode)
        assertNull(r.itemName)
        assertNull(r.seqNo)
    }

    @Test
    fun fullArgsConstruction() {
        val r = SysDictRow(
            id = "d-1",
            dictType = "gender",
            dictName = "Gender",
            atomicServiceCode = "sys",
            parentId = "p-1",
            parentCode = "root",
            parentIds = listOf("p-0", "p-1"),
            remark = "r",
            active = false,
            builtIn = false,
            itemId = "i-1",
            itemCode = "male",
            itemName = "Male",
            seqNo = 3,
        )
        assertEquals("d-1", r.id)
        assertEquals("gender", r.dictType)
        assertEquals("Gender", r.dictName)
        assertEquals("sys", r.atomicServiceCode)
        assertEquals("p-1", r.parentId)
        assertEquals("root", r.parentCode)
        assertEquals(listOf("p-0", "p-1"), r.parentIds)
        assertEquals("r", r.remark)
        assertEquals(false, r.active)
        assertEquals(false, r.builtIn)
        assertEquals("i-1", r.itemId)
        assertEquals("male", r.itemCode)
        assertEquals("Male", r.itemName)
        assertEquals(3, r.seqNo)
    }

    @Test
    fun dataClassContract() {
        val a = SysDictRow(id = "d-1", dictType = "gender")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(seqNo = 1))
        assertTrue(a.toString().contains("gender"))
    }

}
