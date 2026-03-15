package io.kudos.ms.user.common.vo.loginremember

import io.kudos.base.support.payload.ListSearchPayload


/**
 * 记住我登录查询条件载体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class UserLoginRememberMeQuery (


    /** 用户名 */
    val username: String? = null,

) : ListSearchPayload() {


    constructor() : this("")

    override fun getReturnEntityClass() = UserLoginRememberMeRow::class


}
