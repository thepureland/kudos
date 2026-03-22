package io.kudos.ms.sys.common.vo.resource.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import jakarta.validation.constraints.NotBlank


/**
 * 资源表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysResourceFormCreate (

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
    @get:MaxLength(128)
    val remark: String? = null,

)