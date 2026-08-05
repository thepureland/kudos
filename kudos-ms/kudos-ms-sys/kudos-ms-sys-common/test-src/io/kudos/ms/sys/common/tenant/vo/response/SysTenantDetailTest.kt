package io.kudos.ms.sys.common.tenant.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysTenantDetail
 *
 * Covers default values, full-args construction, the mutable subSystemCodes extra field,
 * IIdEntity contract and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantDetailTest {

    @Test
    fun defaults() {
        val d = SysTenantDetail()
        assertEquals("", d.id)
        assertEquals("", d.name)
        assertNull(d.timezone)
        assertNull(d.defaultLanguageCode)
        assertNull(d.remark)
        assertTrue(d.active)
        assertEquals(false, d.builtIn)
        assertNull(d.createUserId)
        assertNull(d.createUserName)
        assertNull(d.createTime)
        assertNull(d.updateUserId)
        assertNull(d.updateUserName)
        assertNull(d.updateTime)
        assertEquals("", d.subSystemCodes)
        assertTrue(d is IIdEntity<*>)
    }

    @Test
    fun fullArgsAndSubSystemCodes() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val d = SysTenantDetail(
            id = "t-1", name = "Tenant One", timezone = "Asia/Shanghai",
            defaultLanguageCode = "zh-CN", remark = "r", active = false, builtIn = true,
            createUserId = "u-1", createUserName = "creator", createTime = now,
            updateUserId = "u-2", updateUserName = "updater", updateTime = now,
        )
        d.subSystemCodes = "sys,crm"
        assertEquals("t-1", d.id)
        assertEquals(false, d.active)
        assertEquals(true, d.builtIn)
        assertEquals(now, d.createTime)
        assertEquals("updater", d.updateUserName)
        assertEquals("sys,crm", d.subSystemCodes)
    }

    @Test
    fun dataClassContract() {
        val a = SysTenantDetail(id = "t-1", name = "Tenant One")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "Other"))
        assertTrue(a.toString().contains("t-1"))
    }

    @Test
    fun subSystemCodesNotPartOfDataClassEquality() {
        val a = SysTenantDetail(id = "t-1")
        val b = a.copy()
        a.subSystemCodes = "sys"
        // subSystemCodes is in the class body, excluded from equals/hashCode.
        assertEquals(a, b)
    }

}
