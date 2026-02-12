package io.kudos.ms.sys.common.vo.tenantresource

import io.kudos.base.support.payload.FormPayload


/**
 * 租户-资源关系表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantResourcePayload (

    /** 主键 */
    override var id: String = "",

    //region your codes 1

    /** 租户id */
    var tenantId: String? = null,

    /** 资源id */
    var resourceId: String? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}