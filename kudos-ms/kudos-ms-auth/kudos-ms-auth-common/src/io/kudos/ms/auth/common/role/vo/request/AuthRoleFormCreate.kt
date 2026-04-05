package io.kudos.ms.auth.common.role.vo.request
/**
 * 角色表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class AuthRoleFormCreate (

    override val code: String? ,

    override val name: String? ,

    override val tenantId: String? ,

    override val subsysCode: String? ,

    override val remark: String? ,

) : IAuthRoleFormBase
