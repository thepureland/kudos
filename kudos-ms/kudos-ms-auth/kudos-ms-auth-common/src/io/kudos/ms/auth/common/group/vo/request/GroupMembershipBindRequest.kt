package io.kudos.ms.auth.common.group.vo.request

import java.io.Serializable
import java.time.LocalDateTime


/**
 * Add a member to a group with an explicit validity window.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class GroupMembershipBindRequest(

    /** The group. */
    val groupId: String,

    /** The member. */
    val userId: String,

    /** Effective from; NULL = immediately. */
    val startTime: LocalDateTime? = null,

    /** Effective until; NULL = never expires. */
    val endTime: LocalDateTime? = null,

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
