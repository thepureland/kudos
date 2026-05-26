package io.kudos.ms.sys.common.locale.vo.request

import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.model.contract.entity.IIdEntity
import jakarta.validation.constraints.NotBlank


/**
 * Request VO for updating a language dictionary form.
 *
 * @author K
 * @since 1.0.0
 */
data class SysLocaleFormUpdate(

    /** Primary key */
    @get:NotBlank
    @get:FixedLength(36)
    override val id: String,

    override val code: String,

    override val displayName: String,

    override val englishName: String,

    override val sortNo: Int,

    override val remark: String?,

) : IIdEntity<String>, ISysLocaleFormBase
