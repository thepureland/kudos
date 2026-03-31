package io.kudos.ms.auth.common.vo.group.request


/**
 * 用户组表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class AuthGroupFormCreate (

    override val code: String? = null,

    override val name: String? = null,

    override val tenantId: String? = null,

    override val subsysCode: String? = null,

    override val remark: String? = null,

) : IAuthGroupFormBase
