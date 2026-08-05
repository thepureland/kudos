package io.kudos.ms.sys.common.accessrule.vo.request

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysAccessRuleIpFormUpdate
 *
 * Covers property accessors, the ipv4/ipv6 branches of getIpStartString/getIpEndString,
 * getIpTypeDictCodeString, and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAccessRuleIpFormUpdateTest {

    private fun ipv4Form() = SysAccessRuleIpFormUpdate(
        id = "ip-1",
        ipv4StartStr = "10.0.0.1",
        ipv4EndStr = "10.0.0.9",
        ipv6StartStr = null,
        ipv6EndStr = null,
        ipTypeDictCode = "ipv4",
        expirationDate = null,
        parentRuleId = "rule-1",
        remark = null,
    )

    private fun ipv6Form() = SysAccessRuleIpFormUpdate(
        id = "ip-2",
        ipv4StartStr = null,
        ipv4EndStr = null,
        ipv6StartStr = "::1",
        ipv6EndStr = "::2",
        ipTypeDictCode = "ipv6",
        expirationDate = null,
        parentRuleId = "rule-1",
        remark = "r",
    )

    @Test
    fun properties() {
        val form = ipv4Form()
        assertEquals("ip-1", form.id)
        assertEquals("10.0.0.1", form.ipv4StartStr)
        assertEquals("10.0.0.9", form.ipv4EndStr)
        assertNull(form.ipv6StartStr)
        assertNull(form.ipv6EndStr)
        assertEquals("ipv4", form.ipTypeDictCode)
        assertNull(form.expirationDate)
        assertEquals("rule-1", form.parentRuleId)
        assertNull(form.remark)
    }

    @Test
    fun ipv4BranchOfStringAccessors() {
        val form = ipv4Form()
        assertEquals("10.0.0.1", form.getIpStartString())
        assertEquals("10.0.0.9", form.getIpEndString())
        assertEquals("ipv4", form.getIpTypeDictCodeString())
        assertEquals(BigDecimal("167772161"), form.getIpStart())
        assertEquals(BigDecimal("167772169"), form.getIpEnd())
    }

    @Test
    fun ipv6BranchOfStringAccessors() {
        val form = ipv6Form()
        assertEquals("::1", form.getIpStartString())
        assertEquals("::2", form.getIpEndString())
        assertEquals("ipv6", form.getIpTypeDictCodeString())
        assertEquals(0, BigDecimal.ONE.compareTo(form.getIpStart()))
        assertEquals(0, BigDecimal(2).compareTo(form.getIpEnd()))
    }

    @Test
    fun dataClassContract() {
        val a = ipv6Form()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(parentRuleId = "rule-2"))
        assertTrue(a.toString().contains("ip-2"))
    }

}
