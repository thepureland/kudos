package io.kudos.ms.sys.common.domain.vo.response

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDomainRow
 *
 * Covers default values, full-args construction, the mutable tenantName extra field
 * and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDomainRowTest {

    @Test
    fun defaults() {
        val r = SysDomainRow()
        assertEquals("", r.id)
        assertEquals("", r.domain)
        assertEquals("", r.systemCode)
        assertEquals("", r.tenantId)
        assertNull(r.remark)
        assertTrue(r.active)
        assertEquals(false, r.builtIn)
        assertNull(r.createTime)
        assertEquals("", r.tenantName)
    }

    @Test
    fun fullArgsAndTenantName() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val r = SysDomainRow(
            id = "d-1", domain = "example.com", systemCode = "sys", tenantId = "t-1",
            remark = "r", active = false, builtIn = true, createTime = now,
        )
        r.tenantName = "Tenant One"
        assertEquals("d-1", r.id)
        assertEquals(false, r.active)
        assertEquals(now, r.createTime)
        assertEquals("Tenant One", r.tenantName)
    }

    @Test
    fun dataClassContract() {
        val a = SysDomainRow(id = "d-1", domain = "example.com")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(domain = "other.com"))
        assertTrue(a.toString().contains("d-1"))
    }

}
