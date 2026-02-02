package io.kudos.ms.user.common.vo.loginremember

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 记住我登录详情
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class UserLoginRememberMeDetail (

    //region your codes 1

    /** 用户名 */
    var username: String? = null,

    /** 令牌 */
    var token: String? = null,

    /** 最后使用时间 */
    var lastUsed: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    //endregion your codes 3

}
