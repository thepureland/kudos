package io.kudos.ms.auth.core.platform.authz

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.authz.api.IAuthzDecisionApi
import io.kudos.ms.auth.common.authz.enums.PermissionEffect
import io.kudos.ms.auth.common.authz.support.PermissionCodes
import io.kudos.ms.auth.common.authz.vo.AuthzDecision
import io.kudos.ms.auth.common.authz.vo.AuthzRequest
import io.kudos.ms.auth.common.authz.vo.PermissionGrantVo
import io.kudos.ms.auth.common.authz.vo.SubjectRef
import io.kudos.ms.auth.core.instance.dao.AuthInstanceGrantDao
import io.kudos.ms.auth.core.instance.model.po.AuthInstanceGrant
import io.kudos.ms.auth.core.platform.authz.cache.PermissionGrantsByUserIdCache
import io.kudos.ms.auth.core.platform.authz.condition.IConditionEvaluator
import io.kudos.ms.auth.core.platform.authz.audit.AuthzDecisionAuditRecorder
import io.kudos.ms.auth.core.platform.authz.init.properties.AuthzProperties
import io.kudos.ms.auth.core.principal.PrincipalDirectoryRegistry
import io.kudos.ms.auth.core.platform.authz.spi.AuthorizationPolicyContribution
import io.kudos.ms.auth.core.platform.authz.spi.AuthorizationPolicyExtensionEvaluator
import jakarta.annotation.Resource
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * The RBAC decision point — the default implementation of [IAuthzDecisionApi].
 *
 * The evaluation order in [decide] is fixed and closed; nothing else in the module may re-implement
 * a piece of it. Two of the four steps are there for reasons worth stating:
 *
 * **The platform-admin short circuit is explicit.** Every real deployment needs a subject that can
 * act when the permission data itself is broken. If the framework does not define that subject, each
 * application invents its own — as a hardcoded id check, an "if username == admin", a bypass flag on
 * a filter — and none of those are auditable. Here it is one configured set of role codes
 * ([AuthzProperties.platformAdminRoleCodes]), it goes through the same call, and it produces a
 * decision object with [AuthzDecision.Reason.PLATFORM_ADMIN] that the audit trail records like any
 * other. A match is accepted only for a deployment-configured platform tenant, a protected
 * built-in role, and an explicit zero delegation ceiling.
 *
 * **Default deny is the last step, not an error path.** A permission code nobody granted, a code
 * nobody registered, a subject with no roles — all reach the same answer through the same route.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
@Primary
@Service
open class AuthzDecisionApi : IAuthzDecisionApi {

    @Resource
    private lateinit var permissionGrantsByUserIdCache: PermissionGrantsByUserIdCache

    @Resource
    private lateinit var authInstanceGrantDao: AuthInstanceGrantDao

    @Resource
    private lateinit var conditionEvaluator: IConditionEvaluator

    @Resource
    private lateinit var platformAdministratorPolicy: PlatformAdministratorPolicy

    @Resource
    private lateinit var decisionAuditRecorder: AuthzDecisionAuditRecorder

    @Resource
    private lateinit var policyExtensionEvaluator: AuthorizationPolicyExtensionEvaluator

    @Resource
    private lateinit var principalDirectoryRegistry: PrincipalDirectoryRegistry

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun decide(request: AuthzRequest): AuthzDecision =
        decideInternal(request).also { decisionAuditRecorder.record(request, it) }

    private fun decideInternal(request: AuthzRequest): AuthzDecision {
        if (request.permissionCode.isBlank()) {
            return AuthzDecision.deny(
                AuthzDecision.Reason.DENIED_BY_DEFAULT,
                detail = "the request carried no permission code",
            )
        }

        // 1. Platform administrator short circuit.
        if (platformAdministratorPolicy.isPlatformAdministrator(request.subject)) {
            return AuthzDecision.permit(
                AuthzDecision.Reason.PLATFORM_ADMIN,
                code = request.permissionCode,
                detail = "subject holds a platform administrator role",
            )
        }

        // 2. Collect, then 3. drop bindings whose condition does not hold in this context. A
        //    condition judged false makes the binding *absent*, not denying; one that cannot be
        //    judged at all is asymmetric by effect — see applies() below.
        val applicable = resolveGrants(request.subject)
            .filter { it.applies(request.context) }
            .filter { PermissionCodes.matches(it.permissionCode, request.permissionCode) }

        // Instance-level grants join the same collection when the question names a particular thing.
        val instanceGrants = collectInstanceGrants(request)

        // 4. DENY wins, then ALLOW, then default deny. Instance grants are merged by the same rule
        //    rather than layered on top: a share that could override a DENY would make "revoke this
        //    person's access" a statement nobody could rely on.
        val deny = applicable.firstOrNull { it.effect == PermissionEffect.DENY }
        if (deny != null) {
            return AuthzDecision.deny(
                AuthzDecision.Reason.DENIED_BY_RULE,
                code = deny.permissionCode,
                roleId = deny.roleId,
                detail = "denied by an explicit DENY binding, which no ALLOW can override",
            )
        }
        val instanceDeny = instanceGrants.firstOrNull { PermissionEffect.fromCode(it.effect) == PermissionEffect.DENY }
        if (instanceDeny != null) {
            return AuthzDecision.deny(
                AuthzDecision.Reason.DENIED_BY_RULE,
                code = instanceDeny.action,
                detail = "denied by an explicit DENY on ${instanceDeny.resourceType}#${instanceDeny.instanceId}",
            )
        }
        // Extensions run only after the common model found no explicit denial. This keeps the hot
        // path cheap and avoids loading deployment-specific facts for a request already settled.
        val extensionResults = policyExtensionEvaluator.evaluate(request)
        val extensionDeny = extensionResults.firstOrNull {
            it.effect == AuthorizationPolicyContribution.Effect.DENY
        }
        if (extensionDeny != null) {
            return AuthzDecision.deny(
                AuthzDecision.Reason.DENIED_BY_EXTENSION,
                code = extensionDeny.policyCode,
                detail = extensionDeny.detail ?: "denied by an authorization policy extension",
            )
        }
        val allow = applicable.firstOrNull { it.effect == PermissionEffect.ALLOW }
        if (allow != null) {
            return AuthzDecision.permit(
                AuthzDecision.Reason.ALLOWED_BY_GRANT,
                code = allow.permissionCode,
                roleId = allow.roleId,
            )
        }
        val instanceAllow = instanceGrants.firstOrNull { PermissionEffect.fromCode(it.effect) == PermissionEffect.ALLOW }
        if (instanceAllow != null) {
            return AuthzDecision.permit(
                AuthzDecision.Reason.ALLOWED_BY_INSTANCE_GRANT,
                code = instanceAllow.action,
                detail = "granted on ${instanceAllow.resourceType}#${instanceAllow.instanceId} by ${instanceAllow.grantedBy ?: "unknown"}",
            )
        }
        val extensionAllow = extensionResults.firstOrNull {
            it.effect == AuthorizationPolicyContribution.Effect.ALLOW
        }
        if (extensionAllow != null) {
            return AuthzDecision.permit(
                AuthzDecision.Reason.ALLOWED_BY_EXTENSION,
                code = extensionAllow.policyCode,
                detail = extensionAllow.detail ?: "allowed by an authorization policy extension",
            )
        }
        return AuthzDecision.deny(
            AuthzDecision.Reason.DENIED_BY_DEFAULT,
            code = request.permissionCode,
            detail = "no binding of the subject matches this permission code",
        )
    }

