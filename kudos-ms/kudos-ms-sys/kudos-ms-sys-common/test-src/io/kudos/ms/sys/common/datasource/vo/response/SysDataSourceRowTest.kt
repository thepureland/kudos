package io.kudos.ms.sys.common.datasource.vo.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDataSourceRow
 *
 * Covers default values, full-args construction, mutable tenantName and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDataSourceRowTest {

    @Test
    fun defaults() {
        val r = SysDataSourceRow()
        assertEquals("", r.id)
        assertEquals("", r.name)
        assertEquals("", r.subSystemCode)
        assertEquals("", r.microServiceCode)
        assertNull(r.tenantId)
        assertNull(r.tenantName)
        assertEquals("", r.url)
        assertEquals("", r.username)
        assertNull(r.password)
        assertNull(r.initialSize)
        assertNull(r.maxActive)
        assertNull(r.maxIdle)
        assertNull(r.minIdle)
        assertNull(r.maxWait)
        assertNull(r.maxAge)
        assertNull(r.remark)
        assertNull(r.active)
        assertNull(r.builtIn)
    }

    @Test
    fun fullArgsAndMutableTenantName() {
        val r = SysDataSourceRow(
            id = "ds-1",
            name = "ds1",
            subSystemCode = "sys",
            microServiceCode = "ms",
            tenantId = "t-1",
            tenantName = "Tenant",
            url = "jdbc:h2:mem:test",
            username = "sa",
            password = "******",
            initialSize = 1,
            maxActive = 10,
            maxIdle = 8,
            minIdle = 2,
            maxWait = 1000,
            maxAge = 60000,
            remark = "r",
            active = true,
            builtIn = false,
        )
        assertEquals("ds-1", r.id)
        r.tenantName = "Renamed"
        assertEquals("Renamed", r.tenantName)
        assertEquals(60000, r.maxAge)
    }

    @Test
    fun dataClassContract() {
        val a = SysDataSourceRow(id = "ds-1", name = "ds1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "ds2"))
        assertTrue(a.toString().contains("ds-1"))
    }

}
