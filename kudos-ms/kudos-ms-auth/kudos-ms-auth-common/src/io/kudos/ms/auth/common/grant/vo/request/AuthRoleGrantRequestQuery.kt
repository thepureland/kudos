package io.kudos.ms.auth.common.grant.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.auth.common.grant.vo.response.AuthRoleGrantRequestRow

/**
 * Query criteria for the grant-request list (e.g. the approver dashboard).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuthRoleGrantRequestQuery(

    /** Filter by tenant. */
    val tenantId: String? = null,

    /** Filter by status (PENDING / APPROVED / REJECTED / CANCELLED). */
    val status: String? = null,

    /** Filter by the user the role would be granted to. */
    val userId: String? = null,

    /** Filter by the role being requested. */
    val roleId: String? = null,

    /** Filter by who submitted the request. */
    val requesterId: String? = null,

) : ListSearchPayload() {
    override fun getReturnEntityClass() = AuthRoleGrantRequestRow::class
}
