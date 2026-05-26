package io.kudos.ms.sys.common.outline.vo.request

import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.model.contract.entity.IIdEntity
import jakarta.validation.constraints.NotBlank


/**
 * Request VO for updating an outbound whitelist form.
 *
 * @author K
 * @since 1.0.0
 */
data class SysOutLineFormUpdate(

    /** Primary key */
    @get:NotBlank
    @get:FixedLength(36)
    override val id: String,

    override val name: String,

    override val host: String,

    override val port: Int?,

    override val protocol: String,

    override val systemCode: String,

    override val tenantId: String?,

    override val remark: String?,

) : IIdEntity<String>, ISysOutLineFormBase
