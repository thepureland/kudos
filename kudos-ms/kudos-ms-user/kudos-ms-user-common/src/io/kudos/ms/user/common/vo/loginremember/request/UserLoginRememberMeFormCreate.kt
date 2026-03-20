package io.kudos.ms.user.common.vo.loginremember.request

import java.time.LocalDateTime


/**
 * 记住我登录表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserLoginRememberMeFormCreate (

    /** 用户名 */
    val username: String? = null,

    /** 令牌 */
    val token: String? = null,

    /** 最后使用时间 */
    val lastUsed: LocalDateTime? = null,

)
