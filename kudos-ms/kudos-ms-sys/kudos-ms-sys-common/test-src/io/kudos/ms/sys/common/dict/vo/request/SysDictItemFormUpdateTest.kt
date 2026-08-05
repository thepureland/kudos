package io.kudos.ms.sys.common.dict.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDictItemFormUpdate
 *
 * Covers property accessors (IIdEntity id + ISysDictItemFormBase overrides) and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictItemFormUpdateTest {

    @Test
    fun properties() {
        val form = SysDictItemFormUpdate(
            id = "i-1",
            itemCode = "male",
            itemName = "Male",
            dictId = "d-1",
            orderNum = 3,
            parentId = "p-1",
            remark = "r",
        )
        assertEquals("i-1", form.id)
        assertEquals("male", form.itemCode)
        assertEquals("Male", form.itemName)
        assertEquals("d-1", form.dictId)
        assertEquals(3, form.orderNum)
        assertEquals("p-1", form.parentId)
        assertEquals("r", form.remark)
    }

    @Test
    fun nullableFields() {
        val form = SysDictItemFormUpdate("i-1", "male", "Male", "d-1", null, null, null)
        assertNull(form.orderNum)
        assertNull(form.parentId)
        assertNull(form.remark)
    }

    @Test
    fun dataClassContract() {
        val a = SysDictItemFormUpdate("i-1", "male", "Male", "d-1", 1, null, null)
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(dictId = "d-2"))
        assertTrue(a.toString().contains("i-1"))
    }

}
