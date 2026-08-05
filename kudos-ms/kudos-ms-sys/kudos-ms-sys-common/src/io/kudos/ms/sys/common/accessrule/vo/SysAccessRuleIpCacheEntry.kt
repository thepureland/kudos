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

    /**
     * Access rule type dictionary code of the parent rule (`sys_access_rule.access_rule_type_dict_code`,
     * see `AccessRuleTypeEnum`: 1=unlimited, 2=whitelist, 3=blacklist, 4=whitelist+blacklist).
     * Required by `checkIpAccess` to tell a deny rule (blacklist) from an allow rule (whitelist).
     * `null` only for legacy entries serialized before this field existed; such entries never deny access.
     */
    val accessRuleTypeDictCode: String? = null,

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
