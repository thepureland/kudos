package io.kudos.ms.sys.common.tenant.vo.response

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysTenantRow
 *
 * Covers default values, full-args construction, the mutable subSystemCodes extra field
 * and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantRowTest {

    @Test
    fun defaults() {
        val r = SysTenantRow()
        assertEquals("", r.id)
        assertEquals("", r.name)
        assertNull(r.timezone)
        assertNull(r.defaultLanguageCode)
        assertNull(r.createTime)
        assertNull(r.remark)
        assertTrue(r.active)
        assertEquals(false, r.builtIn)
        assertEquals("", r.subSystemCodes)
    }

    @Test
    fun fullArgsAndSubSystemCodes() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val r = SysTenantRow(
            id = "t-1", name = "Tenant One", timezone = "Asia/Shanghai",
            defaultLanguageCode = "zh-CN", createTime = now, remark = "r",
            active = false, builtIn = true,
        )
        r.subSystemCodes = "sys,crm"
        assertEquals("t-1", r.id)
        assertEquals("Tenant One", r.name)
        assertEquals(now, r.createTime)
        assertEquals(false, r.active)
        assertEquals(true, r.builtIn)
        assertEquals("sys,crm", r.subSystemCodes)
    }

    @Test
    fun dataClassContract() {
        val a = SysTenantRow(id = "t-1", name = "Tenant One")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "Other"))
        assertTrue(a.toString().contains("t-1"))
    }

    @Test
    fun subSystemCodesNotPartOfDataClassEquality() {
        val a = SysTenantRow(id = "t-1")
        val b = a.copy()
        a.subSystemCodes = "sys"
        assertEquals(a, b)
    }

}
