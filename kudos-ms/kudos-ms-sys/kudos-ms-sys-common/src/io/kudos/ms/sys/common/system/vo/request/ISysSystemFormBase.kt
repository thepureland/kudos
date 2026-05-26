package io.kudos.ms.sys.common.system.vo.request

import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.support.RegExpEnum
import jakarta.validation.constraints.NotBlank

/**
 * System form base fields (shared by create / update).
 *
 * @author K
 * @since 1.0.0
 */
interface ISysSystemFormBase {

    /** Code */
    @get:NotBlank
    @get:MaxLength(32)
    @get:Matches(RegExpEnum.RELAXED_VAR_NAME)
    val code: String

    /** Name */
    @get:NotBlank
    @get:MaxLength(128)
    val name: String

    /** Whether it is a subsystem */
    val subSystem: Boolean

    /** Parent system code */
    @get:MaxLength(32)
    val parentCode: String?

    /** Remark */
    @get:MaxLength(128)
    val remark: String?
}
