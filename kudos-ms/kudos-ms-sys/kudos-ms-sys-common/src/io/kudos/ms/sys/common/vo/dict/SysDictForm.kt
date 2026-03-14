package io.kudos.ms.sys.common.vo.dict

import io.kudos.base.support.payload.FormPayload
import jakarta.validation.constraints.NotBlank


/**
 * 字典表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictForm (

    /** 主键 */
    override val id: String? = null,

    //region your codes 1

    /** 字典类型 */
    @get:NotBlank
    val dictType: String = "",

    /** 字典名称 */
    @get:NotBlank
    val dictName: String = "",

    /** 原子服务编码 */
    @get:NotBlank
    val atomicServiceCode: String = "",

    /** 备注 */
    val remark: String? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String?>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}