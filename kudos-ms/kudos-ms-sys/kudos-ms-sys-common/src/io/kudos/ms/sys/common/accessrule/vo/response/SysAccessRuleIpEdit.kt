package io.kudos.ms.sys.common.accessrule.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.math.BigDecimal
import java.time.LocalDateTime


/**
 * IP access rule edit response VO: [ipStart]/[ipEnd] are decimal strings for the in-DB `NUMERIC` boundary; `ipv4*Str`/`ipv6*Str` are provided for form binding.
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpEdit (

    override val id: String = "",

    val ipStart: BigDecimal? = null,

    val ipEnd: BigDecimal? = null,

    /** IP type dictionary code. */
    val ipTypeDictCode: String? = null,

    /** Expiration time. */
    val expirationDate: LocalDateTime? = null,

    /** Parent rule id. */
    val parentRuleId: String? = null,

    /** Remark. */
    val remark: String? = null,

    /** Whether active. */
    val active: Boolean? = null,

    /** Creator id. */
    val createUserId: String? = null,

    /** Creator name. */
    val createUserName: String? = null,

    /** Create time. */
    val createTime: LocalDateTime? = null,

    /** Updater id. */
    val updateUserId: String? = null,

    /** Updater name. */
    val updateUserName: String? = null,

    /** Update time. */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>, IIpBigDecimalToStringSupport {

    override fun getIpStartBigDecimal(): BigDecimal? {
        return ipStart
    }

    override fun getIpEndBigDecimal(): BigDecimal? {
        return ipEnd
    }

    override fun getIpTypeDictCodeStr(): String? {
        return ipTypeDictCode
    }

}
