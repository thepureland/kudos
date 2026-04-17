package io.kudos.ms.sys.common.domain.vo.request

import io.kudos.base.bean.validation.constraint.annotations.FixedLength
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
    @get:NotBlank
    @get:FixedLength(36)
    override val id: String,

    override val domain: String,

    override val systemCode: String,

    override val tenantId: String,

    override val remark: String?,

) : IIdEntity<String>, ISysDomainFormBase
