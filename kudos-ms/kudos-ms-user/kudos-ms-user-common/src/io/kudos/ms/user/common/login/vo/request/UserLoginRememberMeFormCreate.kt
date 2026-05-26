package io.kudos.ms.user.common.login.vo.request

import java.time.LocalDateTime


/**
 * Remember-me login form create request VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserLoginRememberMeFormCreate (

    override val username: String? ,

    override val token: String? ,

    override val lastUsed: LocalDateTime? ,

) : IUserLoginRememberMeFormBase
