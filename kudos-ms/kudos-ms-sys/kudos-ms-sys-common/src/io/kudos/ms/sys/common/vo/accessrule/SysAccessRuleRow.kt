package io.kudos.ms.sys.common.vo.accessrule

import io.kudos.base.model.contract.result.IdJsonResult


/**
 * 访问规则查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleRow (


    /** 主键 */
    override val id: String = "",

    /** 租户id */
    val tenantId: String? = null,

    /** 系统编码 */
    val systemCode: String? = null,

    /** 规则类型 */
    val ruleType: Int? = null,

) : IdJsonResult<String>() {


    constructor() : this("")


}