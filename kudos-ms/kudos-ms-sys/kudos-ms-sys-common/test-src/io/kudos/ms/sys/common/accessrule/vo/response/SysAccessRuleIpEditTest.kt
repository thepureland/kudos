package io.kudos.ms.sys.common.accessrule.vo.response

import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysAccessRuleIpEdit
 *
 * Covers default values, full-args construction, IIpBigDecimalToStringSupport overrides
 * (including IPv6 full-string rendering) and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAccessRuleIpEditTest {

    @Test
    fun defaults() {
        val e = SysAccessRuleIpEdit()
        assertEquals("", e.id)
        assertNull(e.ipStart)
        assertNull(e.ipEnd)
        assertNull(e.ipTypeDictCode)
        assertNull(e.expirationDate)
        assertNull(e.parentRuleId)
        assertNull(e.remark)
        assertNull(e.active)
        assertNull(e.createUserId)
        assertNull(e.createUserName)
        assertNull(e.createTime)
        assertNull(e.updateUserId)
        assertNull(e.updateUserName)
        assertNull(e.updateTime)
        assertNull(e.getIpStartStr())
        assertNull(e.getIpEndStr())
    }

    @Test
    fun fullArgsConstructionAndIpv6Rendering() {
        val now = LocalDateTime.of(2031, 1, 2, 3, 4)
        val e = SysAccessRuleIpEdit(
            id = "ip-1",
            ipStart = BigDecimal.ONE,
            ipEnd = BigDecimal(255),
            ipTypeDictCode = "ipv6",
            expirationDate = now,
            parentRuleId = "rule-1",
            remark = "r",
            active = false,
            createUserId = "u-1",
            createUserName = "alice",
            createTime = now,
            updateUserId = "u-2",
            updateUserName = "bob",
            updateTime = now,
        )
        assertEquals("ip-1", e.id)
        assertEquals(BigDecimal.ONE, e.getIpStartBigDecimal())
        assertEquals(BigDecimal(255), e.getIpEndBigDecimal())
        assertEquals("ipv6", e.getIpTypeDictCodeStr())
        assertEquals("0000:0000:0000:0000:0000:0000:0000:0001", e.getIpStartStr())
        assertEquals("0000:0000:0000:0000:0000:0000:0000:00FF", e.getIpEndStr())
        assertEquals(now, e.expirationDate)
        assertEquals("rule-1", e.parentRuleId)
        assertEquals("r", e.remark)
        assertEquals(false, e.active)
        assertEquals("u-1", e.createUserId)
        assertEquals("alice", e.createUserName)
        assertEquals(now, e.createTime)
        assertEquals("u-2", e.updateUserId)
        assertEquals("bob", e.updateUserName)
        assertEquals(now, e.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysAccessRuleIpEdit(id = "ip-1", parentRuleId = "rule-1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(parentRuleId = "rule-2"))
        assertTrue(a.toString().contains("ip-1"))
    }

}
