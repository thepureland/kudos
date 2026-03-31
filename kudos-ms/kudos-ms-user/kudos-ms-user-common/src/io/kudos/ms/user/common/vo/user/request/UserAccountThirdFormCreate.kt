package io.kudos.ms.user.common.vo.user.request

import java.time.LocalDateTime


/**
 * 用户第三方账号表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountThirdFormCreate (

    override val userId: String? = null,

    override val accountProviderDictCode: String? = null,

    override val accountProviderIssuer: String? = null,

    override val subject: String? = null,

    override val unionId: String? = null,

    override val externalDisplayName: String? = null,

    override val externalEmail: String? = null,

    override val avatarUrl: String? = null,

    override val lastLoginTime: LocalDateTime? = null,

    override val tenantId: String? = null,

    override val remark: String? = null,

) : IUserAccountThirdFormBase
