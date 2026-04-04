package io.kudos.ms.sys.common.vo.cache.request

import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.model.contract.entity.IIdEntity
import jakarta.validation.constraints.NotBlank

/**
 * 缓存表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysCacheFormUpdate (

    /** 主键 */
    @get:NotBlank
    @get:FixedLength(36)
    override val id: String,

    override val strategyDictCode: String,

    override val writeOnBoot: Boolean = true,

    override val writeInTime: Boolean = true,

    override val ttl: Int?,

    override val remark: String?,

) : IIdEntity<String>, ISysCacheFormBase
