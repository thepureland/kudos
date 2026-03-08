package io.kudos.ms.sys.common.vo.param

import io.kudos.base.support.payload.FormPayload


/**
 * 参数表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysParamPayload (

    /** 主键 */
    override val id: String = "",

    //region your codes 1

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

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}