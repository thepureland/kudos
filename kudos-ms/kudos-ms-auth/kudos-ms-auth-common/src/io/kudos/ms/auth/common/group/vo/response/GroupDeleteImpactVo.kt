package io.kudos.ms.auth.common.group.vo.response

import java.io.Serializable

/**
 * Aggregate delete-impact summary for a batch of user groups.
 *
 * Counts are over the union of bindings — i.e. if two groups both contain user U, U is counted
 * once in [users]. Same de-dup rule for [roles].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class GroupDeleteImpactVo(
    /** Distinct users currently belonging to any group in the request. */
    val users: Int,
    /** Distinct roles currently granted by any group in the request. */
    val roles: Int,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L

        @JvmStatic
        fun zero(): GroupDeleteImpactVo = GroupDeleteImpactVo(users = 0, roles = 0)
    }
}
