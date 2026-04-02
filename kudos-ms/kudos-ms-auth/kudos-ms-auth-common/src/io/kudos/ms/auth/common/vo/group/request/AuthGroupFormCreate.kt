package io.kudos.ms.auth.common.vo.group.request


/**
 * 用户组表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class AuthGroupFormCreate (

    override val code: String? ,

    override val name: String? ,

    override val tenantId: String? ,

    override val subsysCode: String? ,

    override val remark: String? ,

) : IAuthGroupFormBase
