package io.kudos.ms.sys.common.datasource.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDataSourceEdit
 *
 * Covers default values, full-args construction, mutable tenantName, IIdEntity contract
 * and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDataSourceEditTest {

    @Test
    fun defaults() {
        val e = SysDataSourceEdit()
        assertEquals("", e.id)
        assertEquals("", e.name)
        assertNull(e.tenantId)
        assertNull(e.tenantName)
        assertEquals("", e.url)
        assertEquals("", e.username)
        assertNull(e.password)
        assertNull(e.active)
        assertNull(e.builtIn)
        assertNull(e.createTime)
        assertNull(e.updateTime)
    }

    @Test
    fun fullArgsAndMutableTenantName() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val e = SysDataSourceEdit(
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
        assertTrue(e is IIdEntity<*>)
        e.tenantName = "Renamed"
        assertEquals("Renamed", e.tenantName)
        assertEquals(now, e.createTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysDataSourceEdit(id = "ds-1", name = "ds1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "ds2"))
        assertTrue(a.toString().contains("ds-1"))
    }

}
