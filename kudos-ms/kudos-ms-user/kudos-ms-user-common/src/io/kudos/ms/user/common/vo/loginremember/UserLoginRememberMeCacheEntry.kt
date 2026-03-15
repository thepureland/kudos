package io.kudos.ms.user.common.vo.loginremember

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 记住我登录缓存项
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class UserLoginRememberMeCacheEntry (

    /** 主键 */
    override val id: String = "",


    /** 用户名 */
    val username: String? = null,

    /** 令牌 */
    val token: String? = null,

    /** 最后使用时间 */
    val lastUsed: LocalDateTime? = null,

) : IIdEntity<String>, Serializable {


    constructor() : this("")


    companion object {
        private const val serialVersionUID = 1L
    }

}
