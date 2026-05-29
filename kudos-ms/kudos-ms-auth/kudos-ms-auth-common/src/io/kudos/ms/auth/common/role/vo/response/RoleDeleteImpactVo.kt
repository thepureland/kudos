package io.kudos.ms.auth.common.role.vo.response

import java.io.Serializable

/**
 * Aggregate delete-impact summary for a batch of roles.
 *
 * Counts are over the union of bindings — i.e. if two roles both grant user U, U is counted
 * once in [users]. Same de-dup rule for [groups].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class RoleDeleteImpactVo(
    /** Distinct users currently bound to any role in the request. */
    val users: Int,
    /** Distinct groups currently bound to any role in the request. */
    val groups: Int,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L

        @JvmStatic
        fun zero(): RoleDeleteImpactVo = RoleDeleteImpactVo(users = 0, groups = 0)
    }
}
