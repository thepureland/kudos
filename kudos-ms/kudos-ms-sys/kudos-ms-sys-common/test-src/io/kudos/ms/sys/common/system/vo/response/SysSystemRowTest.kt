package io.kudos.ms.sys.common.system.vo.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysSystemRow
 *
 * Covers default values, full-args construction and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysSystemRowTest {

    @Test
    fun defaults() {
        val r = SysSystemRow()
        assertEquals("", r.id)
        assertEquals("", r.code)
        assertEquals("", r.name)
        assertTrue(r.subSystem)
        assertNull(r.parentCode)
        assertNull(r.remark)
        assertTrue(r.active)
        assertEquals(false, r.builtIn)
    }

    @Test
    fun fullArgs() {
        val r = SysSystemRow(
            id = "sys", code = "sys", name = "System", subSystem = false,
            parentCode = "root", remark = "r", active = false, builtIn = true,
        )
        assertEquals("sys", r.id)
        assertEquals("System", r.name)
        assertEquals(false, r.subSystem)
        assertEquals("root", r.parentCode)
        assertEquals(false, r.active)
        assertEquals(true, r.builtIn)
    }

    @Test
    fun dataClassContract() {
        val a = SysSystemRow(id = "sys", code = "sys")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(code = "other"))
        assertTrue(a.toString().contains("sys"))
    }

}
