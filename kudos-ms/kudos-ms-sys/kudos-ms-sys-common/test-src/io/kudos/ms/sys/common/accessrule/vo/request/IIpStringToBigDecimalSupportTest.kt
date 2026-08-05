package io.kudos.ms.sys.common.accessrule.vo.request

import io.kudos.base.error.ServiceException
import io.kudos.ms.sys.common.accessrule.enums.SysAccessRuleErrorCodeEnum
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * test for IIpStringToBigDecimalSupport
 *
 * Covers getIpStart/getIpEnd default implementations: null/blank input returns null,
 * IPv4/IPv6/AUTO storage-mode dispatch by ip type dict code, and the ServiceException
 * branches (INVALID_IP_START_ADDRESS / INVALID_IP_END_ADDRESS) when parsing fails.
 *
 * @author K
 * @since 1.0.0
 */
internal class IIpStringToBigDecimalSupportTest {

    private class Fake(
        private val start: String?,
        private val end: String?,
        private val type: String?,
    ) : IIpStringToBigDecimalSupport {
        override fun getIpStartString(): String? = start
        override fun getIpEndString(): String? = end
        override fun getIpTypeDictCodeString(): String? = type
    }

    @Test
    fun nullOrBlankIpReturnsNull() {
        assertNull(Fake(null, null, "ipv4").getIpStart())
        assertNull(Fake(null, null, "ipv4").getIpEnd())
        assertNull(Fake("", "   ", "ipv4").getIpStart())
        assertNull(Fake("", "   ", "ipv4").getIpEnd())
    }

    @Test
    fun ipv4ModeParsesDottedAddress() {
        val fake = Fake("192.168.0.1", "192.168.0.10", "ipv4")
        assertEquals(BigDecimal("3232235521"), fake.getIpStart())
        assertEquals(BigDecimal("3232235530"), fake.getIpEnd())
    }

    @Test
    fun ipv4ModeParsesPureDecimalString() {
        val fake = Fake("3232235521", "4294967295", "ipv4")
        assertEquals(BigDecimal("3232235521"), fake.getIpStart())
        assertEquals(BigDecimal("4294967295"), fake.getIpEnd())
    }

    @Test
    fun ipv6ModeParsesAddress() {
        val fake = Fake("::1", "0000:0000:0000:0000:0000:0000:0000:00FF", "ipv6")
        assertEquals(0, BigDecimal.ONE.compareTo(fake.getIpStart()))
        assertEquals(0, BigDecimal(255).compareTo(fake.getIpEnd()))
    }

    @Test
    fun blankTypeCodeFallsBackToAutoMode() {
        // blank / null type code -> AUTO mode
        assertEquals(BigDecimal("167772161"), Fake("10.0.0.1", null, null).getIpStart())
        assertEquals(BigDecimal("167772161"), Fake("10.0.0.1", null, "  ").getIpStart())
        // AUTO: pure decimal text is taken as-is
        assertEquals(BigDecimal("12345"), Fake("12345", null, "").getIpStart())
    }

    @Test
    fun unknownNonIpv4TypeCodeIsTreatedAsIpv6() {
        // any non-blank type code other than "ipv4" goes through IPv6 mode
        val fake = Fake("::1", "::1", "whatever")
        assertNotNull(fake.getIpStart())
        assertEquals(0, BigDecimal.ONE.compareTo(fake.getIpEnd()))
    }

    @Test
    fun invalidStartAddressThrowsWithStartErrorCode() {
        val ex = assertFailsWith<ServiceException> { Fake("abc", null, "ipv4").getIpStart() }
        assertEquals(SysAccessRuleErrorCodeEnum.INVALID_IP_START_ADDRESS, ex.errorCode)
    }

    @Test
    fun invalidEndAddressThrowsWithEndErrorCode() {
        val ex = assertFailsWith<ServiceException> { Fake(null, "abc", "ipv4").getIpEnd() }
        assertEquals(SysAccessRuleErrorCodeEnum.INVALID_IP_END_ADDRESS, ex.errorCode)
    }

    @Test
    fun invalidIpv6LiteralThrows() {
        // 9 groups: invalid IPv6 literal, and not parsable as a decimal either
        val ex = assertFailsWith<ServiceException> {
            Fake("1:2:3:4:5:6:7:8:9", null, "ipv6").getIpStart()
        }
        assertEquals(SysAccessRuleErrorCodeEnum.INVALID_IP_START_ADDRESS, ex.errorCode)
    }

    @Test
    fun invalidTextInAutoModeThrows() {
        val ex = assertFailsWith<ServiceException> { Fake("not-an-ip", null, null).getIpStart() }
        assertEquals(SysAccessRuleErrorCodeEnum.INVALID_IP_START_ADDRESS, ex.errorCode)
    }

}
