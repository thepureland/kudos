package io.kudos.ms.sys.common.locale.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import jakarta.validation.constraints.NotBlank

/**
 * Base fields shared by language/locale dictionary forms (create / update).
 *
 * @author K
 * @since 1.0.0
 */
interface ISysLocaleFormBase {

    /** Language code */
    @get:NotBlank
    @get:MaxLength(32)
    val code: String

    /** Display name */
    @get:NotBlank
    @get:MaxLength(64)
    val displayName: String

    /** English name */
    @get:NotBlank
    @get:MaxLength(64)
    val englishName: String

    /** Sort number */
    val sortNo: Int

    /** Remark */
    @get:MaxLength(128)
    val remark: String?
}
