package io.kudos.ms.sys.common.vo.accessrule

import io.kudos.base.support.payload.FormPayload


/**
 * 访问规则表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRulePayload (

    //region your codes 1

    override val id: String,

    /** 租户id */
    val tenantId: String? = null,

    /** 系统编码 */
    val systemCode: String? = null,

    /** 规则类型 */
    val ruleType: Int? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}