package io.kudos.ms.user.common.login.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.user.common.login.vo.response.UserLoginRememberMeRow


/**
 * Remember-me login list query request VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserLoginRememberMeQuery (

    /** Username */
    val username: String? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = UserLoginRememberMeRow::class

}