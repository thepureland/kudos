package io.kudos.ms.user.common.account.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.user.common.account.vo.response.UserAccountRow


/**
 * User account list query request VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountQuery (

    /** Username */
    val username: String? = null,

    /** Tenant id */
    val tenantId: String? = null,

    /** Account type dictionary code */
    val accountTypeDictCode: String? = null,

    /** Account status dictionary code */
    val accountStatusDictCode: String? = null,

    /** Organization id */
    val orgId: String? = null,

    /** Supervisor id */
    val supervisorId: String? = null,

    /** Remark */
    val remark: String? = null,

    /** Whether active */
    val active: Boolean? = null,

    /** Whether built-in */
    val builtIn: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = UserAccountRow::class

}