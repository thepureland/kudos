package io.kudos.ms.sys.common.datasource.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDataSourceFormCreate
 *
 * Covers property accessors (ISysDataSourceFormBase overrides), nullable fields and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDataSourceFormCreateTest {

    private fun form() = SysDataSourceFormCreate(
        name = "ds1",
        subSystemCode = "sys",
        microServiceCode = "ms",
        tenantId = "t-1",
        url = "jdbc:mysql://localhost:3306/db",
        username = "root",
        password = "pwd",
        initialSize = 1,
        maxActive = 10,
        maxIdle = 8,
        minIdle = 2,
        maxWait = 1000,
        maxAge = 60000,
        remark = "r",
    )

    @Test
    fun properties() {
        val f = form()
        assertTrue(f is ISysDataSourceFormBase)
        assertEquals("ds1", f.name)
        assertEquals("sys", f.subSystemCode)
        assertEquals("ms", f.microServiceCode)
        assertEquals("t-1", f.tenantId)
        assertEquals("jdbc:mysql://localhost:3306/db", f.url)
        assertEquals("root", f.username)
        assertEquals("pwd", f.password)
        assertEquals(1, f.initialSize)
        assertEquals(10, f.maxActive)
        assertEquals(8, f.maxIdle)
        assertEquals(2, f.minIdle)
        assertEquals(1000, f.maxWait)
        assertEquals(60000, f.maxAge)
        assertEquals("r", f.remark)
    }

    @Test
    fun nullableFields() {
        val f = form().copy(
            tenantId = null, password = null, initialSize = null, maxActive = null,
            maxIdle = null, minIdle = null, maxWait = null, maxAge = null, remark = null,
        )
        assertNull(f.tenantId)
        assertNull(f.password)
        assertNull(f.initialSize)
        assertNull(f.maxActive)
        assertNull(f.maxIdle)
        assertNull(f.minIdle)
        assertNull(f.maxWait)
        assertNull(f.maxAge)
        assertNull(f.remark)
    }

    @Test
    fun dataClassContract() {
        val a = form()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "ds2"))
        assertTrue(a.toString().contains("ds1"))
    }

}
