package io.kudos.ms.sys.common.i18n.vo.request

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.ms.sys.common.platform.consts.SysConsts
import io.kudos.ms.sys.common.platform.consts.SysDictTypes
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * Base fields shared by i18n forms (create / update).
 *
 * @author K
 * @since 1.0.0
 */
interface ISysI18nFormBase {

    /** Language_Region */
    @get:NotBlank
    @get:FixedLength(5)
    val locale: String

    /** Atomic service code */
    @get:NotBlank
    @get:MaxLength(32)
    @get:Pattern(regexp = I18nPathPolicy.SAFE_PATH_PATTERN)
    val atomicServiceCode: String

    /** I18n type dictionary code */
    @get:NotBlank
    @get:MaxLength(32)
    @get:Pattern(regexp = I18nPathPolicy.SAFE_PATH_PATTERN)
    @get:DictItemCode(dictType = SysDictTypes.I18N_TYPE, atomicServiceCode = SysConsts.ATOMIC_SERVICE_NAME)
    val i18nTypeDictCode: String

    /** I18n namespace */
    @get:NotBlank
    @get:MaxLength(128)
    @get:Pattern(regexp = I18nPathPolicy.SAFE_PATH_PATTERN)
    val namespace: String

    /** I18n key */
    @get:NotBlank
    @get:MaxLength(128)
    @get:Pattern(regexp = I18nPathPolicy.SAFE_PATH_PATTERN)
    val key: String

    /** I18n value */
    @get:NotBlank
    @get:MaxLength(1000)
    val value: String

    /** Remark */
    @get:MaxLength(128)
    val remark: String?
}
