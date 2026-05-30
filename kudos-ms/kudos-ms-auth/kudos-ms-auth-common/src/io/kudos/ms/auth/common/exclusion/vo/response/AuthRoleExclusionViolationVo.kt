package io.kudos.ms.auth.common.exclusion.vo.response

import java.io.Serializable

/**
 * Describes one SoD violation: a user who currently holds both roles in a mutually
 * exclusive pair. Returned by findViolatingUserIds and the admin "scan for violations" endpoint.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuthRoleExclusionViolationVo(

    /** The exclusion rule that is violated. */
    val exclusionId: String,

    /** Role A side. */
    val roleAId: String,

    /** Role B side. */
    val roleBId: String,

    /**
     * IDs of users who simultaneously hold both roles (via any path: direct, group, or
     * parent-chain inheritance). These users are in violation and should be reviewed by an admin.
     */
    val violatingUserIds: List<String>,

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
