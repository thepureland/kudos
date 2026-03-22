package io.kudos.ms.sys.common.vo.system.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.model.contract.entity.IIdEntity
import jakarta.validation.constraints.NotBlank


/**
 * 系统表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysSystemFormUpdate (

    /** 编码 */
    @get:NotBlank
    val code: String = "",

    /** 名称 */
    @get:NotBlank
    val name: String = "",

    /** 是否子系统 */
    val subSystem: Boolean = true,

    /** 父系统编号 */
    val parentCode: String? = null,

    /** 备注 */
    @get:MaxLength(128)
    val remark: String? = null,

) : IIdEntity<String> {

    override val id: String
        get() = code

}