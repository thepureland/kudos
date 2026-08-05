package io.kudos.ms.auth.common.authz.api

import io.kudos.ms.auth.common.authz.vo.AuthzDecision
import io.kudos.ms.auth.common.authz.vo.AuthzRequest
import io.kudos.ms.auth.common.authz.vo.PermissionGrantVo
import io.kudos.ms.auth.common.authz.vo.SubjectRef


/**
 * **The** authorization decision point. Everything that enforces permissions asks this and nothing
 * else — URL filters, method annotations, UI rendering, row-level filtering.
 *
 * That exclusivity is the contract's real content. Roles, groups, inheritance and scopes are ways of
 * *managing* the subject→permission mapping at scale; they are not what authorization is. Keeping
 * the decision behind one interface means the management model can be replaced, extended or
 * layered — and every enforcement point inherits the change for free, because none of them knows
 * how the answer was reached.
 *
 * The default implementation is RBAC over kudos' own tables. Applications may decorate it (add an
 * emergency kill-switch, an allow-list for a migration, an external policy source) or replace it
 * outright; nothing downstream needs to know.
 *
 * **Performance contract**: [decide] is on every request path and must resolve from cache, never
 * from the database.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IAuthzDecisionApi {

    /**
     * Answers one authorization question, with the evidence that settled it.
     *
     * The evaluation order is fixed and closed:
     *  1. a platform-level administrator short-circuits to PERMIT (still audited);
     *  2. collect the subject's resolved bindings;
     *  3. drop bindings whose condition does not hold in this request's context;
     *  4. any matching DENY refuses; otherwise any matching ALLOW permits; otherwise **default deny**.
     *
     * @param request who is asking, what for, and in what context
     * @return the decision plus which rule produced it
     */
    fun decide(request: AuthzRequest): AuthzDecision

    /**
     * Every permission code the subject holds an ALLOW for, minus anything a DENY withholds.
     *
     * For rendering — menus, buttons, a client-side capability list — where the caller wants the
     * whole set in one trip rather than N [decide] calls. Conditional bindings are included when
     * their condition holds in the supplied [context]; a UI that renders without context sees
     * unconditional grants only.
     *
     * Wildcards are returned as-is (`sys:user:*`), not expanded: the set of concrete codes under a
     * wildcard is open-ended, and expanding it would silently go stale.
     *
     * @param subject the principal
     * @param context attributes for condition evaluation
     * @return the granted codes, wildcards included
     */
    fun permissionCodes(subject: SubjectRef, context: Map<String, Any?> = emptyMap()): Set<String>

    /**
     * The subject's resolved bindings, unmerged — ALLOW and DENY, conditions intact.
     *
     * For the "explain" view: it shows *why* the answer is what it is, which the merged set of
     * [permissionCodes] can no longer express.
     *
     * @param subject the principal
     * @return every binding reachable through the subject's effective roles
     */
    fun resolveGrants(subject: SubjectRef): List<PermissionGrantVo>
}
