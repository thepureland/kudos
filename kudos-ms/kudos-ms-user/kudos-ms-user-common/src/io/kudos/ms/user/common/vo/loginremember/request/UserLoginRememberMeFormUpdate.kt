package io.kudos.ms.user.common.vo.loginremember.request

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 记住我登录表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserLoginRememberMeFormUpdate (

    /** 主键 */
    override val id: String? = null,

    /** 用户名 */
    val username: String? = null,

    /** 令牌 */
    val token: String? = null,

    /** 最后使用时间 */
    val lastUsed: LocalDateTime? = null,

) : IIdEntity<String?>
