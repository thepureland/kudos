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
import io.kudos.ms.auth.core.organization.service.OrganizationAuthorizationResolver
import io.kudos.ms.auth.core.organization.service.OrganizationRequestTarget
import io.kudos.ms.auth.core.organization.service.OrganizationRequestTargetResolver
import io.kudos.ms.sys.core.organization.OrganizationMode
import org.springframework.beans.factory.annotation.Autowired


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

    /** Organization-mode decisions; absent in pure unit tests. */
    @Autowired(required = false)
    private var organizationTargets: OrganizationRequestTargetResolver? = null

    @Autowired(required = false)
    private var organizationAuthorization: OrganizationAuthorizationResolver? = null

    @Autowired(required = false)
    private var organizationMode: OrganizationMode? = null

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

        // 1b. Organization mode: an organization account is judged in the scope its session works in.
        organizationTargetOf(request.subject)?.let { return decideForOrganization(request, it) }
        if (isOrganizationAccount(request.subject)) {
            return AuthzDecision.deny(
                AuthzDecision.Reason.DENIED_BY_DEFAULT,
                code = request.permissionCode,
                detail = "an organization account is authorized only within its current organization scope",
            )
        }

        // The directory lookup behind the instance-grant tenant bound runs only when the request names an instance.
        return evaluate(request, resolveGrants(request.subject)) { instanceTenantOf(request.subject) }
    }

    /**
     * Steps 2–4 of the algebra over [grants]; [instanceTenant] supplies the tenant that bounds the
     * instance grants that may join. It is invoked only when the request names an instance, so the
     * legacy path pays no directory lookup for ordinary decisions.
     */
    private fun evaluate(
        request: AuthzRequest,
        grants: List<PermissionGrantVo>,
        context: Map<String, Any?> = request.context,
        instanceTenant: () -> String?,
    ): AuthzDecision {
        // 2. Collect, then 3. drop bindings whose condition does not hold in this context. A
        //    condition judged false makes the binding *absent*, not denying; one that cannot be
        //    judged at all is asymmetric by effect — see applies() below.
        val applicable = grants
            .filter { it.applies(context) }
            .filter { PermissionCodes.matches(it.permissionCode, request.permissionCode) }

        // Instance-level grants join the same collection when the question names a particular thing.
        val instanceGrants = collectInstanceGrants(request, instanceTenant)

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
        organizationTargetOf(subject)?.let { target ->
            val resolver = requireNotNull(organizationAuthorization)
            val authorization = resolver.resolve(target)
            // Administrators and the organization scope hold rule-shaped permissions (management codes,
            // entitled resources) rather than role bindings; their code set is listed as such.
            if (authorization.allowed && (target is OrganizationRequestTarget.Organization || authorization.organizationAdmin)) {
                return resolver.permissionCodesForAdministration(authorization)
            }
        }
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

    /** The legacy subject's own tenant, for bounding instance grants. */
    private fun instanceTenantOf(subject: SubjectRef): String? =
        principalDirectoryRegistry.find(subject.principalId, subject.principalType)?.tenantId

    /** The current organization request target when it belongs to [subject]. */
    private fun organizationTargetOf(subject: SubjectRef): OrganizationRequestTarget? =
        if (subject.principalType != SUBJECT_TYPE_USER) null else organizationTargets?.forUser(subject.principalId)

    private fun isOrganizationAccount(subject: SubjectRef): Boolean =
        organizationMode?.enabled == true &&
            !principalDirectoryRegistry.find(subject.principalId, subject.principalType)?.organizationId.isNullOrBlank()

    /**
     * Organization mode (G-4, G-6, G-9). Tenant scope: the account must be able to enter the tenant now;
     * an organization administrator then holds every entitled function, anybody else exactly the grants
     * of their effective roles there, under the same DENY-first algebra. Organization scope: management
     * permissions only, for administrators and management role holders.
     */
    private fun decideForOrganization(request: AuthzRequest, target: OrganizationRequestTarget): AuthzDecision {
        val resolver = requireNotNull(organizationAuthorization)
        val authorization = resolver.resolve(target)
        val code = request.permissionCode
        if (!authorization.allowed) {
            return AuthzDecision.deny(AuthzDecision.Reason.DENIED_BY_DEFAULT, code = code, detail = "organization: ${authorization.denial}")
        }
        if (target is OrganizationRequestTarget.Organization) {
            return if (resolver.isManagementCode(code)) {
                AuthzDecision.permit(AuthzDecision.Reason.ALLOWED_BY_ORGANIZATION_MANAGEMENT, code = code, detail = "rank ${authorization.rank}")
            } else {
                AuthzDecision.deny(AuthzDecision.Reason.DENIED_BY_DEFAULT, code = code, detail = "the organization scope holds management permissions only")
            }
        }
        val tenant = target as OrganizationRequestTarget.Tenant
        if (authorization.organizationAdmin) {
            return if (resolver.permitsForOrganizationAdmin(tenant.tenantId, code)) {
                AuthzDecision.permit(AuthzDecision.Reason.ORGANIZATION_ADMIN, code = code, detail = "organization administrator in ${tenant.tenantId}")
            } else {
                AuthzDecision.deny(AuthzDecision.Reason.DENIED_BY_DEFAULT, code = code, detail = "outside the tenant's subscribed sub-systems")
            }
        }
        val context = request.context + mapOf(
            "organizationId" to tenant.organizationId,
            "tenantId" to tenant.tenantId,
            "subSystemCode" to tenant.subSystemCode,
        )
        return evaluate(request, authorization.grants, context) { tenant.tenantId }
    }

    @Transactional(readOnly = true)
    override fun resolveGrants(subject: SubjectRef): List<PermissionGrantVo> {
        // An organization account's grants exist only per tenant: those of its current tenant, or none.
        organizationTargetOf(subject)?.let { target ->
            val authorization = requireNotNull(organizationAuthorization).resolve(target)
            return if (authorization.allowed) authorization.grants else emptyList()
        }
        if (isOrganizationAccount(subject)) return emptyList()
        return permissionGrantsByUserIdCache.getGrants(subject.principalId)
    }

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
    private fun collectInstanceGrants(request: AuthzRequest, subjectTenantOf: () -> String?): List<AuthInstanceGrant> {
        val resourceType = request.resourceType?.takeIf { it.isNotBlank() } ?: return emptyList()
        val instanceId = request.instanceId?.takeIf { it.isNotBlank() } ?: return emptyList()
        val subjectTenant = subjectTenantOf()
        // The subject's real tenant (for an organization account: the tenant it works in now), so a
        // grant row cannot reach across the boundary even if one was written before the share path
        // validated it (or straight into the database). Defence in depth: the write path is where
        // this is properly enforced, but the tenant boundary is the one invariant worth checking on
        // both sides.
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

    private companion object {
        const val SUBJECT_TYPE_USER = "USER"
    }
}
