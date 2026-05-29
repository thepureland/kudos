package io.kudos.ms.auth.common.group.vo.request

import java.io.Serializable

/**
 * Request body for group↔users batch bind. Mirrors [io.kudos.ms.auth.common.role.vo.request.AuthRoleBatchBindUsersRequest]
 * for groups.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuthGroupBatchBindUsersRequest(
    /** Group ids to bind into. */
    val groupIds: List<String>,
    /** User ids that should be bound to **every** group in [groupIds]. */
    val userIds: List<String>,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
