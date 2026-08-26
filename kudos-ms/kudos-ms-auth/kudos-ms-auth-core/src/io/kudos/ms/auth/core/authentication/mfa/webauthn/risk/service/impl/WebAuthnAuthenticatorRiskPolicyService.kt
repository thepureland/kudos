package io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.service.iservice.IWebAuthnAuthenticatorRiskService
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskLevelEnum
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.dao.AuthWebAuthnAuthenticatorRiskPolicyDao
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.event.WebAuthnAuthenticatorRiskPolicyBlocked
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.EffectiveWebAuthnAuthenticatorRiskPolicy
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.WebAuthnAuthenticatorRiskEnforcementCommand
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.WebAuthnAuthenticatorRiskPolicyException
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.WebAuthnAuthenticatorRiskPolicySaveCommand
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.po.AuthWebAuthnAuthenticatorRiskPolicy
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.service.iservice.IWebAuthnAuthenticatorRiskPolicyService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
@Transactional
open class WebAuthnAuthenticatorRiskPolicyService(
    private val dao: AuthWebAuthnAuthenticatorRiskPolicyDao,
    private val riskService: IWebAuthnAuthenticatorRiskService,
    private val clock: Clock = Clock.systemUTC(),
    private val eventPublisher: ApplicationEventPublisher? = null,
) : IWebAuthnAuthenticatorRiskPolicyService {
    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getEffective(tenantId: String): EffectiveWebAuthnAuthenticatorRiskPolicy {
        requireTenantId(tenantId)
        return dao.get(tenantId)?.toEffective() ?: EffectiveWebAuthnAuthenticatorRiskPolicy(tenantId)
    }

    @Transactional(readOnly = true)
    override fun isRiskEvaluationAvailable(): Boolean = riskService.isAvailable()

    override fun save(
        command: WebAuthnAuthenticatorRiskPolicySaveCommand,
    ): EffectiveWebAuthnAuthenticatorRiskPolicy {
        requireTenantId(command.tenantId)
        if (command.actorUserId.isBlank()) fail("WEBAUTHN_RISK_POLICY_ACTOR_REQUIRED")
        val reason = command.operationReason.trim()
        if (reason.isBlank() || reason.length > 512) fail("WEBAUTHN_RISK_POLICY_REASON_INVALID")
        val blockedLevels = normalizeBlockedLevels(command.blockedRiskLevels)
        if (blockedLevels.isNotEmpty() && !isRiskEvaluationAvailable()) {
            fail("WEBAUTHN_RISK_EVALUATION_NOT_AVAILABLE")
        }
        val now = LocalDateTime.now(clock)
        val existing = dao.get(command.tenantId)
        val policy = existing ?: AuthWebAuthnAuthenticatorRiskPolicy {
            id = command.tenantId
            tenantId = command.tenantId
            createUserId = command.actorUserId
            createReason = reason
            createTime = now
        }
        policy.tenantId = command.tenantId
        policy.blockedRiskLevels = blockedLevels.map { it.name }.toSortedSet().csvOrNull()
        policy.updateUserId = command.actorUserId
        policy.updateReason = reason
        policy.updateTime = now
        if (existing == null) dao.insert(policy) else if (!dao.update(policy)) {
            fail("WEBAUTHN_RISK_POLICY_UPDATE_FAILED")
        }
        return policy.toEffective()
    }

    @Transactional(readOnly = true)
    override fun enforce(command: WebAuthnAuthenticatorRiskEnforcementCommand) {
        requireEnforcementCommand(command)
        if (command.risk.level in getEffective(command.tenantId).blockedRiskLevels) {
            publishBlockedEvent(command)
            fail("WEBAUTHN_AUTHENTICATOR_RISK_BLOCKED")
        }
    }

    private fun publishBlockedEvent(command: WebAuthnAuthenticatorRiskEnforcementCommand) {
        val event = WebAuthnAuthenticatorRiskPolicyBlocked(
            tenantId = command.tenantId,
            userId = command.userId,
            credentialIdFingerprint = command.credentialIdFingerprint,
            riskLevel = command.risk.level,
            riskSources = command.risk.sources.toSortedSet(),
            riskStatusCodes = command.risk.statusCodes.toSortedSet(),
            occurredAt = clock.instant(),
        )
        try {
            eventPublisher?.publishEvent(event)
        } catch (e: Exception) {
            log.error(e, "Failed to publish WebAuthn authenticator risk policy blocked event")
        }
    }

    private fun AuthWebAuthnAuthenticatorRiskPolicy.toEffective() =
        EffectiveWebAuthnAuthenticatorRiskPolicy(
            tenantId = tenantId,
            blockedRiskLevels = blockedRiskLevels.csvValues().map(::parseLevel).toSet(),
            effectiveFrom = updateTime,
            configured = true,
        )

    private fun normalizeBlockedLevels(values: Set<String>): Set<WebAuthnAuthenticatorRiskLevelEnum> {
        if (values.size > BLOCKABLE_LEVELS.size) fail("WEBAUTHN_RISK_POLICY_INVALID")
        return values.map(::parseLevel).toSet().also {
            if (!BLOCKABLE_LEVELS.containsAll(it)) fail("WEBAUTHN_RISK_POLICY_INVALID")
        }
    }

    private fun parseLevel(value: String): WebAuthnAuthenticatorRiskLevelEnum = runCatching {
        WebAuthnAuthenticatorRiskLevelEnum.valueOf(value.trim().uppercase())
    }.getOrElse { fail("WEBAUTHN_RISK_LEVEL_INVALID", it) }

    private fun requireTenantId(tenantId: String) {
        if (tenantId.isBlank() || tenantId.length > 36) fail("WEBAUTHN_RISK_POLICY_TENANT_INVALID")
    }

    private fun requireEnforcementCommand(command: WebAuthnAuthenticatorRiskEnforcementCommand) {
        requireTenantId(command.tenantId)
        if (command.userId.isBlank() || command.userId.length > 36) {
            fail("WEBAUTHN_RISK_POLICY_USER_INVALID")
        }
        if (!CREDENTIAL_FINGERPRINT.matches(command.credentialIdFingerprint)) {
            fail("WEBAUTHN_RISK_POLICY_CREDENTIAL_INVALID")
        }
    }

    private fun Set<String>.csvOrNull(): String? = takeIf { it.isNotEmpty() }?.joinToString(",")
    private fun String?.csvValues(): Set<String> = this?.split(',')?.map { it.trim() }
        ?.filter { it.isNotBlank() }?.toSortedSet() ?: emptySet()

    private fun fail(errorCode: String, cause: Throwable? = null): Nothing =
        throw WebAuthnAuthenticatorRiskPolicyException(errorCode, cause)

    private companion object {
        val CREDENTIAL_FINGERPRINT = Regex("^[A-Za-z0-9_-]{43}$")
        val BLOCKABLE_LEVELS = setOf(
            WebAuthnAuthenticatorRiskLevelEnum.NOT_EVALUATED,
            WebAuthnAuthenticatorRiskLevelEnum.WARNING,
            WebAuthnAuthenticatorRiskLevelEnum.CRITICAL,
        )
    }
}
