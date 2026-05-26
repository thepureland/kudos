package io.kudos.ms.sys.common.dict.vo.request

import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.support.RegExpEnum
import jakarta.validation.constraints.NotBlank

/**
 * Dictionary form base fields (shared by create / update).
 *
 * @author K
 * @since 1.0.0
 */
interface ISysDictFormBase {

    /** Dictionary type */
    @get:NotBlank
    @get:MaxLength(64)
    @get:Matches(RegExpEnum.VAR_NAME)
    val dictType: String

    /** Dictionary name */
    @get:NotBlank
    @get:MaxLength(64)
    val dictName: String

    /** Atomic service code */
    @get:NotBlank
    @get:Matches(RegExpEnum.VAR_NAME)
    @get:MaxLength(32)
    val atomicServiceCode: String

    /** Remark */
    @get:MaxLength(128)
    val remark: String?
}
