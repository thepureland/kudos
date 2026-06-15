package io.kudos.ms.sys.core.accessrule.model.po

import io.kudos.ms.sys.common.accessrule.vo.request.SysAccessRuleIpFormCreate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pure unit test for [SysAccessRuleIp.of]: assembling a persistable entity from a create form plus parent rule id.
 * Uses Ktorm's in-memory entity factory only; no Spring context or database required.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysAccessRuleIpOfTest {

    @Test
    fun of_ipv4_mapsAllFields() {
        val expiration = LocalDateTime.of(2030, 1, 2, 3, 4, 5)
        val form = SysAccessRuleIpFormCreate(
            ipv4StartStr = "192.168.0.1",
            ipv4EndStr = "192.168.0.10",
            ipv6StartStr = null,
            ipv6EndStr = null,
            ipTypeDictCode = "ipv4",
            expirationDate = expiration,
            systemCode = "sys-a",
            tenantId = "20000000-0000-0000-0000-000000000001",
            remark = "a remark",
        )

        val entity = SysAccessRuleIp.of(form, "parent-rule-1")

        assertEquals("parent-rule-1", entity.parentRuleId)
        assertEquals("ipv4", entity.ipTypeDictCode)
        assertEquals(expiration, entity.expirationTime)
        assertEquals("a remark", entity.remark)
        // 192.168.0.1 -> 3232235521, 192.168.0.10 -> 3232235530
        assertEquals(0, entity.ipStart.compareTo(java.math.BigDecimal("3232235521")))
        assertEquals(0, entity.ipEnd.compareTo(java.math.BigDecimal("3232235530")))
    }

    @Test
    fun of_ipv4_nullExpirationAndRemark() {
        val form = SysAccessRuleIpFormCreate(
            ipv4StartStr = "10.0.0.0",
            ipv4EndStr = "10.0.0.255",
            ipv6StartStr = null,
            ipv6EndStr = null,
            ipTypeDictCode = "ipv4",
            expirationDate = null,
            systemCode = "sys-b",
            tenantId = "20000000-0000-0000-0000-000000000002",
            remark = null,
        )

        val entity = SysAccessRuleIp.of(form, "parent-rule-2")

        assertEquals("parent-rule-2", entity.parentRuleId)
        assertNull(entity.expirationTime)
        assertNull(entity.remark)
    }

    @Test
    fun of_ipv6_mapsRange() {
        val form = SysAccessRuleIpFormCreate(
            ipv4StartStr = null,
            ipv4EndStr = null,
            ipv6StartStr = "::1",
            ipv6EndStr = "::1",
            ipTypeDictCode = "ipv6",
            expirationDate = null,
            systemCode = "sys-c",
            tenantId = "20000000-0000-0000-0000-000000000003",
            remark = null,
        )

        val entity = SysAccessRuleIp.of(form, "parent-rule-3")

        assertEquals("ipv6", entity.ipTypeDictCode)
        // ::1 == 1
        assertEquals(0, entity.ipStart.compareTo(java.math.BigDecimal.ONE))
        assertEquals(0, entity.ipEnd.compareTo(java.math.BigDecimal.ONE))
    }
}
