package io.kudos.ms.sys.common.vo.resource.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import jakarta.validation.constraints.NotBlank

/**
 * 资源表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface ISysResourceFormBase {

    /** 名称 */
    @get:NotBlank
    val name: String

    /** url */
    val url: String?

    /** 资源类型字典代码 */
    @get:NotBlank
    val resourceTypeDictCode: String

    /** 父id */
    val parentId: String?

    /** 在同父节点下的排序号 */
    val orderNum: Int?

    /** 图标 */
    val icon: String?

    /** 子系统编码 */
    @get:NotBlank
    val subSystemCode: String

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}
