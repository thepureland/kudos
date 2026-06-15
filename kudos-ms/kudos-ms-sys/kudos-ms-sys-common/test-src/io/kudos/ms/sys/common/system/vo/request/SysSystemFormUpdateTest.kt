package io.kudos.ms.sys.common.system.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysSystemFormUpdate
 *
 * Covers ISysSystemFormBase property accessors, the id getter delegating to code
 * (IIdEntity), the default subSystem=true, nullable fields and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysSystemFormUpdateTest {

    private fun form() = SysSystemFormUpdate(
        code = "sys",
        name = "System",
        subSystem = false,
        parentCode = "root",
        remark = "r",
    )

    @Test
    fun properties() {
        val f = form()
        assertTrue(f is ISysSystemFormBase)
        assertTrue(f is IIdEntity<*>)
        assertEquals("sys", f.code)
        assertEquals("System", f.name)
        assertEquals(false, f.subSystem)
        assertEquals("root", f.parentCode)
        assertEquals("r", f.remark)
    }

    @Test
    fun idDelegatesToCode() {
        val f = form()
        assertEquals("sys", f.id)
        assertEquals(f.code, f.id)
        assertEquals("other", f.copy(code = "other").id)
    }

    @Test
    fun defaultSubSystemIsTrue() {
        val f = SysSystemFormUpdate(code = "c", name = "n", parentCode = null, remark = null)
        assertTrue(f.subSystem)
    }

    @Test
    fun nullableFields() {
        val f = form().copy(parentCode = null, remark = null)
        assertNull(f.parentCode)
        assertNull(f.remark)
    }

    @Test
    fun dataClassContract() {
        val a = form()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(code = "other"))
        assertTrue(a.toString().contains("sys"))
    }

}
