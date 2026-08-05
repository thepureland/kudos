package io.kudos.ms.auth.core.policy.iservice

import io.kudos.ms.auth.core.policy.GrantCandidate
import io.kudos.ms.auth.core.policy.GrantRejection


/**
 * The single admission gate for every write that widens somebody's effective permissions.
 *
 * **Why one service.** There are five such writes (role→principal, principal→group, role→group,
 * role→permission, role re-parent). Before this service each enforced whatever its author
 * remembered — SoD was checked on exactly one of them, which is precisely why adding a user to a
 * group was the way to walk around a separation-of-duties rule. Constraints that live in five
 * places are constraints that hold in one.
 *
 * **What it does not do.** It answers "is this grant admissible?", never "who is asking?" — the
 * delegation rules (may the operator hand this out at all, and how far onwards) land here later as
 * additional screening steps, on top of the same [GrantCandidate] reduction.
 *
 * **Reporting, not deciding.** Screening returns rejections rather than throwing, because the right
 * reaction differs per write: a direct role bind rejects the whole call, while a broadcast bind to a
 * 500-member group must report per-member and let the admissible members through. Callers that want
 * all-or-nothing call [assertNoRejection].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IAuthGrantPolicyService {

    /**
     * Screens grant candidates against every invariant that guards the effective-permission set:
     * the role and principal must exist, must belong to the same tenant, and the resulting
     * effective role set must not breach a separation-of-duties rule.
     *
     * Candidates the principal already satisfies are **not** filtered out here — callers deduplicate
     * before screening, since "already holds it" is a no-op rather than a policy decision.
     *
     * @param candidates the (role, principal) pairs to admit
     * @return one entry per rejected candidate; empty when everything is admissible
     */
    fun screenGrants(candidates: Collection<GrantCandidate>): List<GrantRejection>

    /**
     * Screens a role→permission binding: the addressed permissions must exist and belong to the
     * role's tenant/subsystem, and each binding must address its permission exactly one way
     * (a resource id **or** a permission code, never neither).
     *
     * @param roleId the role gaining the permissions
     * @param resourceIds resource-id-addressed bindings
     * @param permissionCodes code-addressed bindings
     * @return human-readable reasons, one per rejected binding; empty when all are admissible
     */
    fun screenPermissionBindings(
        roleId: String,
        resourceIds: Collection<String> = emptyList(),
        permissionCodes: Collection<String> = emptyList(),
    ): List<String>

    /**
     * Screens a role re-parent. Moving a role in the tree silently rewrites the effective permission
     * set of **every holder of that role and of its descendants**, so it can introduce a
     * separation-of-duties breach that no bind-time check would ever see — SoD is otherwise only
     * evaluated when a grant is written.
     *
     * Structural validation (cycles, tenant, subsystem) stays in the role service; this is the
     * policy half.
     *
     * @param roleId the role being moved
     * @param newParentId its new parent; NULL detaches it to a root, which never widens anything
     * @return rejections naming the affected holders; empty when the move is admissible
     */
    fun screenReparent(roleId: String, newParentId: String?): List<GrantRejection>

    /**
     * Throws [IllegalArgumentException] listing every rejection, or returns quietly when there is
     * none. For write paths whose semantics are all-or-nothing.
     */
    fun assertNoRejection(rejections: List<GrantRejection>) {
        if (rejections.isEmpty()) return
        throw IllegalArgumentException(
            "Grant policy rejected ${rejections.size} assignment(s):\n" +
                rejections.joinToString("\n") { "  - ${it}" },
        )
    }
}
