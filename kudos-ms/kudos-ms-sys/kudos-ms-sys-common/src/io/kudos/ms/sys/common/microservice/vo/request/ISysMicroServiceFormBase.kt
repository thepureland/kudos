package io.kudos.ms.sys.common.microservice.vo.request

import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.support.RegExpEnum
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * Base fields shared by microservice forms (create / update).
 *
 * @author K
 * @since 1.0.0
 */
interface ISysMicroServiceFormBase {

    /** Code */
    @get:NotBlank
    @get:MaxLength(32)
    @get:Matches(RegExpEnum.RELAXED_VAR_NAME)
    val code: String

    /** Name */
    @get:NotBlank
    @get:MaxLength(128)
    val name: String

    /** Context */
    @get:NotBlank
    @get:Matches(RegExpEnum.CONTEXT)
    val context: String

    /** Whether atomic service */
    val atomicService: Boolean

    /** Parent service code */
    @get:MaxLength(32)
    @get:Matches(RegExpEnum.VAR_NAME)
    val parentCode: String?

    /** Remark */
    @get:MaxLength(128)
    val remark: String?
}
