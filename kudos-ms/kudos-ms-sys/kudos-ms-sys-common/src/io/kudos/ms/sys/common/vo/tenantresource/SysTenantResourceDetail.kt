package io.kudos.ms.sys.common.vo.tenantresource

import io.kudos.base.support.result.IdJsonResult


/**
 * 租户-资源关系查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantResourceDetail (

    /** 主键 */
    override val id: String = "",

    //region your codes 1

    /** 租户id */
    val tenantId: String? = null,

    /** 资源id */
    val resourceId: String? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    //endregion your codes 3

}