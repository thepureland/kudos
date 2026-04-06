package io.kudos.ms.user.common.login.vo.request
import java.time.LocalDateTime


/**
 * 记住我登录表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserLoginRememberMeFormCreate (

    override val username: String? ,

    override val token: String? ,

    override val lastUsed: LocalDateTime? ,

) : IUserLoginRememberMeFormBase
