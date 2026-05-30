package io.kudos.ms.auth.common.role.vo.request

/**
 * Request VO for role form creation.
 *
 * @author K
 * @since 1.0.0
 */
data class AuthRoleFormCreate (

    override val code: String? ,

    override val name: String? ,

    override val tenantId: String? ,

    override val subsysCode: String? ,

    override val parentId: String? = null,

    override val approvalRequired: Boolean? = false,

    override val dataScope: String? = null,

    override val remark: String? ,

) : IAuthRoleFormBase
