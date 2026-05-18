package io.kudos.ms.sys.common.locale.vo.request

import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.model.contract.entity.IIdEntity
import jakarta.validation.constraints.NotBlank


/**
 * 语言字典表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysLocaleFormUpdate(

    /** 主键 */
    @get:NotBlank
    @get:FixedLength(36)
    override val id: String,

    override val code: String,

    override val displayName: String,

    override val englishName: String,

    override val sortNo: Int,

    override val remark: String?,

) : IIdEntity<String>, ISysLocaleFormBase
