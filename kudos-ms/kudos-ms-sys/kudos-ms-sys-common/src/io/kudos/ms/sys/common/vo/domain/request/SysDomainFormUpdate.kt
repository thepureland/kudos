package io.kudos.ms.sys.common.vo.domain.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.model.contract.entity.IIdEntity
import jakarta.validation.constraints.NotBlank


/**
 * 域名表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysDomainFormUpdate (

    /** 主键 */
    override val id: String? = null,

    /** 域名 */
    @get:NotBlank
    val domain: String = "",

    /** 系统编码 */
    @get:NotBlank
    val systemCode: String = "",

    /** 租户id */
    @get:NotBlank
    val tenantId: String = "",

    /** 备注 */
    @get:MaxLength(128)
    val remark: String? = null,

) : IIdEntity<String?>