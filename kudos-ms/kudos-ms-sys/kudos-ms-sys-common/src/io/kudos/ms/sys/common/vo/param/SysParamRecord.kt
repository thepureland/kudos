package io.kudos.ms.sys.common.vo.param

import io.kudos.base.support.result.IdJsonResult


/**
 * 参数查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysParamRecord (

    //region your codes 1

    /** 主键 */
    override val id: String = "",

    /** 参数名称 */
    val paramName: String? = null,

    /** 参数值 */
    val paramValue: String? = null,

    /** 默认参数值 */
    val defaultValue: String? = null,

    /** 模块 */
    val atomicServiceCode: String? = null,

    /** 序号 */
    val orderNum: Int? = null,

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