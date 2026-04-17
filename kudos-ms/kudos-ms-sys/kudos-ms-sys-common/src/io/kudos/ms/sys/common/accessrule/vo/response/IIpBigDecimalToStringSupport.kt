package io.kudos.ms.sys.common.accessrule.vo.response

import io.kudos.base.net.IpKit
import io.kudos.ms.sys.common.accessrule.enums.IpTypeEnum
import java.math.BigDecimal


/**
 * 支持将BigDecimal表示的ip转为其人可读的String表示，以便用于页面展现，兼容ipv4和ipv6
 *
 * @author K
 * @since 1.0.0
 */
interface IIpBigDecimalToStringSupport {

    /** 返回起始ip(BigDecimal表达) */
    fun getIpStartBigDecimal(): BigDecimal?

    /** 返回结束ip(BigDecimal表达) */
    fun getIpEndBigDecimal(): BigDecimal?

    /** 返回 ip 类型字典代码 */
    fun getIpTypeDictCodeStr(): String?

    /**
     * 返回ip起始值的String表示，兼容ipv4和ipv6
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
     * 返回ip结束值的String表示，兼容ipv4和ipv6
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