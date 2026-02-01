package io.kudos.ams.sys.common.vo.tenantsubsystem

import io.kudos.base.support.payload.FormPayload


/**
 * 租户-子系统关系表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantSubSystemPayload (

    /** 主键 */
    override var id: String? = null,

    //region your codes 1

    /** 租户id */
    var tenantId: String? = null,

    /** 子系统编码 */
    var subSystemCode: String? = null,

    /** 系统编码 */
    var systemCode: String? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}