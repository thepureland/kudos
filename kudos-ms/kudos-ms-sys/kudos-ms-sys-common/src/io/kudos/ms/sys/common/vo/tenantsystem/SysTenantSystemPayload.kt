package io.kudos.ms.sys.common.vo.tenantsystem

import io.kudos.base.support.payload.FormPayload


/**
 * 租户-系统关系表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantSystemPayload (

    /** 主键 */
    override val id: String = "",

    //region your codes 1

    /** 租户id */
    val tenantId: String? = null,

    /** 系统编码 */
    val systemCode: String? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}
