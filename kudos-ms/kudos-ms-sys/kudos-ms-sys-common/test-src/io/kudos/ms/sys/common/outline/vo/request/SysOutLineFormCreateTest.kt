package io.kudos.ms.sys.common.outline.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysOutLineFormCreate
 *
 * Covers ISysOutLineFormBase property accessors (including nullable port / tenantId / remark)
 * and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysOutLineFormCreateTest {

    private fun form() = SysOutLineFormCreate(
        name = "wl",
        host = "example.com",
        port = 443,
        protocol = "https",
        systemCode = "sys",
        tenantId = "t-1",
        remark = "r",
    )

    @Test
    fun properties() {
        val f = form()
        assertTrue(f is ISysOutLineFormBase)
        assertEquals("wl", f.name)
        assertEquals("example.com", f.host)
        assertEquals(443, f.port)
        assertEquals("https", f.protocol)
        assertEquals("sys", f.systemCode)
        assertEquals("t-1", f.tenantId)
        assertEquals("r", f.remark)
    }

    @Test
    fun nullableFields() {
        val f = form().copy(port = null, tenantId = null, remark = null)
        assertNull(f.port)
        assertNull(f.tenantId)
        assertNull(f.remark)
    }

    @Test
    fun dataClassContract() {
        val a = form()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(host = "other.com"))
        assertTrue(a.toString().contains("example.com"))
    }

}
