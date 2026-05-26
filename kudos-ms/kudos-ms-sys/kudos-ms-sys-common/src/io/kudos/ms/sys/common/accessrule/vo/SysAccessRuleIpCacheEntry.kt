package io.kudos.ms.sys.common.accessrule.vo

import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.ms.sys.common.accessrule.vo.response.IIpBigDecimalToStringSupport
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime


/**
 * IP access rule cache entry: keeps both the `NUMERIC` boundary as [BigDecimal] ([ipStart]/[ipEnd]) and the decimal strings ([ipStartStr]/[ipEndStr]).
 * Whether it is IPv4 or IPv6 is distinguished by [ipTypeDictCode]; for display or fixed-length formats, callers can decode from the numeric column based on type.
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpCacheEntry (

    /** Primary key. */
    override val id: String,

    val ipStart: BigDecimal,

    val ipEnd: BigDecimal,

    /** IP type dictionary code. */
    val ipTypeDictCode: String,

    /** Expiration time. */
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
