package io.kudos.ms.sys.common.accessrule.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import java.time.LocalDateTime

/**
 * IP access rule list query condition request VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpQuery(

    /** Primary key. */
    val id: String? = null,

    /** IP start (string representation). */
    val ipStartStr: String? = null,

    /** IP end (string representation). */
    val ipEndStr: String? = null,

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

    /** Whether the parent rule is active. */
    val parentRuleActive: Boolean? = null,

    /** Tenant id. */
    val tenantId: String? = null,

    /** System code. */
    val systemCode: String? = null,

    /** Rule type dictionary code. */
    val accessRuleTypeDictCode: String? = null,

    /**
     * Property names that are null but should still be used as query conditions (e.g. when [tenantId] is null this indicates parent rule tenant IS NULL).
     * Semantics same as [io.kudos.base.model.payload.ISearchPayload.getNullProperties].
     */
    val explicitNullProperties: List<String>? = null,

) : ListSearchPayload(), IIpStringToBigDecimalSupport {

    override fun getReturnEntityClass() = SysAccessRuleIpRow::class

    override fun isUnpagedSearchAllowed(): Boolean = true

    override fun getIpStartString(): String? {
        return ipStartStr
    }

    override fun getIpEndString(): String? {
        return ipEndStr
    }

    override fun getIpTypeDictCodeString(): String? {
        return ipTypeDictCode
    }

}