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
    override val id: String? = null,

    override val tenantId: String? = null,

    override val systemCode: String? = null,

    override val ruleType: Int? = null,

) : IIdEntity<String?>, ISysAccessRuleFormBase
