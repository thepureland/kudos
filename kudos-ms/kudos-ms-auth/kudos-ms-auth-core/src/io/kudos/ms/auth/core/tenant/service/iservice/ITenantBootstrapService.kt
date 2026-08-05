package io.kudos.ms.auth.core.tenant.service.iservice

import io.kudos.ms.auth.common.tenant.vo.TenantBootstrapResult
import io.kudos.ms.auth.common.tenant.vo.TenantBootstrapStatusVo


/**
 * Gets a brand-new tenant to the point where it can administer itself.
 *
 * The bootstrap problem, stated plainly: permissions come from roles, roles are administered by
 * somebody holding a role, and a new tenant has neither. Somebody has to break the cycle from
 * outside it. Doing that in the framework — once, auditably — is the alternative to every deployment
 * breaking it with a hand-written INSERT or a shared platform account.
 *
 * **Two steps, deliberately not one.**
 *  1. [seedRoles] creates the tenant's built-in roles. Derivable from configuration, idempotent,
 *     and grants nobody anything — so it is safe to run automatically when the tenant appears.
 *  2. [bindFirstAdministrator] hands one of those roles to a person. That confers real access and is
 *     a decision, not a derivation, so it stays an explicit act with an audit record.
 *
 * Folding them together would make "a tenant was created" silently mean "somebody now has
 * administrative access to it", which is exactly the kind of implicit grant this module exists to
 * eliminate.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface ITenantBootstrapService {

    /**
     * Creates the configured built-in roles for [tenantId], with their permission bindings.
     *
     * Idempotent by role code: a role that already exists is left exactly as it is, including any
     * bindings an administrator has since edited. Re-running must never undo local changes, or a
     * redeploy would quietly reset every tenant's customisation.
     *
     * @param tenantId the tenant
     * @return what was created and what was already there
     */
    fun seedRoles(tenantId: String): TenantBootstrapResult

    /**
     * Binds [userId] to the tenant's bootstrap role — the tenant's first administrator.
     *
     * Screened by the ordinary policy layer (tenant match, existence, SoD) but carrying no operator,
     * because there is by definition nobody in the tenant with the authority to delegate this. That
     * is the one privilege of this call, and the reason it is a separate, audited operation rather
     * than a side effect of creating the tenant.
     *
     * Refuses when the tenant already has an administrator, unless [force] is set: "first
     * administrator" is a one-time act, and an endpoint that silently appoints a second one is an
     * escalation path.
     *
     * @param tenantId the tenant
     * @param userId the person to appoint
     * @param roleCode which bootstrap role; defaults to the first configured one
     * @param force appoint even though the role already has holders
     * @return the result, naming the role bound
     * @throws IllegalArgumentException when the tenant is not seeded, the user is inadmissible, or
     *   the role already has holders and [force] was not set
     */
    fun bindFirstAdministrator(
        tenantId: String,
        userId: String,
        roleCode: String? = null,
        force: Boolean = false,
    ): TenantBootstrapResult

    /**
     * Where the tenant is in the bootstrap: seeded or not, and who administers it.
     *
     * Both facts in one answer because a console needs them together to know which of the two steps
     * the operator is looking at — and because deriving "not seeded" from a failed role lookup makes
     * a UI guess at the difference between "nothing here yet" and "something went wrong".
     *
     * @param tenantId the tenant
     * @return the current state
     */
    fun status(tenantId: String): TenantBootstrapStatusVo
}
