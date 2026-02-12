package io.kudos.ms.sys.common.vo.domain

import io.kudos.base.support.payload.FormPayload


/**
 * 域名表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysDomainPayload (

    /** 主键 */
    override var id: String = "",

    //region your codes 1

    /** 域名 */
    var domain: String? = null,

    /** 系统编码 */
    var systemCode: String? = null,

    /** 租户id */
    var tenantId: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}