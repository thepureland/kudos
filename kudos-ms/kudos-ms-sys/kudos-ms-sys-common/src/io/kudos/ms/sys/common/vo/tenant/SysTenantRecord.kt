package io.kudos.ms.sys.common.vo.tenant

import io.kudos.base.support.result.IdJsonResult


/**
 * 租户查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantRecord (

    //region your codes 1

    /** 主键 */
    override val id: String = "",

    /** 名称 */
    val name: String? = null,

    /** 时区 */
    val timezone: String? = null,

    /** 默认语言编码 */
    val defaultLanguageCode: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}