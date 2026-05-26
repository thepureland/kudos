package io.kudos.ms.sys.common.accessrule.vo.response

import java.math.BigDecimal
import java.time.LocalDateTime


/**
 * IP access rule list query result response VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpRow (

    /** IP rule id. */
    val id: String = "",

    /** Range start numeric value. */
    val ipStart: BigDecimal,

    /** Range end numeric value. */
    val ipEnd: BigDecimal,

    /** IP type dictionary code. */
    val ipTypeDictCode: String,

    /** Expiration time. */
    val expirationTime: LocalDateTime? = null,

    /** Parent rule id. */
    val parentRuleId: String? = null,

    /** Remark. */
    val remark: String? = null,

    /** Whether active. */
    val active: Boolean? = null,

    /** Whether the parent rule is active. */
    val parentRuleActive: Boolean? = null,

    /** Tenant id. */
    val tenantId: String? = null,

    /** System code. */
    val systemCode: String? = null,

    /** Rule type dictionary code. */
    val accessRuleTypeDictCode: String? = null,

): IIpBigDecimalToStringSupport {

    override fun getIpStartBigDecimal(): BigDecimal {
        return this.ipStart
    }

    override fun getIpEndBigDecimal(): BigDecimal {
        return this.ipEnd
    }

    override fun getIpTypeDictCodeStr(): String {
        return this.ipTypeDictCode
    }

}
