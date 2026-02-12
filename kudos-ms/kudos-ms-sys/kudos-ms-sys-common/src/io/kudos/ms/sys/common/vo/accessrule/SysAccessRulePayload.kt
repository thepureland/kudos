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

    /** 租户id */
    var tenantId: String? = null,

    /** 系统编码 */
    var systemCode: String? = null,

    /** 规则类型 */
    var ruleType: Int? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}