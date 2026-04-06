package io.kudos.ms.user.common.account.vo.request
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.user.common.account.vo.response.UserAccountProtectionRow


/**
 * 用户账号保护列表查询条件请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountProtectionQuery (

    /** 用户ID */
    val userId: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = UserAccountProtectionRow::class

}