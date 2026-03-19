package io.kudos.ms.sys.common.vo.accessrule.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 访问规则表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleFormUpdate (

    override val id: String? = null,

    /** 租户id */
    val tenantId: String? = null,

    /** 系统编码 */
    val systemCode: String? = null,

    /** 规则类型 */
    val ruleType: Int? = null,

) : IIdEntity<String?>