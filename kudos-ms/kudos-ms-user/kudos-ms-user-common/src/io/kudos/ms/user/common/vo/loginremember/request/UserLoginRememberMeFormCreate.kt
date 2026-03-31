package io.kudos.ms.user.common.vo.loginremember.request

import java.time.LocalDateTime


/**
 * 记住我登录表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserLoginRememberMeFormCreate (

    override val username: String? = null,

    override val token: String? = null,

    override val lastUsed: LocalDateTime? = null,

) : IUserLoginRememberMeFormBase
