package io.kudos.ms.sys.common.vo.accessrule

import io.kudos.base.support.payload.ListSearchPayload


/**
 * 访问规则查询条件载体
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

    constructor() : this("")

    override fun getReturnEntityClass() = SysAccessRuleRow::class

    override fun isUnpagedSearchAllowed(): Boolean = true

}