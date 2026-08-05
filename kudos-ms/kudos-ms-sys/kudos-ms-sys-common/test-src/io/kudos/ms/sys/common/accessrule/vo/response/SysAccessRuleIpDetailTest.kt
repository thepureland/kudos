package io.kudos.ms.sys.common.accessrule.vo.response

import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysAccessRuleIpDetail
 *
 * Covers default values, full-args construction, IIpBigDecimalToStringSupport overrides
 * (including null-value and IPv4 rendering) and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAccessRuleIpDetailTest {

    @Test
    fun defaults() {
        val d = SysAccessRuleIpDetail()
        assertEquals("", d.id)
        assertNull(d.ipStart)
        assertNull(d.ipEnd)
        assertNull(d.ipTypeDictCode)
        assertNull(d.expirationDate)
        assertNull(d.parentRuleId)
        assertNull(d.remark)
        assertNull(d.active)
        assertNull(d.createUserId)
        assertNull(d.createUserName)
        assertNull(d.createTime)
        assertNull(d.updateUserId)
        assertNull(d.updateUserName)
        assertNull(d.updateTime)
        // overrides on an all-null instance
        assertNull(d.getIpStartBigDecimal())
        assertNull(d.getIpEndBigDecimal())
        assertNull(d.getIpTypeDictCodeStr())
        assertNull(d.getIpStartStr())
        assertNull(d.getIpEndStr())
    }

    @Test
    fun fullArgsConstructionAndIpRendering() {
        val now = LocalDateTime.of(2030, 7, 8, 9, 10)
        val d = SysAccessRuleIpDetail(
            id = "ip-1",
            ipStart = BigDecimal("3232235521"),
            ipEnd = BigDecimal("3232235530"),
            ipTypeDictCode = "ipv4",
            expirationDate = now,
            parentRuleId = "rule-1",
            remark = "r",
            active = true,
            createUserId = "u-1",
            createUserName = "alice",
            createTime = now,
            updateUserId = "u-2",
            updateUserName = "bob",
            updateTime = now,
        )
        assertEquals("ip-1", d.id)
        assertEquals(BigDecimal("3232235521"), d.getIpStartBigDecimal())
        assertEquals(BigDecimal("3232235530"), d.getIpEndBigDecimal())
        assertEquals("ipv4", d.getIpTypeDictCodeStr())
        assertEquals("192.168.0.1", d.getIpStartStr())
        assertEquals("192.168.0.10", d.getIpEndStr())
        assertEquals(now, d.expirationDate)
        assertEquals("rule-1", d.parentRuleId)
        assertEquals("r", d.remark)
        assertEquals(true, d.active)
        assertEquals("u-1", d.createUserId)
        assertEquals("alice", d.createUserName)
        assertEquals(now, d.createTime)
        assertEquals("u-2", d.updateUserId)
        assertEquals("bob", d.updateUserName)
        assertEquals(now, d.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysAccessRuleIpDetail(id = "ip-1", ipTypeDictCode = "ipv4")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = false))
        assertTrue(a.toString().contains("ip-1"))
    }

}
