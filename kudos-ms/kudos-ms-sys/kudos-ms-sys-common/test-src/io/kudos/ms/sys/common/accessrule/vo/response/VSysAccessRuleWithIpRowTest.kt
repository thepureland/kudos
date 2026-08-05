package io.kudos.ms.sys.common.accessrule.vo.response

import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for VSysAccessRuleWithIpRow
 *
 * Covers default values (row without IP child), full-args construction,
 * IIpBigDecimalToStringSupport overrides and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class VSysAccessRuleWithIpRowTest {

    @Test
    fun defaultsRepresentRuleWithoutIpChild() {
        val r = VSysAccessRuleWithIpRow()
        assertEquals("", r.id)
        assertEquals("", r.parentId)
        assertNull(r.tenantId)
        assertNull(r.tenantName)
        assertNull(r.systemCode)
        assertNull(r.accessRuleTypeDictCode)
        assertNull(r.parentRemark)
        assertNull(r.parentActive)
        assertNull(r.parentBuiltIn)
        assertNull(r.parentCreateUserId)
        assertNull(r.parentCreateUserName)
        assertNull(r.parentCreateTime)
        assertNull(r.parentUpdateUserId)
        assertNull(r.parentUpdateUserName)
        assertNull(r.parentUpdateTime)
        assertNull(r.ipId)
        assertNull(r.ipStart)
        assertNull(r.ipEnd)
        assertNull(r.ipTypeDictCode)
        assertNull(r.expirationTime)
        assertNull(r.parentRuleId)
        assertNull(r.remark)
        assertNull(r.active)
        assertNull(r.builtIn)
        assertNull(r.createUserId)
        assertNull(r.createUserName)
        assertNull(r.createTime)
        assertNull(r.updateUserId)
        assertNull(r.updateUserName)
        assertNull(r.updateTime)
        assertNull(r.getIpStartStr())
        assertNull(r.getIpEndStr())
    }

    @Test
    fun fullArgsConstructionAndIpRendering() {
        val now = LocalDateTime.of(2032, 2, 3, 4, 5)
        val r = VSysAccessRuleWithIpRow(
            id = "v-1",
            parentId = "p-1",
            tenantId = "t-1",
            tenantName = "Tenant One",
            systemCode = "sys-a",
            accessRuleTypeDictCode = "2",
            parentRemark = "pr",
            parentActive = true,
            parentBuiltIn = false,
            parentCreateUserId = "u-0",
            parentCreateUserName = "creator",
            parentCreateTime = now,
            parentUpdateUserId = "u-9",
            parentUpdateUserName = "updater",
            parentUpdateTime = now,
            ipId = "ip-1",
            ipStart = BigDecimal("3232235521"),
            ipEnd = BigDecimal("3232235530"),
            ipTypeDictCode = "ipv4",
            expirationTime = now,
            parentRuleId = "p-1",
            remark = "r",
            active = true,
            builtIn = false,
            createUserId = "u-1",
            createUserName = "alice",
            createTime = now,
            updateUserId = "u-2",
            updateUserName = "bob",
            updateTime = now,
        )
        assertEquals("v-1", r.id)
        assertEquals("p-1", r.parentId)
        assertEquals("t-1", r.tenantId)
        assertEquals("Tenant One", r.tenantName)
        assertEquals("sys-a", r.systemCode)
        assertEquals("2", r.accessRuleTypeDictCode)
        assertEquals("pr", r.parentRemark)
        assertEquals(true, r.parentActive)
        assertEquals(false, r.parentBuiltIn)
        assertEquals("u-0", r.parentCreateUserId)
        assertEquals("creator", r.parentCreateUserName)
        assertEquals(now, r.parentCreateTime)
        assertEquals("u-9", r.parentUpdateUserId)
        assertEquals("updater", r.parentUpdateUserName)
        assertEquals(now, r.parentUpdateTime)
        assertEquals("ip-1", r.ipId)
        assertEquals(BigDecimal("3232235521"), r.getIpStartBigDecimal())
        assertEquals(BigDecimal("3232235530"), r.getIpEndBigDecimal())
        assertEquals("ipv4", r.getIpTypeDictCodeStr())
        assertEquals("192.168.0.1", r.getIpStartStr())
        assertEquals("192.168.0.10", r.getIpEndStr())
        assertEquals(now, r.expirationTime)
        assertEquals("p-1", r.parentRuleId)
        assertEquals("r", r.remark)
        assertEquals(true, r.active)
        assertEquals(false, r.builtIn)
        assertEquals("u-1", r.createUserId)
        assertEquals("alice", r.createUserName)
        assertEquals(now, r.createTime)
        assertEquals("u-2", r.updateUserId)
        assertEquals("bob", r.updateUserName)
        assertEquals(now, r.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = VSysAccessRuleWithIpRow(id = "v-1", parentId = "p-1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(parentId = "p-2"))
        assertTrue(a.toString().contains("v-1"))
    }

}
