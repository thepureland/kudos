package io.kudos.ms.sys.common.vo.dictitem

import io.kudos.base.support.payload.FormPayload
import jakarta.validation.constraints.NotBlank


/**
 * 字典项表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemForm (

    /** 主键 */
    override val id: String? = null,

    //region your codes 1

    /** 字典项代码 */
    @get:NotBlank
    val itemCode: String = "",

    /** 字典项名称 */
    @get:NotBlank
    val itemName: String = "",

    /** 字典id */
    val dictId: String = "",

    /** 字典项排序 */
    val orderNum: Int? = null,

    /** 父id */
    val parentId: String? = null,

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