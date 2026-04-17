package io.kudos.ms.sys.common.accessrule.vo

import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.ms.sys.common.accessrule.vo.response.IIpBigDecimalToStringSupport
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime


/**
 * IP 访问规则缓存项：同时保留 `NUMERIC` 边界的 [BigDecimal]（[ipStart]/[ipEnd]）与十进制字符串（[ipStartStr]/[ipEndStr]）。
 * 是否为 IPv4/IPv6 由 [ipTypeDictCode] 区分；展示或定长格式可在使用方按类型从数值列解码。
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpCacheEntry (

    /** 主键 */
    override val id: String,

    val ipStart: BigDecimal,

    val ipEnd: BigDecimal,

    /** ip类型字典代码 */
    val ipTypeDictCode: String,

    /** 过期时间 */
    val expirationTime: LocalDateTime?,

) : IIdEntity<String>, Serializable, IIpBigDecimalToStringSupport {

    override fun getIpStartBigDecimal(): BigDecimal? {
        return ipStart
    }

    override fun getIpEndBigDecimal(): BigDecimal? {
        return ipEnd
    }

    override fun getIpTypeDictCodeStr(): String? {
        return ipTypeDictCode
    }

    companion object {
        private const val serialVersionUID = 6895365638061974343L
    }

}
