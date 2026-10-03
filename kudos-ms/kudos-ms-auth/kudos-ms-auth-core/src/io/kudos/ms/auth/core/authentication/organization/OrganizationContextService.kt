package io.kudos.ms.auth.core.authentication.organization

import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationContext
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.common.authentication.vo.AvailableOrganizationContexts
import io.kudos.ms.auth.common.authentication.vo.AvailableTenantContext
import io.kudos.ms.auth.common.authentication.vo.OrganizationContextSwitchRequest
import io.kudos.ms.auth.common.authentication.vo.SessionScope
import io.kudos.ms.auth.common.authentication.vo.SessionTarget
import io.kudos.ms.auth.core.authentication.mfa.policy.AuthenticationMfaPolicyEnforcer
import io.kudos.ms.auth.core.authentication.mfa.policy.MfaPolicyEnforcementOutcomeEnum
import io.kudos.ms.auth.core.authentication.session.model.AuthenticationSessionIssueCommand
import io.kudos.ms.auth.core.authentication.session.service.iservice.IAuthenticationSessionService
import io.kudos.ms.auth.core.authentication.session.store.IAuthenticationSessionStore
import io.kudos.ms.auth.core.organization.service.OrganizationAuthority
import io.kudos.ms.auth.core.organization.service.TenantEntryService
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.core.tenant.dao.SysTenantDao
import io.kudos.ms.sys.core.tenant.model.po.SysTenant

/** A context request that cannot be served; [code] is a stable error code. */
class OrganizationContextException(val code: String) : IllegalStateException(code)

/**
 * Lists and switches the scope of an organization account's session.
 *
 * Switching never re-authenticates and never changes the identity: it verifies the target on the
 * server (entry, or organization scope), applies the target tenant's MFA policy (a stronger policy
 * means step-up first, G-15), issues a replacement session that keeps the original absolute deadline,
 * and retires the source with a compare-and-set so two concurrent switches cannot both win.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Service
open class OrganizationContextService(
    private val sessions: IAuthenticationSessionService,
    private val store: IAuthenticationSessionStore,
    private val entries: TenantEntryService,
    private val authority: OrganizationAuthority,
    private val mfaPolicy: AuthenticationMfaPolicyEnforcer,
    private val tenants: SysTenantDao,
) {

    open fun contexts(source: AuthenticationSession): AvailableOrganizationContexts {
        val organizationId = source.organizationId ?: fail("AUTHENTICATION_ORGANIZATION_REQUIRED")
        val tenants = entries.accessibleTenants(source.userId)
            .map { AvailableTenantContext(it.tenantId, it.tenantName, it.organizationId, it.subSystemCodes.toList()) }
        return AvailableOrganizationContexts(
            organizationId = organizationId,
            current = source.target(),
            organizationScope = authority.hasOrganizationScope(organizationId, source.userId),
            tenants = tenants,
        )
    }

    open fun switch(source: AuthenticationSession, request: OrganizationContextSwitchRequest): AuthenticationSession {
        if (request.contextVersion != source.id || !source.isActive()) fail("AUTHENTICATION_CONTEXT_CHANGED")
        val organizationId = source.organizationId ?: fail("AUTHENTICATION_ORGANIZATION_REQUIRED")
        val tenantId = request.tenantId?.takeIf(String::isNotBlank)
        if (tenantId == null) {
            if (!authority.hasOrganizationScope(organizationId, source.userId)) fail("AUTHENTICATION_ORGANIZATION_SCOPE_UNAVAILABLE")
            // The organization scope manages every tenant, so it needs the strongest authentication any of
            // them demands (G-15).
            tenants.search(Criteria(SysTenant::organizationId eq organizationId)).forEach { requireMfa(it.id, source) }
        } else {
            val entry = entries.check(tenantId, source.userId, request.subSystemCode?.takeIf(String::isNotBlank))
            val target = entry.entry ?: fail("AUTHENTICATION_TENANT_UNAVAILABLE:${entry.denial}")
            if (target.organizationId != organizationId) fail("AUTHENTICATION_ORGANIZATION_MISMATCH")
            requireMfa(tenantId, source)
        }
        val remainingSeconds = Duration.between(Instant.now(), source.absoluteExpiresAt).seconds
        if (remainingSeconds <= 0) fail("AUTHENTICATION_CONTEXT_CHANGED")
        val replacement = sessions.issue(AuthenticationSessionIssueCommand(
            context = AuthenticationContext(
                userId = source.userId, tenantId = tenantId.orEmpty(), authTime = source.authTime,
                amr = source.amr, acr = source.acr, credentialVersion = source.credentialVersion,
                riskLevel = source.riskLevel, organizationId = organizationId,
                subSystemCode = request.subSystemCode?.takeIf(String::isNotBlank),
            ),
            username = source.username, clientId = source.clientId, deviceId = source.deviceId,
            loginIp = source.loginIp, loginDevice = source.loginDevice, loginBrowser = source.loginBrowser,
            loginOs = source.loginOs, userAgent = source.userAgent, absoluteTimeoutSeconds = remainingSeconds,
        ))
        // One winner across nodes: a concurrent switch or revocation cannot publish a second target.
        val retired = store.save(source.copy(revokedAt = Instant.now(), revokeReason = "CONTEXT_SWITCHED"), source.version)
        if (retired == null) {
            sessions.revokeForUser(replacement.id, replacement.tenantId, replacement.userId, "CONTEXT_SWITCH_CONFLICT")
            fail("AUTHENTICATION_CONTEXT_CHANGED")
        }
        return replacement
    }

    /** The session's authentication must already satisfy [tenantId]'s MFA policy; otherwise step up first. */
    private fun requireMfa(tenantId: String, source: AuthenticationSession) {
        val policy = mfaPolicy.enforce(AuthenticationTransactionPurposeEnum.LOGIN, tenantId, source.userId, source.acr)
        if (policy.outcome == MfaPolicyEnforcementOutcomeEnum.REQUIRE_SECOND_FACTOR ||
            policy.outcome == MfaPolicyEnforcementOutcomeEnum.DENY_ENROLLMENT_REQUIRED
        ) {
            fail("AUTHENTICATION_STEP_UP_REQUIRED")
        }
    }

    private fun fail(code: String): Nothing = throw OrganizationContextException(code)
}

/** The session's target as the console sees it. */
fun AuthenticationSession.target(): SessionTarget = SessionTarget(
    organizationId = requireNotNull(organizationId),
    scope = if (tenantId.isBlank()) SessionScope.ORGANIZATION else SessionScope.TENANT,
    tenantId = tenantId.takeIf(String::isNotBlank),
    subSystemCode = subSystemCode,
    contextVersion = id,
)
