package io.kudos.ms.user.common.loginremember.vo.response
import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 记住我登录详情响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserLoginRememberMeDetail (

    /** 主键 */
    override val id: String = "",

    /** 用户名 */
    val username: String? = null,

    /** 令牌 */
    val token: String? = null,

    /** 最后使用时间 */
    val lastUsed: LocalDateTime? = null,

) : IIdEntity<String>