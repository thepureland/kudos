package io.kudos.ms.sys.common.datasource.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDataSourceDetail
 *
 * Covers default values, full-args construction, mutable tenantName, IIdEntity contract
 * and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDataSourceDetailTest {

    @Test
    fun defaults() {
        val d = SysDataSourceDetail()
        assertEquals("", d.id)
        assertEquals("", d.name)
        assertEquals("", d.subSystemCode)
        assertEquals("", d.microServiceCode)
        assertNull(d.tenantId)
        assertNull(d.tenantName)
        assertEquals("", d.url)
        assertEquals("", d.username)
        assertNull(d.password)
        assertNull(d.initialSize)
        assertNull(d.maxActive)
        assertNull(d.maxIdle)
        assertNull(d.minIdle)
        assertNull(d.maxWait)
        assertNull(d.maxAge)
        assertNull(d.remark)
        assertNull(d.active)
        assertNull(d.builtIn)
        assertNull(d.createTime)
        assertNull(d.updateTime)
    }

    @Test
    fun fullArgsAndMutableTenantName() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val d = SysDataSourceDetail(
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
            createUserId = "u1",
            createUserName = "creator",
            createTime = now,
            updateUserId = "u2",
            updateUserName = "updater",
            updateTime = now,
        )
        assertTrue(d is IIdEntity<*>)
        assertEquals("Tenant", d.tenantName)
        d.tenantName = "Renamed"
        assertEquals("Renamed", d.tenantName)
        assertEquals(true, d.active)
        assertEquals(false, d.builtIn)
        assertEquals(now, d.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysDataSourceDetail(id = "ds-1", name = "ds1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "ds2"))
        assertTrue(a.toString().contains("ds-1"))
    }

}
