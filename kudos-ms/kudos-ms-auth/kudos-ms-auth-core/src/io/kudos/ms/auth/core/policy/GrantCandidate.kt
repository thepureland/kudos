package io.kudos.ms.auth.core.policy

import java.time.LocalDateTime


/**
 * One admissibility question: *may [roleId] be added to [principalId]?*
 *
 * Every write that widens somebody's effective permissions reduces to a set of these, whatever
 * shape it takes at the API surface:
 *
 * | write | reduces to |
 * |---|---|
 * | bind role R to users U… | one candidate per user |
 * | add users U… to group G | one candidate per (user, role held by G) |
 * | bind role R to group G | one candidate per current member of G |
 *
 * Reducing them to a common form is the whole point: the constraints then live in one place
 * instead of being re-implemented — and forgotten — per write path.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class GrantCandidate(

    /** The role that would be added. */
    val roleId: String,

    /** The principal that would receive it. */
    val principalId: String,

    /**
     * What kind of principal that is — `USER`, or whatever kind a deployment registered an
     * [io.kudos.ms.auth.core.principal.spi.IPrincipalDirectory] for.
     *
     * Defaults to USER because that is what every existing write path means, and null would make
     * the directory guess. A caller granting to a service account states it, so the lookup goes to
     * the directory that actually owns that principal rather than to whichever one answers first.
     */
    val principalType: String = "USER",

    /**
     * How the principal would receive it — used only for the rejection message, so an admin can
     * tell "blocked when adding them to group X" from "blocked when granting the role directly".
     */
    val via: Via = Via.DIRECT,

    /**
     * Who is handing the role out, when the write is somebody exercising delegated authority.
     *
     * NULL means the write is not a delegation — a platform bootstrap, a migration, an approved
     * grant request being applied by the workflow. Those skip the delegation screening entirely,
     * which is why it must be an explicit null rather than a defaulted "system" id that could be
     * mistaken for a real grantor in the chain.
     */
    val operatorId: String? = null,

    /**
     * How many further hops the new grant should be allowed to travel. Only meaningful together
     * with [operatorId]; must narrow strictly against what the operator holds.
     */
    val requestedDepth: Int = 0,

    /** Expiry proposed for the new grant; NULL = never expires. */
    val endTime: LocalDateTime? = null,

    /**
     * Set **only** by the approval workflow when it applies a decision that has already been made.
     *
     * A role marked `approval_required` may not be handed out by a direct write; the point of the
     * flag is that a second person signs off. The workflow is the one path that has satisfied that
     * requirement, so it says so explicitly here rather than the policy layer trying to infer it
     * from the call stack — an inference that would silently break the first time somebody wrapped
     * the call.
     */
    val approvalSatisfied: Boolean = false,
) {

    enum class Via { DIRECT, GROUP_MEMBERSHIP, GROUP_ROLE_BINDING }
}


/**
 * A rejected [GrantCandidate] plus the reason, in a form an admin UI can render per row.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class GrantRejection(
    val candidate: GrantCandidate,
    val reason: String,
) {
    override fun toString(): String =
        "role ${candidate.roleId} → principal ${candidate.principalId} (${candidate.via}): ${reason}"
}
