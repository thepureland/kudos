package io.kudos.ms.auth.core.authentication.credentialrevocation

import io.kudos.ms.auth.core.authentication.credentialrevocation.dao.AuthCredentialRevocationDao
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.AuthCredentialRevocationCommand
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.AuthCredentialRevocationException
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.AuthCredentialTypeEnum
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.po.AuthCredentialRevocation
import io.kudos.ms.auth.core.authentication.credentialrevocation.service.impl.AuthCredentialRevocationService
import io.kudos.ms.auth.core.authentication.mfa.policy.model.EffectiveTenantMfaPolicy
import io.kudos.ms.auth.core.authentication.mfa.policy.model.MfaPolicyDecision
import io.kudos.ms.auth.core.authentication.mfa.policy.service.iservice.ITenantMfaPolicyService
import io.kudos.ms.auth.core.authentication.mfa.service.iservice.ITotpEnrollmentService
import io.kudos.ms.auth.core.authentication.mfa.webauthn.dao.AuthWebAuthnCredentialDao
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.po.AuthWebAuthnCredential
import io.kudos.ms.auth.core.authentication.mfa.webauthn.service.iservice.IWebAuthnCredentialService
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class AuthCredentialRevocationServiceTest {
    private val instant = Instant.parse("2026-08-26T10:00:00Z")
    private val now = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
    private val dao = mock(AuthCredentialRevocationDao::class.java)
    private val credentialDao = mock(AuthWebAuthnCredentialDao::class.java)
    private val credentials = mock(IWebAuthnCredentialService::class.java)
    private val totp = mock(ITotpEnrollmentService::class.java)
    private val policies = mock(ITenantMfaPolicyService::class.java)
    private val service = AuthCredentialRevocationService(
        dao,
        credentialDao,
        credentials,
        totp,
        policies,
        Clock.fixed(instant, ZoneOffset.UTC),
    )

    @Test
    fun aPasskeyIsRevokedByTheAuditRowIdAndRecordedWithItsFingerprint() {
        `when`(credentialDao.findByUser("t-1", "u-1")).thenReturn(listOf(credential()))
        `when`(credentials.revoke("t-1", "u-1", CREDENTIAL_ID)).thenReturn(true)
        `when`(policies.evaluate("t-1", "u-1")).thenReturn(decision(enrolled = true))

        val result = service.revoke(command())

        assertEquals(AuthCredentialTypeEnum.WEBAUTHN, result.credentialType)
        assertEquals("row-1", result.credentialRef)
        // The stored evidence is the same fingerprint the audit view shows, never the raw credential id.
        assertNotEquals(CREDENTIAL_ID, result.credentialFingerprint)
        assertEquals(43, result.credentialFingerprint?.length)
        assertEquals("admin-1", result.actorUserId)
        assertEquals("event-1", result.securityEventId)
        assertEquals(now, result.revokedAt)
        assertFalse(result.leftWithoutFactor)
        assertFalse(result.enrollmentBlocked)
        verify(credentials).revoke("t-1", "u-1", CREDENTIAL_ID)
    }

    @Test
    fun cuttingTheLastFactorIsCarriedOutAndReported() {
        `when`(credentialDao.findByUser("t-1", "u-1")).thenReturn(listOf(credential()))
        `when`(credentials.revoke("t-1", "u-1", CREDENTIAL_ID)).thenReturn(true)
        // Nothing left, the tenant requires a factor, and the grace period is long gone.
        `when`(policies.evaluate("t-1", "u-1"))
            .thenReturn(decision(enrolled = false, required = true, grace = false))

        val result = service.revoke(command())

        // A credential believed compromised has to stop working; refusing would leave it usable.
        assertTrue(result.leftWithoutFactor)
        // ...and the operator is told immediately that this account now needs an enrollment exemption.
        assertTrue(result.enrollmentBlocked)
        verify(credentials).revoke("t-1", "u-1", CREDENTIAL_ID)
    }

    @Test
    fun anAccountStillInsideItsGracePeriodIsNotReportedAsBlocked() {
        `when`(credentialDao.findByUser("t-1", "u-1")).thenReturn(listOf(credential()))
        `when`(credentials.revoke("t-1", "u-1", CREDENTIAL_ID)).thenReturn(true)
        `when`(policies.evaluate("t-1", "u-1"))
            .thenReturn(decision(enrolled = false, required = true, grace = true))

        val result = service.revoke(command())

        assertTrue(result.leftWithoutFactor)
        assertFalse(result.enrollmentBlocked)
    }

    @Test
    fun aCredentialBelongingToAnotherAccountIsNotFound() {
        `when`(credentialDao.findByUser("t-1", "u-1")).thenReturn(emptyList())

        val failure = assertFailsWith<AuthCredentialRevocationException> { service.revoke(command()) }

        assertEquals("AUTH_CREDENTIAL_REVOCATION_CREDENTIAL_NOT_FOUND", failure.errorCode)
        verify(dao, never()).insert(any(AuthCredentialRevocation::class.java) ?: revocationPo())
    }

    @Test
    fun anAlreadyRevokedCredentialIsNotRevokedTwice() {
        `when`(credentialDao.findByUser("t-1", "u-1"))
            .thenReturn(listOf(credential().apply { revokedAt = now.minusDays(1) }))

        val failure = assertFailsWith<AuthCredentialRevocationException> { service.revoke(command()) }

        assertEquals("AUTH_CREDENTIAL_REVOCATION_ALREADY_REVOKED", failure.errorCode)
        verify(credentials, never()).revoke("t-1", "u-1", CREDENTIAL_ID)
    }

    @Test
    fun theTotpAuthenticatorIsRevokedWithoutNamingACredential() {
        `when`(totp.disable("u-1", "t-1")).thenReturn(true)
        `when`(policies.evaluate("t-1", "u-1")).thenReturn(decision(enrolled = false, required = false))

        val result = service.revoke(command(type = AuthCredentialTypeEnum.TOTP, credentialRef = null))

        assertEquals(AuthCredentialTypeEnum.TOTP, result.credentialType)
        assertNull(result.credentialRef)
        assertNull(result.credentialFingerprint)
        val stored = ArgumentCaptor.forClass(AuthCredentialRevocation::class.java)
        verify(dao).insert(stored.capture() ?: revocationPo())
        assertEquals("TOTP", stored.value.credentialType)
    }

    @Test
    fun malformedRevocationsAreRefusedBeforeAnyCredentialIsTouched() {
        val cases = mapOf(
            "AUTH_CREDENTIAL_REVOCATION_TENANT_INVALID" to command(tenantId = " "),
            "AUTH_CREDENTIAL_REVOCATION_USER_INVALID" to command(userId = ""),
            "AUTH_CREDENTIAL_REVOCATION_ACTOR_INVALID" to command(actorUserId = "x".repeat(37)),
            "AUTH_CREDENTIAL_REVOCATION_REASON_INVALID" to command(reason = "  "),
            "AUTH_CREDENTIAL_REVOCATION_REF_REQUIRED" to command(credentialRef = null),
            "AUTH_CREDENTIAL_REVOCATION_REF_UNEXPECTED" to
                command(type = AuthCredentialTypeEnum.TOTP, credentialRef = "row-1"),
        )

        cases.forEach { (errorCode, command) ->
            val failure = assertFailsWith<AuthCredentialRevocationException> { service.revoke(command) }
            assertEquals(errorCode, failure.errorCode)
        }
        verify(credentials, never()).revoke("t-1", "u-1", CREDENTIAL_ID)
        verify(totp, never()).disable("u-1", "t-1")
    }

    private fun command(
        tenantId: String = "t-1",
        userId: String = "u-1",
        type: AuthCredentialTypeEnum = AuthCredentialTypeEnum.WEBAUTHN,
        credentialRef: String? = "row-1",
        actorUserId: String = "admin-1",
        reason: String = "authenticator reported stolen",
    ) = AuthCredentialRevocationCommand(
        tenantId = tenantId,
        userId = userId,
        credentialType = type,
        credentialRef = credentialRef,
        actorUserId = actorUserId,
        reason = reason,
        securityEventId = "event-1",
    )

    private fun decision(
        enrolled: Boolean,
        required: Boolean = true,
        grace: Boolean = false,
    ) = MfaPolicyDecision(
        policy = EffectiveTenantMfaPolicy(tenantId = "t-1"),
        required = required,
        enrolled = enrolled,
        enrollmentRequired = required && !enrolled,
        gracePeriodActive = grace,
        graceExpiresAt = null,
    )

    private fun credential() = AuthWebAuthnCredential {
        id = "row-1"
        tenantId = "t-1"
        userId = "u-1"
        credentialId = CREDENTIAL_ID
        userHandle = "aGFuZGxl"
        publicKeyCose = "cG9zZQ"
        signatureCount = 3
        transports = null
        aaguid = null
        attestationFormat = null
        backupEligible = false
        backedUp = false
        discoverable = true
        displayName = "YubiKey"
        createdAt = now.minusDays(30)
        lastUsedAt = now.minusDays(1)
        revokedAt = null
        version = 1
    }

    private fun revocationPo() = AuthCredentialRevocation {
        id = "revocation-1"
        tenantId = "t-1"
        userId = "u-1"
        credentialType = "WEBAUTHN"
        credentialRef = "row-1"
        credentialFingerprint = "fingerprint"
        actorUserId = "admin-1"
        reason = "authenticator reported stolen"
        securityEventId = null
        leftWithoutFactor = false
        revokedAt = now
    }

    private companion object {
        val CREDENTIAL_ID: String =
            Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32) { it.toByte() })
    }
}
