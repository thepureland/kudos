package io.kudos.ms.sys.common.tenant.vo.request
import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.model.contract.entity.IIdEntity
import jakarta.validation.constraints.NotBlank


/**
 * 租户表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantFormUpdate (

    /** 主键 */
    @get:NotBlank
    @get:FixedLength(36)
    override val id: String,

    override val name: String,

    override var subSystemCodes: Set<String> = emptySet(),

    override val timezone: String?,

    override val defaultLanguageCode: String?,

    override val remark: String?,

) : IIdEntity<String>, ISysTenantFormBase
