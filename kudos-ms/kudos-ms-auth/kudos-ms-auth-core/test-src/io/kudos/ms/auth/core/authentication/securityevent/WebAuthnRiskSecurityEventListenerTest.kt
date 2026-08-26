package io.kudos.ms.auth.core.authentication.securityevent

import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskLevelEnum
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.event.WebAuthnAuthenticatorRiskPolicyBlocked
import io.kudos.ms.auth.core.authentication.securityevent.listener.WebAuthnRiskSecurityEventListener
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventRecordCommand
import io.kudos.ms.auth.core.authentication.securityevent.service.iservice.IAuthSecurityEventService
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.sql.SQLException
import java.time.Instant
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class WebAuthnRiskSecurityEventListenerTest {
    private val service = mock(IAuthSecurityEventService::class.java)
    private val listener = WebAuthnRiskSecurityEventListener(service)

    @Test
    fun mapsEventToStableFiveMinuteDeduplicationBucket() {
        listener.onBlocked(event())

        val captor = ArgumentCaptor.forClass(AuthSecurityEventRecordCommand::class.java)
        verify(service).recordOrAggregate(captor.capture() ?: fallbackCommand())
        val command = captor.value
        assertEquals(LocalDateTime.of(2026, 8, 25, 10, 0), command.bucketStart)
        assertEquals(LocalDateTime.of(2026, 8, 25, 10, 2, 34), command.occurredAt)
        assertEquals(FINGERPRINT, command.subjectFingerprint)
        assertEquals(43, command.deduplicationKey.length)
        assertEquals(setOf("CUSTOM_SOURCE", "FIDO_MDS"), command.riskSources)
        assertEquals(setOf("KEY_COMPROMISE", "REVOKED"), command.riskStatusCodes)
    }

    @Test
    fun normalizesCodesDeterministicallyBeforeHashing() {
        listener.onBlocked(event().copy(riskSources = linkedSetOf("Z_SOURCE", "A_SOURCE", "来源")))
        listener.onBlocked(event().copy(riskSources = linkedSetOf("来源", "A_SOURCE", "Z_SOURCE")))

        val captor = ArgumentCaptor.forClass(AuthSecurityEventRecordCommand::class.java)
        verify(service, org.mockito.Mockito.times(2)).recordOrAggregate(captor.capture() ?: fallbackCommand())
        val commands = captor.allValues
        assertEquals(setOf("A_SOURCE", "Z_SOURCE", "__"), commands.first().riskSources)
        assertEquals(commands.first().deduplicationKey, commands.last().deduplicationKey)
    }

    @Test
    fun retriesOnlyUniqueConstraintConflictsInAnotherServiceCall() {
        doThrow(
            org.springframework.dao.DataIntegrityViolationException(
                "duplicate",
                SQLException("unique", "23505"),
            )
        ).`when`(service).recordOrAggregate(any(AuthSecurityEventRecordCommand::class.java) ?: fallbackCommand())

        listener.onBlocked(event())

        val captor = ArgumentCaptor.forClass(AuthSecurityEventRecordCommand::class.java)
        verify(service).aggregateAfterConcurrentInsert(captor.capture() ?: fallbackCommand())
        assertEquals(FINGERPRINT, captor.value.subjectFingerprint)
    }

    @Test
    fun doesNotRetryUnrelatedIntegrityFailures() {
        doThrow(org.springframework.dao.DataIntegrityViolationException("not-null"))
            .`when`(service).recordOrAggregate(any(AuthSecurityEventRecordCommand::class.java) ?: fallbackCommand())

        listener.onBlocked(event())

        verify(service, never()).aggregateAfterConcurrentInsert(
            any(AuthSecurityEventRecordCommand::class.java) ?: fallbackCommand()
        )
    }

    private fun event() = WebAuthnAuthenticatorRiskPolicyBlocked(
        tenantId = "tenant-1",
        userId = "user-1",
        credentialIdFingerprint = FINGERPRINT,
        riskLevel = WebAuthnAuthenticatorRiskLevelEnum.CRITICAL,
        riskSources = setOf("FIDO_MDS", "CUSTOM SOURCE"),
        riskStatusCodes = setOf("REVOKED", "KEY/COMPROMISE"),
        occurredAt = Instant.parse("2026-08-25T10:02:34Z"),
    )

    private fun fallbackCommand() = AuthSecurityEventRecordCommand(
        tenantId = "fallback",
        userId = "fallback",
        eventType = io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventTypeEnum
            .WEBAUTHN_AUTHENTICATOR_RISK_BLOCKED,
        subjectType = "WEBAUTHN_CREDENTIAL",
        subjectFingerprint = FINGERPRINT,
        riskLevel = WebAuthnAuthenticatorRiskLevelEnum.CRITICAL,
        riskSources = emptySet(),
        riskStatusCodes = emptySet(),
        deduplicationKey = "B".repeat(43),
        bucketStart = LocalDateTime.of(2026, 8, 25, 10, 0),
        occurredAt = LocalDateTime.of(2026, 8, 25, 10, 2),
    ).also { assertTrue(it.deduplicationKey.isNotBlank()) }

    private companion object {
        val FINGERPRINT = "A".repeat(43)
    }
}
