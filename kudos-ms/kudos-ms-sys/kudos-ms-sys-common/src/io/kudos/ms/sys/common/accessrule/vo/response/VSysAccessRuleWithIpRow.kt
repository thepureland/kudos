package io.kudos.ms.sys.common.accessrule.vo.response

import java.math.BigDecimal
import java.time.LocalDateTime


/**
 * Flattened DTO for one row of the `v_sys_access_rule_with_ip` view; used for list queries and API responses.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class VSysAccessRuleWithIpRow(

    /** Row primary key: COALESCE(ip.id, ar.id); falls back to the parent rule id when there is no IP child row. */
    val id: String = "",

    /** Parent access rule id (sys_access_rule.id). */
    val parentId: String = "",

    val tenantId: String? = null,

    /** Joined `sys_tenant.name`; null for platform-level rules (no tenant). */
    val tenantName: String? = null,

    val systemCode: String? = null,

    val accessRuleTypeDictCode: String? = null,

    val parentRemark: String? = null,

    val parentActive: Boolean? = null,

    val parentBuiltIn: Boolean? = null,

    val parentCreateUserId: String? = null,

    val parentCreateUserName: String? = null,

    val parentCreateTime: LocalDateTime? = null,

    val parentUpdateUserId: String? = null,

    val parentUpdateUserName: String? = null,

    val parentUpdateTime: LocalDateTime? = null,

    /** sys_access_rule_ip.id; null when there is no IP child row. */
    val ipId: String? = null,

    val ipStart: BigDecimal? = null,

    val ipEnd: BigDecimal? = null,

    val ipTypeDictCode: String? = null,

    val expirationTime: LocalDateTime? = null,

    val parentRuleId: String? = null,

    val remark: String? = null,

    val active: Boolean? = null,

    val builtIn: Boolean? = null,

    val createUserId: String? = null,

    val createUserName: String? = null,

    val createTime: LocalDateTime? = null,

    val updateUserId: String? = null,

    val updateUserName: String? = null,

    val updateTime: LocalDateTime? = null,

): IIpBigDecimalToStringSupport {

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
