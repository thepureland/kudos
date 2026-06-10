package io.kudos.ms.sys.common.accessrule.vo.response

import io.kudos.base.net.IpKit
import io.kudos.ms.sys.common.accessrule.enums.IpTypeEnum
import java.math.BigDecimal


/**
 * Supports converting a BigDecimal IP into its human-readable String representation for UI display; compatible with both IPv4 and IPv6.
 *
 * @author K
 * @since 1.0.0
 */
interface IIpBigDecimalToStringSupport {

    /** Returns the start IP (BigDecimal representation). */
    fun getIpStartBigDecimal(): BigDecimal?

    /** Returns the end IP (BigDecimal representation). */
    fun getIpEndBigDecimal(): BigDecimal?

    /** Returns the IP type dictionary code. */
    fun getIpTypeDictCodeStr(): String?

    /**
     * Returns the String representation of the start IP, compatible with IPv4 and IPv6.
     */
    fun getIpStartStr(): String? = toIpString(getIpStartBigDecimal())

    /**
     * Returns the String representation of the end IP, compatible with IPv4 and IPv6.
     */
    fun getIpEndStr(): String? = toIpString(getIpEndBigDecimal())

    /**
     * Converts a BigDecimal IP value to its String form, dispatching on the IP type dictionary code.
     * Returns null when the type code is blank or the value is null.
     */
    private fun toIpString(value: BigDecimal?): String? {
        val typeCode = getIpTypeDictCodeStr()?.takeIf { it.isNotBlank() } ?: return null
        if (value == null) return null
        return if (IpTypeEnum.IPV4.code == typeCode) {
            IpKit.ipv4LongToString(value.toLong())
        } else {
            IpKit.ipv6BigDecimalToFullString(value)
        }
    }

}