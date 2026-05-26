package io.kudos.ms.sys.common.accessrule.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.accessrule.vo.response.VSysAccessRuleWithIpRow

/**
 * List/paginated query criteria payload, corresponding to view `v_sys_access_rule_with_ip`,
 * returning row type [VSysAccessRuleWithIpRow].
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class VSysAccessRuleWithIpQuery(

    /** View row primary key, semantically `COALESCE(ip.id, ar.id)` */
    val id: String? = null,

    /** Parent access rule primary key `sys_access_rule.id` */
    val parentId: String? = null,

    /** Parent rule tenant id; combine with [explicitNullProperties] to explicitly query NULL */
    val tenantId: String? = null,

    /** Parent rule system code */
    val systemCode: String? = null,

    /** Parent rule type dict code */
    val accessRuleTypeDictCode: String? = null,

    /** Whether the parent rule is active */
    val parentActive: Boolean? = null,

    /** Whether the parent rule is built-in */
    val parentBuiltIn: Boolean? = null,

    /** IP child table primary key `sys_access_rule_ip.id` */
    val ipId: String? = null,

    /** IP start (string form) */
    val ipStartStr: String? = null,

    /** IP end (string form) */
    val ipEndStr: String? = null,

    /** IP type dict code */
    val ipTypeDictCode: String? = null,

    /**
     * Field names that should participate in WHERE as "property IS NULL"
     * (e.g. when only containing `tenantId` it means parent rule `tenant_id IS NULL`).
     */
    val explicitNullProperties: List<String>? = null,

) : ListSearchPayload(), IIpStringToBigDecimalSupport {

    override fun getReturnEntityClass() = VSysAccessRuleWithIpRow::class

    override fun isUnpagedSearchAllowed(): Boolean = true

    override fun getNullProperties(): List<String>? {
        val raw = explicitNullProperties ?: return null
        if (raw.isEmpty()) return raw
        return raw.map { name ->
            when (name) {
                "ipStartStr" -> "ipStart"
                "ipEndStr" -> "ipEnd"
                else -> name
            }
        }
    }

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
