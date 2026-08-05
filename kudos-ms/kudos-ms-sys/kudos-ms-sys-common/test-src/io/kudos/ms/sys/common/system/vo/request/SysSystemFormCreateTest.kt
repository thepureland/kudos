package io.kudos.ms.sys.common.system.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysSystemFormCreate
 *
 * Covers ISysSystemFormBase property accessors (incl. nullable parentCode / remark)
 * and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysSystemFormCreateTest {

    private fun form() = SysSystemFormCreate(
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
        assertEquals("sys", f.code)
        assertEquals("System", f.name)
        assertEquals(false, f.subSystem)
        assertEquals("root", f.parentCode)
        assertEquals("r", f.remark)
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
