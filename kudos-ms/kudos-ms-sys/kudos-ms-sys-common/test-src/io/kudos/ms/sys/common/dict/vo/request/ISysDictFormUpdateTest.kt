package io.kudos.ms.sys.common.dict.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for ISysDictFormUpdate
 *
 * Covers property accessors (nullable id + ISysDictFormBase overrides) and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class ISysDictFormUpdateTest {

    @Test
    fun properties() {
        val form = ISysDictFormUpdate(
            id = "d-1",
            dictType = "gender",
            dictName = "Gender",
            atomicServiceCode = "sys",
            remark = "r",
        )
        assertEquals("d-1", form.id)
        assertEquals("gender", form.dictType)
        assertEquals("Gender", form.dictName)
        assertEquals("sys", form.atomicServiceCode)
        assertEquals("r", form.remark)
    }

    @Test
    fun nullableIdIsAllowed() {
        val form = ISysDictFormUpdate(null, "gender", "Gender", "sys", null)
        assertNull(form.id)
        assertNull(form.remark)
    }

    @Test
    fun dataClassContract() {
        val a = ISysDictFormUpdate("d-1", "gender", "Gender", "sys", null)
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "d-2"))
        assertTrue(a.toString().contains("d-1"))
    }

}
