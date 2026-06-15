package io.kudos.ms.sys.common.accessrule.vo.response

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * test for IIpBigDecimalToStringSupport
 *
 * Covers getIpStartStr/getIpEndStr default implementations: null/blank type code returns null,
 * null numeric value returns null, IPv4 long-to-string conversion and IPv6 BigDecimal-to-full-string conversion.
 *
 * @author K
 * @since 1.0.0
 */
internal class IIpBigDecimalToStringSupportTest {

    private class Fake(
        private val start: BigDecimal?,
        private val end: BigDecimal?,
        private val type: String?,
    ) : IIpBigDecimalToStringSupport {
        override fun getIpStartBigDecimal(): BigDecimal? = start
        override fun getIpEndBigDecimal(): BigDecimal? = end
        override fun getIpTypeDictCodeStr(): String? = type
    }

    @Test
    fun nullOrBlankTypeCodeReturnsNull() {
        assertNull(Fake(BigDecimal.ONE, BigDecimal.TEN, null).getIpStartStr())
        assertNull(Fake(BigDecimal.ONE, BigDecimal.TEN, null).getIpEndStr())
        assertNull(Fake(BigDecimal.ONE, BigDecimal.TEN, "").getIpStartStr())
        assertNull(Fake(BigDecimal.ONE, BigDecimal.TEN, "   ").getIpEndStr())
    }

    @Test
    fun nullValueReturnsNull() {
        assertNull(Fake(null, null, "ipv4").getIpStartStr())
        assertNull(Fake(null, null, "ipv6").getIpEndStr())
    }

    @Test
    fun ipv4ValueIsRenderedAsDottedString() {
        val fake = Fake(BigDecimal("3232235521"), BigDecimal("167772161"), "ipv4")
        assertEquals("192.168.0.1", fake.getIpStartStr())
        assertEquals("10.0.0.1", fake.getIpEndStr())
    }

    @Test
    fun ipv6ValueIsRenderedAsFullString() {
        val fake = Fake(BigDecimal.ONE, BigDecimal(255), "ipv6")
        assertEquals("0000:0000:0000:0000:0000:0000:0000:0001", fake.getIpStartStr())
        assertEquals("0000:0000:0000:0000:0000:0000:0000:00FF", fake.getIpEndStr())
    }

    @Test
    fun nonIpv4TypeCodeUsesIpv6Rendering() {
        // any non-blank type code other than "ipv4" goes through the IPv6 branch
        val fake = Fake(BigDecimal.ZERO, null, "other")
        assertEquals("0000:0000:0000:0000:0000:0000:0000:0000", fake.getIpStartStr())
    }

}
