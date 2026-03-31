package io.kudos.ms.auth.common.vo.role.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 角色表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class AuthRoleFormUpdate (

    /** 主键 */
    override val id: String? = null,

    override val code: String? = null,

    override val name: String? = null,

    override val tenantId: String? = null,

    override val subsysCode: String? = null,

    override val remark: String? = null,

) : IIdEntity<String?>, IAuthRoleFormBase
