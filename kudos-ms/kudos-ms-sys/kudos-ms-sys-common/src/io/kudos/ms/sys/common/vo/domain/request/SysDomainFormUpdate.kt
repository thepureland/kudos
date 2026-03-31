package io.kudos.ms.sys.common.vo.domain.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 域名表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysDomainFormUpdate (

    /** 主键 */
    override val id: String? = null,

    override val domain: String = "",

    override val systemCode: String = "",

    override val tenantId: String = "",

    override val remark: String? = null,

) : IIdEntity<String?>, ISysDomainFormBase
