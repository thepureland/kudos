package io.kudos.ms.auth.common.temporal.vo.response

import java.io.Serializable
import java.time.LocalDateTime

/**
 * A single temporal role-user grant row for the admin view.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class RoleTemporalGrantRow(

    /** Grant record id (used to identify the row for deletion/replacement). */
    val id: String,

    /** The user the role is granted to. */
    val userId: String,

    /** Grant effective time; NULL = effective immediately. */
    val startTime: LocalDateTime? = null,

    /** Grant expiry time; NULL = never expires. */
    val endTime: LocalDateTime? = null,

    /** Whether the grant is active right now (start ≤ now ≤ end, NULL bounds open). */
    val active: Boolean,

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
