package io.kudos.ms.auth.common.group.vo.response

import java.io.Serializable
import java.time.LocalDateTime


/**
 * One membership row, with the validity window that makes it temporary.
 *
 * Membership has to be able to express everything a direct grant can, a window included — otherwise
 * "put them in the group instead" becomes the way to get around the expiry somebody set.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class GroupMembershipVo(

    /** Membership row id. */
    val id: String,

    val groupId: String,

    val userId: String,

    /** Display name of the member, resolved for the admin list. */
    val userName: String? = null,

    /** Effective from; NULL = immediately. */
    val startTime: LocalDateTime? = null,

    /** Effective until; NULL = never expires. */
    val endTime: LocalDateTime? = null,

    /** Whether the window contains "now" — precomputed so the UI does not re-derive the rule. */
    val active: Boolean = true,

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
