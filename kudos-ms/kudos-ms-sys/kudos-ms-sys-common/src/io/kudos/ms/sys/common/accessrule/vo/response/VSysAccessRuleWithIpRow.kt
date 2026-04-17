package io.kudos.ms.sys.common.accessrule.vo.response

import java.math.BigDecimal
import java.time.LocalDateTime


/**
 * 视图 `v_sys_access_rule_with_ip` 一行的扁平 DTO，用于列表查询与 API 出参。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class VSysAccessRuleWithIpRow(

    /** 行主键：COALESCE(ip.id, ar.id)；无 IP 子行时为父规则 id */
    val id: String = "",

    /** 父访问规则 id（sys_access_rule.id） */
    val parentId: String = "",

    val tenantId: String? = null,

    /** 关联 `sys_tenant.name`；平台级规则（无租户）为 null */
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

    /** sys_access_rule_ip.id，无 IP 子行时为 null */
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
