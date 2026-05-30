package io.kudos.ms.auth.common.temporal.vo.request

import java.io.Serializable
import java.time.LocalDateTime

/**
 * Request body to grant a role to a user with an optional validity window (时效性授权).
 *
 * Replace semantics: any existing grant for the same (roleId, userId) is superseded, so the
 * supplied window becomes authoritative. Both bounds are optional:
 *   - startTime NULL ⇒ effective immediately
 *   - endTime   NULL ⇒ never expires
 * If both are set, startTime must not be after endTime.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuthRoleUserTemporalBindRequest(

    /** Role to grant. */
    val roleId: String,

    /** User to grant it to. */
    val userId: String,

    /** Grant effective time; NULL = immediately. */
    val startTime: LocalDateTime? = null,

    /** Grant expiry time; NULL = never. */
    val endTime: LocalDateTime? = null,

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
