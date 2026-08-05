package io.kudos.ms.auth.common.authz.vo.response

import io.kudos.ms.auth.common.authz.enums.PermissionEffect
import io.kudos.ms.auth.common.authz.vo.AuthzDecision
import java.io.Serializable


/**
 * Why a subject does — or does not — hold a permission.
 *
 * The question an operator actually asks is never "does user X have Y" (the UI already shows that);
 * it is "**why**". Without an answer, diagnosing a permission problem means reading the role tree,
 * the group memberships and the bindings by hand and hoping to reconstruct what the decision point
 * did. This returns the decision point's own evidence instead of a reconstruction of it.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class PermissionExplanationVo(

    /** The subject asked about. */
    val userId: String,

    /** The permission code asked about. */
    val permissionCode: String,

    /** The verdict, with the rule that settled it. */
    val decision: AuthzDecision,

    /**
     * Every binding of the subject that addresses this code, whether or not it decided the outcome.
     *
     * A denial is usually explained by what is *missing*, so an empty list is itself the answer:
     * nothing the subject holds speaks to this permission at all.
     */
    val relevantGrants: List<GrantEvidence>,

) : Serializable {

    /** One binding, traced back to where it came from. */
    data class GrantEvidence(

        /** The code as written on the binding — a wildcard stays a wildcard. */
        val permissionCode: String,

        val effect: PermissionEffect,

        /** The role carrying the binding. */
        val roleId: String?,

        /** That role's code and name, so the answer is readable without a second lookup. */
        val roleCode: String?,
        val roleName: String?,

        /** How the subject reaches that role: DIRECT, GROUP, or INHERITED (via the parent chain). */
        val source: String,

        /** The condition attached, if any; a binding that is gated says so. */
        val condition: String?,

    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
