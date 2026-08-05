package io.kudos.ms.sys.common.domain.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDomainDetail
 *
 * Covers default values, full-args construction, the mutable tenantName extra field,
 * IIdEntity contract and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDomainDetailTest {

    @Test
    fun defaults() {
        val d = SysDomainDetail()
        assertEquals("", d.id)
        assertEquals("", d.domain)
        assertEquals("", d.systemCode)
        assertEquals("", d.tenantId)
        assertNull(d.remark)
        assertTrue(d.active)
        assertEquals(false, d.builtIn)
        assertNull(d.createTime)
        assertEquals("", d.tenantName)
        assertTrue(d is IIdEntity<*>)
    }

    @Test
    fun fullArgsAndTenantName() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val d = SysDomainDetail(
            id = "d-1", domain = "example.com", systemCode = "sys", tenantId = "t-1",
            remark = "r", active = false, builtIn = true, createTime = now, updateTime = now,
        )
        d.tenantName = "Tenant One"
        assertEquals("d-1", d.id)
        assertEquals(false, d.active)
        assertEquals(true, d.builtIn)
        assertEquals(now, d.createTime)
        assertEquals("Tenant One", d.tenantName)
    }

    @Test
    fun dataClassContract() {
        val a = SysDomainDetail(id = "d-1", domain = "example.com")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(domain = "other.com"))
        assertTrue(a.toString().contains("d-1"))
    }

    @Test
    fun tenantNameNotPartOfDataClassEquality() {
        val a = SysDomainDetail(id = "d-1")
        val b = a.copy()
        a.tenantName = "Tenant One"
        // tenantName is in the class body, excluded from equals/hashCode.
        assertEquals(a, b)
    }

}
