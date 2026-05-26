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
    fun getIpStartStr(): String? {
        if (getIpTypeDictCodeStr().isNullOrBlank() || getIpStartBigDecimal() == null)
            return null
        return if (IpTypeEnum.IPV4.code == getIpTypeDictCodeStr()) {
            IpKit.ipv4LongToString(getIpStartBigDecimal()!!.toLong())
        } else {
            IpKit.ipv6BigDecimalToFullString(getIpStartBigDecimal())
        }
    }

    /**
     * Returns the String representation of the end IP, compatible with IPv4 and IPv6.
     */
    fun getIpEndStr(): String? {
        if (getIpTypeDictCodeStr().isNullOrBlank() || getIpEndBigDecimal() == null)
            return null
        return if (IpTypeEnum.IPV4.code == getIpTypeDictCodeStr()) {
            IpKit.ipv4LongToString(getIpEndBigDecimal()!!.toLong())
        } else {
            IpKit.ipv6BigDecimalToFullString(getIpEndBigDecimal())
        }
    }

}