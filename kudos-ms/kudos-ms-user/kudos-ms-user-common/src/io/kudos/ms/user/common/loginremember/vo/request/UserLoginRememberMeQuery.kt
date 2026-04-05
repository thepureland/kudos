package io.kudos.ms.user.common.loginremember.vo.request
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.user.common.loginremember.vo.response.UserLoginRememberMeRow


/**
 * 记住我登录列表查询条件请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserLoginRememberMeQuery (

    /** 用户名 */
    val username: String? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = UserLoginRememberMeRow::class

}