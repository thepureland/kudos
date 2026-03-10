package io.kudos.ms.sys.common.vo.param

import io.kudos.base.support.payload.FormPayload
import jakarta.validation.constraints.NotBlank


/**
 * 参数表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysParamForm (

    /** 主键 */
    override val id: String = "",

    //region your codes 1

    /** 参数名称 */
    @get:NotBlank
    val paramName: String = "",

    /** 参数值 */
    @get:NotBlank
    val paramValue: String = "",

    /** 默认参数值 */
    val defaultValue: String? = null,

    /** 原子服务编码 */
    @get:NotBlank
    val atomicServiceCode: String = "",

    /** 序号 */
    val orderNum: Int? = null,

    /** 备注 */
    val remark: String? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}