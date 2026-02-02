package io.kudos.ms.sys.common.vo.domain

import io.kudos.base.support.result.IdJsonResult


/**
 * 域名查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysDomainRecord (

    //region your codes 1

    /** 主键 */
    override var id: String? = null,

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

    /** 是否内置 */
    var builtIn: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}