package io.kudos.ms.auth.common.exclusion.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.auth.common.exclusion.vo.response.AuthRoleExclusionRow

/**
 * Query criteria for SoD exclusion list.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuthRoleExclusionQuery(

    /** Filter by tenant. */
    val tenantId: String? = null,

    /** Filter rows that involve this role on either side. */
    val roleId: String? = null,

) : ListSearchPayload() {
    override fun getReturnEntityClass() = AuthRoleExclusionRow::class
}
