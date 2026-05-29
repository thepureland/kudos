package io.kudos.ms.auth.common.role.vo.request

import java.io.Serializable

/**
 * Request body for role↔users batch bind.
 *
 * Sent as a single JSON object so both lists travel in the body — the alternative (one list in
 * the query string and one in the body) is awkward for the HTTP client wrappers used in the
 * console UI and would limit length.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuthRoleBatchBindUsersRequest(
    /** Role ids to bind into. */
    val roleIds: List<String>,
    /** User ids that should be bound to **every** role in [roleIds]. */
    val userIds: List<String>,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
