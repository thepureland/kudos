package io.kudos.ms.sys.common.vo.accessrule.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 访问规则表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleFormUpdate (

    /** 主键 */
    override val id: String,

    override val tenantId: String?,

    override val systemCode: String?,

    override val ruleType: Int?,

) : IIdEntity<String>, ISysAccessRuleFormBase
