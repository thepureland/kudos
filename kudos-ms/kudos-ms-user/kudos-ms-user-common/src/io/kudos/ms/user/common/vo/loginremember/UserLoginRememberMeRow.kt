package io.kudos.ms.user.common.vo.loginremember

import io.kudos.base.model.contract.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 记住我登录记录
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class UserLoginRememberMeRow (


    /** 用户名 */
    val username: String? = null,

    /** 令牌 */
    val token: String? = null,

    /** 最后使用时间 */
    val lastUsed: LocalDateTime? = null,

) : IdJsonResult<String>() {


    constructor() : this("")


}
