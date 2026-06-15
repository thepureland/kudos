package io.kudos.ms.sys.common.accessrule.vo.request

import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysAccessRuleIpQuery
 *
 * Covers default values, full-args construction, ListSearchPayload overrides
 * (getReturnEntityClass / isUnpagedSearchAllowed), IIpStringToBigDecimalSupport
 * string accessors and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAccessRuleIpQueryTest {

    @Test
    fun defaultsAreAllNull() {
        val q = SysAccessRuleIpQuery()
        assertNull(q.id)
        assertNull(q.ipStartStr)
        assertNull(q.ipEndStr)
        assertNull(q.ipTypeDictCode)
        assertNull(q.expirationDate)
        assertNull(q.parentRuleId)
        assertNull(q.remark)
        assertNull(q.active)
        assertNull(q.parentRuleActive)
        assertNull(q.tenantId)
        assertNull(q.systemCode)
        assertNull(q.accessRuleTypeDictCode)
        assertNull(q.explicitNullProperties)
        assertNull(q.getIpStartString())
        assertNull(q.getIpEndString())
        assertNull(q.getIpTypeDictCodeString())
        assertNull(q.getIpStart())
        assertNull(q.getIpEnd())
    }

    @Test
    fun fullArgsConstruction() {
        val time = LocalDateTime.of(2030, 5, 6, 7, 8, 9)
        val q = SysAccessRuleIpQuery(
            id = "id-1",
            ipStartStr = "192.168.0.1",
            ipEndStr = "192.168.0.2",
            ipTypeDictCode = "ipv4",
            expirationDate = time,
            parentRuleId = "rule-1",
            remark = "r",
            active = true,
            parentRuleActive = false,
            tenantId = "t-1",
            systemCode = "sys-a",
            accessRuleTypeDictCode = "2",
            explicitNullProperties = listOf("tenantId"),
        )
        assertEquals("id-1", q.id)
        assertEquals("192.168.0.1", q.getIpStartString())
        assertEquals("192.168.0.2", q.getIpEndString())
        assertEquals("ipv4", q.getIpTypeDictCodeString())
        assertEquals(BigDecimal("3232235521"), q.getIpStart())
        assertEquals(BigDecimal("3232235522"), q.getIpEnd())
        assertEquals(time, q.expirationDate)
        assertEquals("rule-1", q.parentRuleId)
        assertEquals("r", q.remark)
        assertEquals(true, q.active)
        assertEquals(false, q.parentRuleActive)
        assertEquals("t-1", q.tenantId)
        assertEquals("sys-a", q.systemCode)
        assertEquals("2", q.accessRuleTypeDictCode)
        assertEquals(listOf("tenantId"), q.explicitNullProperties)
    }

    @Test
    fun payloadOverrides() {
        val q = SysAccessRuleIpQuery()
        assertEquals(SysAccessRuleIpRow::class, q.getReturnEntityClass())
        assertTrue(q.isUnpagedSearchAllowed())
    }

    @Test
    fun dataClassContract() {
        val a = SysAccessRuleIpQuery(id = "id-1", systemCode = "sys-a")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(systemCode = "sys-b"))
        assertTrue(a.toString().contains("id-1"))
    }

}
