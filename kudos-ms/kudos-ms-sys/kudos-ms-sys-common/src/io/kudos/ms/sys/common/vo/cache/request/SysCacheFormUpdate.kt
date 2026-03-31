package io.kudos.ms.sys.common.vo.cache.request

import io.kudos.base.model.contract.entity.IIdEntity

/**
 * 缓存表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysCacheFormUpdate (

    /** 主键 */
    override val id: String? = null,

    override val strategyDictCode: String = "",

    override val writeOnBoot: Boolean = true,

    override val writeInTime: Boolean = true,

    override val ttl: Int? = null,

    override val remark: String? = null,

) : IIdEntity<String?>, ISysCacheFormBase