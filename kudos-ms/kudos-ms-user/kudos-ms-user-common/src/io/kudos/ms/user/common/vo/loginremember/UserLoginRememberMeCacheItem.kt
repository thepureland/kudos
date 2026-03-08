package io.kudos.ms.user.common.vo.loginremember

import io.kudos.base.support.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 记住我登录缓存项
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class UserLoginRememberMeCacheItem (

    /** 主键 */
    override val id: String = "",

    //region your codes 1

    /** 用户名 */
    val username: String? = null,

    /** 令牌 */
    val token: String? = null,

    /** 最后使用时间 */
    val lastUsed: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IIdEntity<String>, Serializable {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    //endregion your codes 3

    companion object {
        private const val serialVersionUID = 1L
    }

}
