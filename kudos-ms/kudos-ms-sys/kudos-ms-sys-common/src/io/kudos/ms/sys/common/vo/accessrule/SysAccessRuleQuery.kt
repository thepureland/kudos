package io.kudos.ms.sys.common.vo.accessrule

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 访问规则查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleQuery (

    //region your codes 1


    /** 租户id */
    val tenantId: String? = null,

    /** 系统编码 */
    val systemCode: String? = null,

    /** 规则类型 */
    val ruleType: Int? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    override var returnEntityClass: KClass<*>? = SysAccessRuleRow::class

    //endregion your codes 3

}