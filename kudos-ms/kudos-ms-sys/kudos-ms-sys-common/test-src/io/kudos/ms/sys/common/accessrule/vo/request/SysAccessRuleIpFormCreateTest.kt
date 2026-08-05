package io.kudos.ms.sys.common.accessrule.vo.request

import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysAccessRuleIpFormCreate
 *
 * Covers property accessors, the ipv4/ipv6 branches of getIpStartString/getIpEndString,
 * getIpTypeDictCodeString, the inherited getIpStart/getIpEnd conversion, and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAccessRuleIpFormCreateTest {

    private fun ipv4Form() = SysAccessRuleIpFormCreate(
        ipv4StartStr = "192.168.0.1",
        ipv4EndStr = "192.168.0.10",
        ipv6StartStr = null,
        ipv6EndStr = null,
        ipTypeDictCode = "ipv4",
        expirationDate = LocalDateTime.of(2031, 12, 31, 23, 59, 59),
        systemCode = "sys-a",
        tenantId = "t-1",
        remark = "r",
    )

    private fun ipv6Form() = SysAccessRuleIpFormCreate(
        ipv4StartStr = null,
        ipv4EndStr = null,
        ipv6StartStr = "::1",
        ipv6EndStr = "::ff",
        ipTypeDictCode = "ipv6",
        expirationDate = null,
        systemCode = "sys-a",
        tenantId = "t-1",
        remark = null,
    )

    @Test
    fun properties() {
        val form = ipv4Form()
        assertEquals("192.168.0.1", form.ipv4StartStr)
        assertEquals("192.168.0.10", form.ipv4EndStr)
        assertNull(form.ipv6StartStr)
        assertNull(form.ipv6EndStr)
        assertEquals("ipv4", form.ipTypeDictCode)
        assertEquals(LocalDateTime.of(2031, 12, 31, 23, 59, 59), form.expirationDate)
        assertEquals("sys-a", form.systemCode)
        assertEquals("t-1", form.tenantId)
        assertEquals("r", form.remark)
    }

    @Test
    fun ipv4BranchOfStringAccessors() {
        val form = ipv4Form()
        assertEquals("192.168.0.1", form.getIpStartString())
        assertEquals("192.168.0.10", form.getIpEndString())
        assertEquals("ipv4", form.getIpTypeDictCodeString())
        assertEquals(BigDecimal("3232235521"), form.getIpStart())
        assertEquals(BigDecimal("3232235530"), form.getIpEnd())
    }

    @Test
    fun ipv6BranchOfStringAccessors() {
        val form = ipv6Form()
        assertEquals("::1", form.getIpStartString())
        assertEquals("::ff", form.getIpEndString())
        assertEquals("ipv6", form.getIpTypeDictCodeString())
        assertEquals(0, BigDecimal.ONE.compareTo(form.getIpStart()))
        assertEquals(0, BigDecimal(255).compareTo(form.getIpEnd()))
    }

    @Test
    fun dataClassContract() {
        val a = ipv4Form()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(tenantId = "t-2"))
        assertTrue(a.toString().contains("192.168.0.1"))
    }

}
