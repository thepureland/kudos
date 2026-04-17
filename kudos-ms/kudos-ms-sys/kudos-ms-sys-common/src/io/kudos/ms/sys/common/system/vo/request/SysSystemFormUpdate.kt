package io.kudos.ms.sys.common.system.vo.request

import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.model.contract.entity.IIdEntity
import jakarta.validation.constraints.NotBlank


/**
 * 系统表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysSystemFormUpdate (

    @get:NotBlank
    override val code: String,

    override val name: String,

    override val subSystem: Boolean = true,

    override val parentCode: String?,

    override val remark: String?,

) : ISysSystemFormBase, IIdEntity<String> {

    override val id: String
        get() = code

}
