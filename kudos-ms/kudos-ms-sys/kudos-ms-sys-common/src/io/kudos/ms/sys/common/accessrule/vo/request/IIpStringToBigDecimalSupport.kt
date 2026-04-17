package io.kudos.ms.sys.common.accessrule.vo.request

import io.kudos.base.error.ServiceException
import io.kudos.base.net.IpKit
import io.kudos.base.net.IpKit.IpStorageNumericMode
import io.kudos.ms.sys.common.accessrule.enums.IpTypeEnum
import io.kudos.ms.sys.common.accessrule.enums.SysAccessRuleErrorCodeEnum
import java.math.BigDecimal

/**
 * 支持将人可读的String表示的ip串转为其BigDecimal表示，以便DB进行存储和查询，兼容ipv4和ipv6
 *
 * @author K
 * @since 1.0.0
 */
interface IIpStringToBigDecimalSupport {

    /** 返回起始ip(字符串表达) */
    fun getIpStartString(): String?

    /** 返回结束ip(字符串表达) */
    fun getIpEndString(): String?

    /** 返回 ip 类型字典代码 */
    fun getIpTypeDictCodeString(): String?

    /**
     * 返回ip起始值的BigDecimal表示，兼容ipv4和ipv6
     */
    fun getIpStart(): BigDecimal? {
        if (getIpStartString().isNullOrBlank())
            return null
        return IpKit.ipTextToUnsignedStorageDecimal(getIpStartString()!!, getIpStorageNumericMode())
            ?: throw ServiceException(SysAccessRuleErrorCodeEnum.INVALID_IP_START_ADDRESS)
    }

    /**
     * 返回ip结束值的BigDecimal表示，兼容ipv4和ipv6
     */
    fun getIpEnd(): BigDecimal? {
        if (getIpEndString().isNullOrBlank())
            return null
        return IpKit.ipTextToUnsignedStorageDecimal(getIpEndString()!!, getIpStorageNumericMode())
            ?: throw ServiceException(SysAccessRuleErrorCodeEnum.INVALID_IP_END_ADDRESS)
    }

    private fun getIpStorageNumericMode(): IpStorageNumericMode {
        if (getIpTypeDictCodeString().isNullOrBlank())
            return IpStorageNumericMode.AUTO
        return if (IpTypeEnum.IPV4.code == getIpTypeDictCodeString()) {
            IpStorageNumericMode.IPV4
        } else {
            IpStorageNumericMode.IPV6
        }
    }

}