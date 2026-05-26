package io.kudos.ms.sys.common.accessrule.vo.request

import io.kudos.base.error.ServiceException
import io.kudos.base.net.IpKit
import io.kudos.base.net.IpKit.IpStorageNumericMode
import io.kudos.ms.sys.common.accessrule.enums.IpTypeEnum
import io.kudos.ms.sys.common.accessrule.enums.SysAccessRuleErrorCodeEnum
import java.math.BigDecimal

/**
 * Supports converting a human-readable String IP into its BigDecimal representation for DB storage and query; compatible with both IPv4 and IPv6.
 *
 * @author K
 * @since 1.0.0
 */
interface IIpStringToBigDecimalSupport {

    /** Returns the start IP (string representation). */
    fun getIpStartString(): String?

    /** Returns the end IP (string representation). */
    fun getIpEndString(): String?

    /** Returns the IP type dictionary code. */
    fun getIpTypeDictCodeString(): String?

    /**
     * Returns the BigDecimal representation of the start IP, compatible with IPv4 and IPv6.
     */
    fun getIpStart(): BigDecimal? {
        val ip = getIpStartString()?.takeIf { it.isNotBlank() } ?: return null
        return IpKit.ipTextToUnsignedStorageDecimal(ip, getIpStorageNumericMode())
            ?: throw ServiceException(SysAccessRuleErrorCodeEnum.INVALID_IP_START_ADDRESS)
    }

    /**
     * Returns the BigDecimal representation of the end IP, compatible with IPv4 and IPv6.
     */
    fun getIpEnd(): BigDecimal? {
        val ip = getIpEndString()?.takeIf { it.isNotBlank() } ?: return null
        return IpKit.ipTextToUnsignedStorageDecimal(ip, getIpStorageNumericMode())
            ?: throw ServiceException(SysAccessRuleErrorCodeEnum.INVALID_IP_END_ADDRESS)
    }

    private fun getIpStorageNumericMode(): IpStorageNumericMode {
        val typeCode = getIpTypeDictCodeString()?.takeIf { it.isNotBlank() } ?: return IpStorageNumericMode.AUTO
        return if (IpTypeEnum.IPV4.code == typeCode) IpStorageNumericMode.IPV4 else IpStorageNumericMode.IPV6
    }

}