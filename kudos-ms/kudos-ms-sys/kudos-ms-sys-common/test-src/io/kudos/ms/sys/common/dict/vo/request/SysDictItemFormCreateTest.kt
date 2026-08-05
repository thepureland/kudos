package io.kudos.ms.sys.common.dict.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDictItemFormCreate
 *
 * Covers property accessors (ISysDictItemFormBase overrides) and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictItemFormCreateTest {

    @Test
    fun properties() {
        val form = SysDictItemFormCreate(
            itemCode = "male",
            itemName = "Male",
            dictId = "d-1",
            orderNum = 2,
            parentId = "p-1",
            remark = "r",
        )
        assertEquals("male", form.itemCode)
        assertEquals("Male", form.itemName)
        assertEquals("d-1", form.dictId)
        assertEquals(2, form.orderNum)
        assertEquals("p-1", form.parentId)
        assertEquals("r", form.remark)
    }

    @Test
    fun nullableFields() {
        val form = SysDictItemFormCreate("male", "Male", "d-1", null, null, null)
        assertNull(form.orderNum)
        assertNull(form.parentId)
        assertNull(form.remark)
    }

    @Test
    fun dataClassContract() {
        val a = SysDictItemFormCreate("male", "Male", "d-1", 1, null, null)
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(itemCode = "female"))
        assertTrue(a.toString().contains("male"))
    }

}
