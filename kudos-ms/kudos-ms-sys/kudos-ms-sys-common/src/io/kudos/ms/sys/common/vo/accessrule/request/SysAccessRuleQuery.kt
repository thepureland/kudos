package io.kudos.ms.sys.common.vo.accessrule.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.vo.accessrule.response.SysAccessRuleRow


/**
 * 访问规则列表查询条件请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleQuery (

    /** 租户id */
    val tenantId: String? = null,

    /** 系统编码 */
    val systemCode: String? = null,

    /** 规则类型 */
    val ruleType: Int? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysAccessRuleRow::class

    override fun isUnpagedSearchAllowed(): Boolean = true

    override fun getSortableProperties() = setOf(
        ::tenantId.name,
        ::systemCode.name,
    )

}