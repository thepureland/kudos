package io.kudos.ms.user.common.loginremember.vo
import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 记住我登录缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class UserLoginRememberMeCacheEntry (

    /** 主键 */
    override val id: String,

    /** 用户名 */
    val username: String?,

    /** 令牌 */
    val token: String?,

    /** 最后使用时间 */
    val lastUsed: LocalDateTime?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

}
