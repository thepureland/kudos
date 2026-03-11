package io.kudos.ms.sys.common.vo.resource

import io.kudos.base.support.payload.FormPayload
import jakarta.validation.constraints.NotBlank


/**
 * 资源表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysResourceForm (

    /** 主键 */
    override val id: String? = null,

    //region your codes 1

    /** 名称 */
    @get:NotBlank
    val name: String = "",

    /** url */
    val url: String? = null,

    /** 资源类型字典代码 */
    @get:NotBlank
    val resourceTypeDictCode: String = "",

    /** 父id */
    val parentId: String? = null,

    /** 在同父节点下的排序号 */
    val orderNum: Int? = null,

    /** 图标 */
    val icon: String? = null,

    /** 子系统编码 */
    @get:NotBlank
    val subSystemCode: String = "",

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean? = true,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String?>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}