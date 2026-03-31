package io.kudos.ms.sys.common.vo.dictitem.request

import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.support.RegExpEnum
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

/**
 * 字典项表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface ISysDictItemFormBase {

    /** 字典项代码 */
    @get:NotBlank
    @get:MaxLength(64)
    @get:Matches(RegExpEnum.VAR_NAME)
    val itemCode: String

    /** 字典项名称 */
    @get:NotBlank
    @get:MaxLength(64)
    val itemName: String

    /** 字典id */
    @get:NotBlank
    val dictId: String

    /** 字典项排序 */
    @get:Positive
    val orderNum: Int?

    /** 父id */
    val parentId: String?

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}
