package io.kudos.ms.sys.common.domain.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDomainEdit
 *
 * Covers default values, full-args construction, the mutable tenantName extra field,
 * IIdEntity contract and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDomainEditTest {

    @Test
    fun defaults() {
        val e = SysDomainEdit()
        assertEquals("", e.id)
        assertEquals("", e.domain)
        assertEquals("", e.systemCode)
        assertEquals("", e.tenantId)
        assertNull(e.remark)
        assertTrue(e.active)
        assertEquals(false, e.builtIn)
        assertEquals("", e.tenantName)
        assertTrue(e is IIdEntity<*>)
    }

    @Test
    fun fullArgsAndTenantName() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val e = SysDomainEdit(
            id = "d-1", domain = "example.com", systemCode = "sys", tenantId = "t-1",
            remark = "r", active = false, builtIn = true, createTime = now, updateTime = now,
        )
        e.tenantName = "Tenant One"
        assertEquals("d-1", e.id)
        assertEquals(false, e.active)
        assertEquals(now, e.updateTime)
        assertEquals("Tenant One", e.tenantName)
    }

    @Test
    fun dataClassContract() {
        val a = SysDomainEdit(id = "d-1", domain = "example.com")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(domain = "other.com"))
        assertTrue(a.toString().contains("d-1"))
    }

}
