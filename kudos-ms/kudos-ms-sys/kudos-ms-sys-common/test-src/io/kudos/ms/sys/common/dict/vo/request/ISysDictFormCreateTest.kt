package io.kudos.ms.sys.common.dict.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for ISysDictFormCreate
 *
 * Covers property accessors (ISysDictFormBase overrides) and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class ISysDictFormCreateTest {

    @Test
    fun properties() {
        val form = ISysDictFormCreate(
            dictType = "gender",
            dictName = "Gender",
            atomicServiceCode = "sys",
            remark = null,
        )
        assertEquals("gender", form.dictType)
        assertEquals("Gender", form.dictName)
        assertEquals("sys", form.atomicServiceCode)
        assertNull(form.remark)
    }

    @Test
    fun dataClassContract() {
        val a = ISysDictFormCreate("gender", "Gender", "sys", "r")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(dictType = "color"))
        assertTrue(a.toString().contains("gender"))
    }

}
