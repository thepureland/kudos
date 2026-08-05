package io.kudos.ms.auth.common.delegation.vo

import java.io.Serializable
import java.time.LocalDateTime


/**
 * Request to hand a role to principals as a **delegated** grant.
 *
 * Distinct from a plain bind because it carries the two things a delegation needs and a bind does
 * not: how far the new grant may travel onwards, and when it must end.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class RoleGrantRequest(

    /** The role to hand out. */
    val roleId: String,

    /** The principals receiving it. */
    val userIds: List<String>,

    /**
     * How many further hops the new grant may travel; 0 = the recipients may not re-delegate.
     * Must narrow strictly against the operator's own allowance.
     */
    val delegableDepth: Int = 0,

    /** When the grant becomes effective; NULL = immediately. */
    val startTime: LocalDateTime? = null,

    /** When it expires; NULL = never, but never beyond the operator's own grant. */
    val endTime: LocalDateTime? = null,

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}


/**
 * What revoking a grant would take away, before it is done.
 *
 * Cascading revocation is correct but its blast radius is invisible from the grant being revoked —
 * one row may sit above a whole subtree of onward delegations. Showing this first is what keeps
 * "revoke one person's access" from silently becoming "revoke forty people's access".
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class RevokeImpactVo(

    /** The grant that would be revoked. */
    val grantId: String,

    /** Role and principal of that grant. */
    val roleId: String,
    val userId: String,

    /** Grants delegated downstream from it, which would be revoked as well. */
    val cascadedGrants: List<CascadedGrant>,

    /** Distinct principals losing something, the direct holder included. */
    val affectedUserCount: Int,

) : Serializable {

    data class CascadedGrant(
        val grantId: String,
        val roleId: String,
        val userId: String,
        /** How many delegation hops below the revoked grant this one sits. */
        val depth: Int,
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    companion object {
        private const val serialVersionUID = 1L

        @JvmStatic
        fun none(grantId: String) = RevokeImpactVo(grantId, "", "", emptyList(), 0)
    }
}
