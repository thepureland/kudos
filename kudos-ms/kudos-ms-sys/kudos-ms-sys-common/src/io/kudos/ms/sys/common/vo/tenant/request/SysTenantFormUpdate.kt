package io.kudos.ms.sys.common.vo.tenant.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 租户表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantFormUpdate (

    /** 主键 */
    override val id: String? = null,

    override val name: String = "",

    override var subSystemCodes: Set<String> = emptySet(),

    override val timezone: String? = null,

    override val defaultLanguageCode: String? = null,

    override val remark: String? = null,

) : IIdEntity<String?>, ISysTenantFormBase
