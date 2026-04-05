package io.kudos.ms.sys.common.vo.accessrule.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.vo.accessrule.response.SysAccessRuleRow


/**
 * 访问规则列表查询条件请求 VO。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class SysAccessRuleQuery (

    /** 租户id */
    val tenantId: String? = null,

    /** 系统编码 */
    val systemCode: String? = null,

    /** 规则类型字典代码 */
    val accessRuleTypeDictCode: String? = null,

    /** 是否启用；为 null 时不按启用状态筛选 */
    val active: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysAccessRuleRow::class

    override fun isUnpagedSearchAllowed(): Boolean = true

}