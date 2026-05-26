package io.kudos.ms.sys.common.dict.vo.request

import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.support.RegExpEnum
import jakarta.validation.constraints.NotBlank

/**
 * Dictionary item form base fields (shared by create / update).
 *
 * @author K
 * @since 1.0.0
 */
interface ISysDictItemFormBase {

    /** Dictionary item code */
    @get:NotBlank
    @get:MaxLength(64)
    @get:Matches(RegExpEnum.VAR_NAME)
    val itemCode: String

    /** Dictionary item name */
    @get:NotBlank
    @get:MaxLength(64)
    val itemName: String

    /** Dictionary id */
    @get:NotBlank
    val dictId: String

    /** Dictionary item order number */
    val orderNum: Int?

    /** Parent id */
    val parentId: String?

    /** Remark */
    @get:MaxLength(128)
    val remark: String?
}
