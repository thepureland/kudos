package io.kudos.ms.sys.common.datasource.vo.request

import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.model.contract.entity.IIdEntity
import jakarta.validation.constraints.NotBlank


/**
 * 数据源表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysDataSourceFormUpdate (

    /** 主键 */
    @get:NotBlank
    @get:FixedLength(36)
    override val id: String,

    override val name: String,

    override val subSystemCode: String,

    override val microServiceCode: String,

    override val tenantId: String?,

    override val url: String,

    override val username: String,

    override val password: String?,

    override val initialSize: Int?,

    override val maxActive: Int?,

    override val maxIdle: Int?,

    override val minIdle: Int?,

    override val maxWait: Int?,

    override val maxAge: Int?,

    override val remark: String?,

) : IIdEntity<String>, ISysDataSourceFormBase
