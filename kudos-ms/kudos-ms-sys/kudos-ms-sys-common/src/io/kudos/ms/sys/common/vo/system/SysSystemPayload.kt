package io.kudos.ms.sys.common.vo.system

import io.kudos.base.support.payload.FormPayload
import jakarta.validation.constraints.NotBlank


/**
 * 系统表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysSystemPayload (

    //region your codes 1

    /** 编码 */
    @get:NotBlank
    val code: String = "",

    /** 名称 */
    @get:NotBlank
    val name: String = "",

    /** 是否子系统 */
    val subSystem: Boolean = true,

    /** 父系统编号 */
    val parentCode: String? = null,

    /** 备注 */
    val remark: String? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    override val id: String
        get() = code

    // endregion your codes 3

}
