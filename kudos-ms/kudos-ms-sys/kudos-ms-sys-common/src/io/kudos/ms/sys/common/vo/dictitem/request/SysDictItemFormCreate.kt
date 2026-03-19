package io.kudos.ms.sys.common.vo.dictitem.request

import jakarta.validation.constraints.NotBlank


/**
 * 字典项表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemFormCreate (

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

)