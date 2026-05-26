package io.kudos.ms.user.common.account.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.user.common.account.vo.response.UserAccountProtectionRow


/**
 * User account protection list query request VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountProtectionQuery (

    /** User ID */
    val userId: String? = null,

    /** Whether active */
    val active: Boolean? = null,

    /** Whether built-in */
    val builtIn: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = UserAccountProtectionRow::class

}