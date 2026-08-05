package io.kudos.ms.sys.common.tenant.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysTenantEdit
 *
 * Covers default values, full-args construction, the mutable subSystemCodes extra field,
 * IIdEntity contract and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantEditTest {

    @Test
    fun defaults() {
        val e = SysTenantEdit()
        assertEquals("", e.id)
        assertEquals("", e.name)
        assertNull(e.timezone)
        assertNull(e.defaultLanguageCode)
        assertNull(e.remark)
        assertTrue(e.active)
        assertEquals(false, e.builtIn)
        assertNull(e.createUserId)
        assertNull(e.createUserName)
        assertNull(e.createTime)
        assertNull(e.updateUserId)
        assertNull(e.updateUserName)
        assertNull(e.updateTime)
        assertEquals("", e.subSystemCodes)
        assertTrue(e is IIdEntity<*>)
    }

    @Test
    fun fullArgsAndSubSystemCodes() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val e = SysTenantEdit(
            id = "t-1", name = "Tenant One", timezone = "Asia/Shanghai",
            defaultLanguageCode = "zh-CN", remark = "r", active = false, builtIn = true,
            createUserId = "u-1", createUserName = "creator", createTime = now,
            updateUserId = "u-2", updateUserName = "updater", updateTime = now,
        )
        e.subSystemCodes = "sys,crm"
        assertEquals("t-1", e.id)
        assertEquals(false, e.active)
        assertEquals(true, e.builtIn)
        assertEquals(now, e.updateTime)
        assertEquals("sys,crm", e.subSystemCodes)
    }

    @Test
    fun dataClassContract() {
        val a = SysTenantEdit(id = "t-1", name = "Tenant One")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "Other"))
        assertTrue(a.toString().contains("t-1"))
    }

    @Test
    fun subSystemCodesNotPartOfDataClassEquality() {
        val a = SysTenantEdit(id = "t-1")
        val b = a.copy()
        a.subSystemCodes = "sys"
        assertEquals(a, b)
    }

}
