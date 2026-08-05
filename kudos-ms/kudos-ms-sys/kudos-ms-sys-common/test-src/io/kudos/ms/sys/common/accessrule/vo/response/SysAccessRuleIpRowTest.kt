package io.kudos.ms.sys.common.accessrule.vo.response

import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysAccessRuleIpRow
 *
 * Covers default/full-args construction, non-null IIpBigDecimalToStringSupport overrides
 * and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAccessRuleIpRowTest {

    private fun row() = SysAccessRuleIpRow(
        id = "ip-1",
        ipStart = BigDecimal("167772161"),
        ipEnd = BigDecimal("167772169"),
        ipTypeDictCode = "ipv4",
        expirationTime = LocalDateTime.of(2030, 9, 9, 9, 9),
        parentRuleId = "rule-1",
        remark = "r",
        active = true,
        parentRuleActive = false,
        tenantId = "t-1",
        systemCode = "sys-a",
        accessRuleTypeDictCode = "2",
    )

    @Test
    fun properties() {
        val r = row()
        assertEquals("ip-1", r.id)
        assertEquals(BigDecimal("167772161"), r.ipStart)
        assertEquals(BigDecimal("167772169"), r.ipEnd)
        assertEquals("ipv4", r.ipTypeDictCode)
        assertEquals(LocalDateTime.of(2030, 9, 9, 9, 9), r.expirationTime)
        assertEquals("rule-1", r.parentRuleId)
        assertEquals("r", r.remark)
        assertEquals(true, r.active)
        assertEquals(false, r.parentRuleActive)
        assertEquals("t-1", r.tenantId)
        assertEquals("sys-a", r.systemCode)
        assertEquals("2", r.accessRuleTypeDictCode)
    }

    @Test
    fun defaultsOfOptionalFields() {
        val r = SysAccessRuleIpRow(
            ipStart = BigDecimal.ONE,
            ipEnd = BigDecimal.TEN,
            ipTypeDictCode = "ipv6",
        )
        assertEquals("", r.id)
        assertNull(r.expirationTime)
        assertNull(r.parentRuleId)
        assertNull(r.remark)
        assertNull(r.active)
        assertNull(r.parentRuleActive)
        assertNull(r.tenantId)
        assertNull(r.systemCode)
        assertNull(r.accessRuleTypeDictCode)
    }

    @Test
    fun ipSupportOverrides() {
        val r = row()
        assertEquals(BigDecimal("167772161"), r.getIpStartBigDecimal())
        assertEquals(BigDecimal("167772169"), r.getIpEndBigDecimal())
        assertEquals("ipv4", r.getIpTypeDictCodeStr())
        assertEquals("10.0.0.1", r.getIpStartStr())
        assertEquals("10.0.0.9", r.getIpEndStr())
    }

    @Test
    fun dataClassContract() {
        val a = row()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "other"))
        assertTrue(a.toString().contains("ip-1"))
    }

}
