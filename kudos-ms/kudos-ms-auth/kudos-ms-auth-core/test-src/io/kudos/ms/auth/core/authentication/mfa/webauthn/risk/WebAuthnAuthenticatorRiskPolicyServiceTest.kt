package io.kudos.ms.auth.core.authentication.mfa.webauthn.risk

import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.service.iservice.IWebAuthnAuthenticatorRiskService
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskLevelEnum
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskSummary
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.dao.AuthWebAuthnAuthenticatorRiskPolicyDao
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.event.WebAuthnAuthenticatorRiskPolicyBlocked
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.WebAuthnAuthenticatorRiskEnforcementCommand
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.WebAuthnAuthenticatorRiskPolicyException
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.WebAuthnAuthenticatorRiskPolicySaveCommand
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.po.AuthWebAuthnAuthenticatorRiskPolicy
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.service.impl.WebAuthnAuthenticatorRiskPolicyService
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class WebAuthnAuthenticatorRiskPolicyServiceTest {
    private val now = Instant.parse("2026-08-25T10:00:00Z")
    private val dao = mock(AuthWebAuthnAuthenticatorRiskPolicyDao::class.java)
    private val riskService = mock(IWebAuthnAuthenticatorRiskService::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val service = WebAuthnAuthenticatorRiskPolicyService(
        dao,
        riskService,
        Clock.fixed(now, ZoneOffset.UTC),
        eventPublisher,
    )

    @Test
    fun defaultPolicyIsAuditOnly() {
        val policy = service.getEffective("t-1")

        assertFalse(policy.configured)
        assertTrue(policy.blockedRiskLevels.isEmpty())
        service.enforce(enforcement(WebAuthnAuthenticatorRiskLevelEnum.CRITICAL))
    }

    @Test
    fun saveNormalizesBlockLevelsAndAuditFields() {
        `when`(riskService.isAvailable()).thenReturn(true)

        val result = service.save(command(setOf(" critical ", "warning")))

        assertEquals(
            setOf(WebAuthnAuthenticatorRiskLevelEnum.WARNING, WebAuthnAuthenticatorRiskLevelEnum.CRITICAL),
            result.blockedRiskLevels,
        )
        val captor = ArgumentCaptor.forClass(AuthWebAuthnAuthenticatorRiskPolicy::class.java)
        verify(dao).insert(captor.capture() ?: fallbackPolicy())
        assertEquals("CRITICAL,WARNING", captor.value.blockedRiskLevels)
        assertEquals("admin-1", captor.value.updateUserId)
        assertEquals(LocalDateTime.ofInstant(now, ZoneOffset.UTC), captor.value.updateTime)
    }

    @Test
    fun blockingPolicyRequiresRiskEvaluationCapability() {
        val error = assertFailsWith<WebAuthnAuthenticatorRiskPolicyException> {
            service.save(command(setOf("CRITICAL")))
        }

        assertEquals("WEBAUTHN_RISK_EVALUATION_NOT_AVAILABLE", error.errorCode)
        verify(dao, never()).insert(any(AuthWebAuthnAuthenticatorRiskPolicy::class.java) ?: fallbackPolicy())
    }

    @Test
    fun normalCannotBeBlockedButNotEvaluatedCanBeBlockedExplicitly() {
        `when`(riskService.isAvailable()).thenReturn(true)

        val error = assertFailsWith<WebAuthnAuthenticatorRiskPolicyException> {
            service.save(command(setOf("NORMAL")))
        }
        val saved = service.save(command(setOf("NOT_EVALUATED")))

        assertEquals("WEBAUTHN_RISK_POLICY_INVALID", error.errorCode)
        assertEquals(
            setOf(WebAuthnAuthenticatorRiskLevelEnum.NOT_EVALUATED),
            saved.blockedRiskLevels,
        )
    }

    @Test
    fun configuredPolicyBlocksOnlySelectedRiskLevels() {
        `when`(dao.get("t-1")).thenReturn(policy("CRITICAL"))

        service.enforce(enforcement(WebAuthnAuthenticatorRiskLevelEnum.WARNING))
        val error = assertFailsWith<WebAuthnAuthenticatorRiskPolicyException> {
            service.enforce(enforcement(WebAuthnAuthenticatorRiskLevelEnum.CRITICAL))
        }

        assertEquals("WEBAUTHN_AUTHENTICATOR_RISK_BLOCKED", error.errorCode)
        val captor = ArgumentCaptor.forClass(WebAuthnAuthenticatorRiskPolicyBlocked::class.java)
        verify(eventPublisher).publishEvent(captor.capture() ?: fallbackBlockedEvent())
        assertEquals("t-1", captor.value.tenantId)
        assertEquals("user-1", captor.value.userId)
        assertEquals(FINGERPRINT, captor.value.credentialIdFingerprint)
        assertEquals(WebAuthnAuthenticatorRiskLevelEnum.CRITICAL, captor.value.riskLevel)
        assertEquals(setOf("FIDO_MDS"), captor.value.riskSources)
        assertEquals(setOf("REVOKED"), captor.value.riskStatusCodes)
        assertEquals(now, captor.value.occurredAt)
    }

    @Test
    fun eventListenerFailureCannotBypassOrReplaceRiskBlock() {
        `when`(dao.get("t-1")).thenReturn(policy("CRITICAL"))
        doThrow(IllegalStateException("telemetry unavailable"))
            .`when`(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any())

        val error = assertFailsWith<WebAuthnAuthenticatorRiskPolicyException> {
            service.enforce(enforcement(WebAuthnAuthenticatorRiskLevelEnum.CRITICAL))
        }

        assertEquals("WEBAUTHN_AUTHENTICATOR_RISK_BLOCKED", error.errorCode)
    }

    private fun command(levels: Set<String>) = WebAuthnAuthenticatorRiskPolicySaveCommand(
        tenantId = "t-1",
        blockedRiskLevels = levels,
        actorUserId = "admin-1",
        operationReason = "Block compromised authenticators",
    )

    private fun policy(levels: String? = null) = AuthWebAuthnAuthenticatorRiskPolicy {
        id = "t-1"
        tenantId = "t-1"
        blockedRiskLevels = levels
        createUserId = "admin-1"
        createReason = "created"
        createTime = LocalDateTime.ofInstant(now.minusSeconds(60), ZoneOffset.UTC)
        updateUserId = "admin-1"
        updateReason = "updated"
        updateTime = LocalDateTime.ofInstant(now, ZoneOffset.UTC)
    }

    private fun fallbackPolicy() = policy()

    private fun risk(level: WebAuthnAuthenticatorRiskLevelEnum) =
        WebAuthnAuthenticatorRiskSummary(
            level = level,
            sources = setOf("FIDO_MDS"),
            statusCodes = if (level == WebAuthnAuthenticatorRiskLevelEnum.CRITICAL) setOf("REVOKED") else emptySet(),
        )

    private fun enforcement(level: WebAuthnAuthenticatorRiskLevelEnum) =
        WebAuthnAuthenticatorRiskEnforcementCommand(
            tenantId = "t-1",
            userId = "user-1",
            credentialIdFingerprint = FINGERPRINT,
            risk = risk(level),
        )

    private fun fallbackBlockedEvent() = WebAuthnAuthenticatorRiskPolicyBlocked(
        tenantId = "fallback",
        userId = "fallback",
        credentialIdFingerprint = FINGERPRINT,
        riskLevel = WebAuthnAuthenticatorRiskLevelEnum.CRITICAL,
        riskSources = emptySet(),
        riskStatusCodes = emptySet(),
        occurredAt = now,
    )

    private companion object {
        val FINGERPRINT = "A".repeat(43)
    }
}
