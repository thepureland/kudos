package io.kudos.ms.sys.common.param.vo.request

import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.support.RegExpEnum
import jakarta.validation.constraints.NotBlank

/**
 * Base fields shared by parameter forms (create / update).
 *
 * @author K
 * @since 1.0.0
 */
interface ISysParamFormBase {

    /** Parameter name */
    @get:NotBlank
    @get:MaxLength(32)
    @get:Matches(RegExpEnum.RELAXED_VAR_NAME)
    val paramName: String

    /** Parameter value */
    @get:NotBlank
    @get:MaxLength(256)
    val paramValue: String

    /** Default parameter value */
    @get:MaxLength(256)
    val defaultValue: String?

    /** Atomic service code */
    @get:NotBlank
    @get:Matches(RegExpEnum.VAR_NAME)
    val atomicServiceCode: String

    /** Order number */
    val orderNum: Int?

    /** Remark */
    @get:MaxLength(128)
    val remark: String?
}
