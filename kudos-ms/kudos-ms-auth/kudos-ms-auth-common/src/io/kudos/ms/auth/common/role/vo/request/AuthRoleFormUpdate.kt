package io.kudos.ms.auth.common.role.vo.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 角色表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class AuthRoleFormUpdate (

    /** 主键 */
    override val id: String,

    override val code: String?,

    override val name: String?,

    override val tenantId: String?,

    override val subsysCode: String?,

    override val remark: String?,

) : IIdEntity<String>, IAuthRoleFormBase
