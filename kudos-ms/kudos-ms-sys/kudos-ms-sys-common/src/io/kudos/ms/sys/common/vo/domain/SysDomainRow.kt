package io.kudos.ms.sys.common.vo.domain

import io.kudos.base.support.result.IdJsonResult


/**
 * 域名查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysDomainRow (

    //region your codes 1

    /** 主键 */
    override val id: String = "",

    /** 域名 */
    val domain: String = "",

    /** 系统编码 */
    val systemCode: String = "",

    /** 租户id */
    val tenantId: String = "",

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = false,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}