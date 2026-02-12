package io.kudos.ms.sys.common.vo.accessrule

import io.kudos.base.support.result.IdJsonResult


/**
 * 访问规则查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleRecord (

    //region your codes 1

    /** 主键 */
    override var id: String = "",

    /** 租户id */
    var tenantId: String? = null,

    /** 系统编码 */
    var systemCode: String? = null,

    /** 规则类型 */
    var ruleType: Int? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}