    @Transactional(readOnly = true)
    override fun permissionCodes(subject: SubjectRef, context: Map<String, Any?>): Set<String> {
        val applicable = resolveGrants(subject).filter { it.applies(context) }
        val denied = applicable.filter { it.effect == PermissionEffect.DENY }.map { it.permissionCode }
        return applicable.asSequence()
            .filter { it.effect == PermissionEffect.ALLOW }
            .map { it.permissionCode }
            // A DENY removes an ALLOW it covers. The reverse is not possible: an ALLOW never
            // narrows a DENY, so this single pass is the whole merge.
            .filterNot { code -> denied.any { PermissionCodes.matches(it, code) } }
            .toSet()
    }

    @Transactional(readOnly = true)
    override fun resolveGrants(subject: SubjectRef): List<PermissionGrantVo> =
        permissionGrantsByUserIdCache.getGrants(subject.principalId)

    /**
     * The subject's grants on the particular instance the request names, matched by the same
     * wildcard rules as everything else — so a share may say `doc:read` or `*`, and nobody has to
     * learn a second matching rule.
     *
     * **Cost note.** This is the one part of the decision path that reads the database rather than a
     * cache, and only when the caller names an instance. That is inherent: the alternative is
     * keeping every share of every principal in memory, which trades a bounded per-request read for
     * an unbounded resident set. Requests that do not name an instance never take this branch.
     */
    private fun collectInstanceGrants(request: AuthzRequest): List<AuthInstanceGrant> {
        val resourceType = request.resourceType?.takeIf { it.isNotBlank() } ?: return emptyList()
        val instanceId = request.instanceId?.takeIf { it.isNotBlank() } ?: return emptyList()
        // The subject's real tenant, so a grant row cannot reach across the boundary even if one
        // was written before the share path validated it (or straight into the database). Defence in
        // depth: the write path is where this is properly enforced, but the tenant boundary is the
        // one invariant worth checking on both sides.
        val subjectTenant = principalDirectoryRegistry
            .find(request.subject.principalId, request.subject.principalType)
            ?.tenantId
        return authInstanceGrantDao
            .searchLiveGrants(request.subject.principalId, resourceType, instanceId)
            .filter { subjectTenant.isNullOrBlank() || it.tenantId == subjectTenant }
            .filter { PermissionCodes.matches(it.action, request.permissionCode) }
    }

    /**
     * A binding with no condition always applies; otherwise the evaluator decides.
     *
     * A condition judged **false** makes the binding absent whatever its effect — that is what a
     * conditional binding means. A condition that **cannot be judged** (the evaluator threw —
     * malformed expression, missing evidence) is handled asymmetrically, and the asymmetry is the
     * security property: an ALLOW is absent, because the caller could not produce the evidence that
     * confers the access; a DENY **applies**, because a restriction that could not be checked has
     * not thereby been lifted. Treating both effects alike here was the defect: every conditional
     * DENY silently vanished whenever its context attribute went unsupplied — and the context is
     * entirely the caller's to supply.
     */
    private fun PermissionGrantVo.applies(context: Map<String, Any?>): Boolean {
        val expression = condition
        if (expression.isNullOrBlank()) return true
        return try {
            conditionEvaluator.evaluate(expression, context)
        } catch (e: Exception) {
            val denies = effect == PermissionEffect.DENY
            log.warn(
                "Condition '${expression}' could not be judged (${e.message}); " +
                    if (denies) "keeping the DENY binding in force." else "treating the ALLOW binding as absent.",
            )
            denies
        }
    }
}
