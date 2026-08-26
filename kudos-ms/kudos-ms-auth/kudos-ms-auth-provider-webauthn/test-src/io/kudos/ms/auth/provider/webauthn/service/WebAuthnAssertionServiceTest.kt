package io.kudos.ms.auth.provider.webauthn.service

import com.yubico.webauthn.AssertionRequest
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.service.iservice.IWebAuthnAuthenticatorRiskService
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskLevelEnum
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskSummary
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.VerifiedWebAuthnAssertion
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.WebAuthnCredentialFingerprints
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.WebAuthnCredentialSummary
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.po.AuthWebAuthnCredential
import io.kudos.ms.auth.core.authentication.mfa.webauthn.service.iservice.IWebAuthnCredentialService
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.WebAuthnAuthenticatorRiskPolicyException
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.WebAuthnAuthenticatorRiskEnforcementCommand
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.service.iservice.IWebAuthnAuthenticatorRiskPolicyService
import io.kudos.ms.auth.provider.webauthn.WebAuthnProviderConfiguration
import io.kudos.ms.auth.provider.webauthn.WebAuthnProviderProperties
import io.kudos.ms.auth.provider.webauthn.WebAuthnUserHandles
import io.kudos.ms.auth.provider.webauthn.ceremony.IWebAuthnCeremonyStore
import io.kudos.ms.auth.provider.webauthn.ceremony.WebAuthnCeremonyState
import io.kudos.ms.auth.provider.webauthn.ceremony.WebAuthnCeremonyTypeEnum
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnAssertionFinishCommand
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnProviderException
import io.kudos.ms.auth.provider.webauthn.protocol.IWebAuthnAssertionVerifier
import io.kudos.ms.auth.provider.webauthn.protocol.VerifiedWebAuthnAssertionResult
import io.kudos.ms.auth.provider.webauthn.protocol.WebAuthnAssertionVerificationRequest
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class WebAuthnAssertionServiceTest {
    private val now = Instant.parse("2026-08-25T10:00:00Z")
    private val accounts = mock(IUserAccountService::class.java)
    private val credentials = mock(IWebAuthnCredentialService::class.java)
    private val store = mock(IWebAuthnCeremonyStore::class.java)
    private val verifier = mock(IWebAuthnAssertionVerifier::class.java)
    private val riskService = mock(IWebAuthnAuthenticatorRiskService::class.java)
    private val riskPolicy = mock(IWebAuthnAuthenticatorRiskPolicyService::class.java)
    private val service = WebAuthnAssertionService(
        accounts,
        credentials,
        store,
        verifier,
        riskService,
        riskPolicy,
        WebAuthnProviderConfiguration(validProperties()),
        Clock.fixed(now, ZoneOffset.UTC),
    )

    @Test
    fun `should start known-user assertion with only that subjects credentials`() {
        activeAccount()
        `when`(credentials.listActive("tenant-1", "user-1")).thenReturn(listOf(summary()))
        `when`(store.create(org.mockito.ArgumentMatchers.any() ?: fallbackState())).thenReturn(true)

        val result = service.begin("tenant-1", "user-1")

        val captor = ArgumentCaptor.forClass(WebAuthnCeremonyState::class.java)
        verify(store).create(captor.capture() ?: fallbackState())
        val state = captor.value
        val request = AssertionRequest.fromJson(state.requestJson)
        assertEquals(WebAuthnCeremonyTypeEnum.ASSERTION, state.type)
        assertEquals("user-1", state.userId)
        assertEquals("alice", request.username.orElseThrow())
        assertFalse(request.userHandle.isPresent)
        assertEquals(listOf(encoded("credential-1")), request.publicKeyCredentialRequestOptions
            .allowCredentials.orElseThrow().map { it.id.base64Url })
        assertEquals(state.expiresAt, result.expiresAt)
        assertTrue(result.publicKeyCredentialRequestOptions.contains(encoded("credential-1")))
    }

    @Test
    fun `should start username-less assertion without subject hints`() {
        `when`(store.create(org.mockito.ArgumentMatchers.any() ?: fallbackState())).thenReturn(true)

        service.begin("tenant-1")

        val captor = ArgumentCaptor.forClass(WebAuthnCeremonyState::class.java)
        verify(store).create(captor.capture() ?: fallbackState())
        val state = captor.value
        val request = AssertionRequest.fromJson(state.requestJson)
        assertNull(state.userId)
        assertFalse(request.username.isPresent)
        assertFalse(request.userHandle.isPresent)
        assertTrue(request.publicKeyCredentialRequestOptions.allowCredentials.orElse(emptyList()).isEmpty())
        verifyNoInteractions(accounts, credentials)
    }

    @Test
    fun `should persist counter only after verified username-less assertion is rebound to active account`() {
        val state = usernameLessState()
        val verified = verifiedAssertion()
        `when`(store.consume(state.id)).thenReturn(state)
        val fallbackVerification = verificationRequest(state)
        `when`(verifier.verify(org.mockito.ArgumentMatchers.any() ?: fallbackVerification))
            .thenReturn(verified)
        `when`(credentials.findActive("tenant-1", verified.credentialId)).thenReturn(storedCredential())
        activeAccount()
        `when`(riskService.evaluate(AAGUID)).thenReturn(normalRisk())
        `when`(credentials.recordVerifiedAssertion(org.mockito.ArgumentMatchers.any() ?: fallbackVerifiedCommand()))
            .thenReturn(summary())

        val result = service.finish(
            "tenant-1",
            WebAuthnAssertionFinishCommand(state.id, "{}"),
        )

        val commandCaptor = ArgumentCaptor.forClass(VerifiedWebAuthnAssertion::class.java)
        verify(credentials).recordVerifiedAssertion(commandCaptor.capture() ?: fallbackVerifiedCommand())
        assertEquals("tenant-1", commandCaptor.value.tenantId)
        assertEquals("user-1", commandCaptor.value.userId)
        assertEquals(encoded("credential-1"), commandCaptor.value.credentialId)
        assertEquals(9, commandCaptor.value.signatureCount)
        assertTrue(commandCaptor.value.backedUp)
        assertEquals("user-1", result.userId)
        assertTrue(result.userVerified)
        assertTrue(result.backupEligible)
    }

    @Test
    fun `should block selected authenticator risk after subject validation and before counter mutation`() {
        val state = usernameLessState()
        val verified = verifiedAssertion()
        `when`(store.consume(state.id)).thenReturn(state)
        val fallbackVerification = verificationRequest(state)
        `when`(verifier.verify(org.mockito.ArgumentMatchers.any() ?: fallbackVerification))
            .thenReturn(verified)
        `when`(credentials.findActive("tenant-1", verified.credentialId)).thenReturn(storedCredential())
        activeAccount()
        val critical = WebAuthnAuthenticatorRiskSummary(
            level = WebAuthnAuthenticatorRiskLevelEnum.CRITICAL,
            sources = setOf("FIDO_MDS"),
            statusCodes = setOf("REVOKED"),
        )
        `when`(riskService.evaluate(AAGUID)).thenReturn(critical)
        val fallbackEnforcement = fallbackEnforcementCommand(critical)
        doThrow(WebAuthnAuthenticatorRiskPolicyException("WEBAUTHN_AUTHENTICATOR_RISK_BLOCKED"))
            .`when`(riskPolicy).enforce(
                org.mockito.ArgumentMatchers.any() ?: fallbackEnforcement
            )

        val error = assertFailsWith<WebAuthnProviderException> {
            service.finish("tenant-1", WebAuthnAssertionFinishCommand(state.id, "{}"))
        }

        assertEquals("WEBAUTHN_AUTHENTICATOR_RISK_BLOCKED", error.errorCode)
        val enforcementCaptor = ArgumentCaptor.forClass(WebAuthnAuthenticatorRiskEnforcementCommand::class.java)
        verify(riskPolicy).enforce(enforcementCaptor.capture() ?: fallbackEnforcement)
        assertEquals("tenant-1", enforcementCaptor.value.tenantId)
        assertEquals("user-1", enforcementCaptor.value.userId)
        assertEquals(
            WebAuthnCredentialFingerprints.sha256(encoded("credential-1")),
            enforcementCaptor.value.credentialIdFingerprint,
        )
        assertEquals(critical, enforcementCaptor.value.risk)
        verify(credentials, never()).recordVerifiedAssertion(
            org.mockito.ArgumentMatchers.any() ?: fallbackVerifiedCommand()
        )
    }

    @Test
    fun `should reject credential owned by another subject before counter mutation`() {
        activeAccount()
        `when`(credentials.listActive("tenant-1", "user-1")).thenReturn(listOf(summary()))
        `when`(store.create(org.mockito.ArgumentMatchers.any() ?: fallbackState())).thenReturn(true)
        service.begin("tenant-1", "user-1")
        val stateCaptor = ArgumentCaptor.forClass(WebAuthnCeremonyState::class.java)
        verify(store).create(stateCaptor.capture() ?: fallbackState())
        val state = stateCaptor.value
        `when`(store.consume(state.id)).thenReturn(state)
        val verified = verifiedAssertion()
        val fallbackVerification = verificationRequest(state)
        `when`(verifier.verify(org.mockito.ArgumentMatchers.any() ?: fallbackVerification))
            .thenReturn(verified)
        `when`(credentials.findActive("tenant-1", verified.credentialId))
            .thenReturn(storedCredential(userId = "user-2"))

        val error = assertFailsWith<WebAuthnProviderException> {
            service.finish(
                "tenant-1",
                WebAuthnAssertionFinishCommand(state.id, "{}"),
                expectedUserId = "user-1",
            )
        }

        assertEquals("WEBAUTHN_CREDENTIAL_SUBJECT_MISMATCH", error.errorCode)
        verify(credentials, never()).recordVerifiedAssertion(
            org.mockito.ArgumentMatchers.any() ?: fallbackVerifiedCommand()
        )
    }

    @Test
    fun `should consume failed verification so assertion cannot be replayed`() {
        val state = usernameLessState()
        `when`(store.consume(state.id)).thenReturn(state, null)
        val fallbackVerification = verificationRequest(state)
        `when`(verifier.verify(org.mockito.ArgumentMatchers.any() ?: fallbackVerification))
            .thenThrow(IllegalArgumentException("invalid signature"))
        val command = WebAuthnAssertionFinishCommand(state.id, "{}")

        val first = assertFailsWith<WebAuthnProviderException> { service.finish("tenant-1", command) }
        val replay = assertFailsWith<WebAuthnProviderException> { service.finish("tenant-1", command) }

        assertEquals("WEBAUTHN_ASSERTION_VERIFICATION_FAILED", first.errorCode)
        assertEquals("WEBAUTHN_CEREMONY_INVALID", replay.errorCode)
        verify(credentials, never()).recordVerifiedAssertion(
            org.mockito.ArgumentMatchers.any() ?: fallbackVerifiedCommand()
        )
    }

    @Test
    fun `should reject malformed ceremony id before store lookup`() {
        val error = assertFailsWith<WebAuthnProviderException> {
            service.finish("tenant-1", WebAuthnAssertionFinishCommand("not-a-server-id", "{}"))
        }

        assertEquals("WEBAUTHN_CEREMONY_INVALID", error.errorCode)
        verifyNoInteractions(store, accounts, credentials, verifier)
    }

    @Test
    fun `should reject ceremony bound to another authentication transaction`() {
        val state = fallbackState().copy(
            id = "A".repeat(43),
            bindingId = "tx-owner",
        )
        `when`(store.consume(state.id)).thenReturn(state)

        val error = assertFailsWith<WebAuthnProviderException> {
            service.finish(
                "tenant-1",
                WebAuthnAssertionFinishCommand(state.id, "{}"),
                expectedBindingId = "tx-attacker",
            )
        }

        assertEquals("WEBAUTHN_CEREMONY_INVALID", error.errorCode)
        verifyNoInteractions(accounts, credentials, verifier)
    }

    private fun usernameLessState(): WebAuthnCeremonyState {
        `when`(store.create(org.mockito.ArgumentMatchers.any() ?: fallbackState())).thenReturn(true)
        service.begin("tenant-1")
        val captor = ArgumentCaptor.forClass(WebAuthnCeremonyState::class.java)
        verify(store).create(captor.capture() ?: fallbackState())
        return captor.value
    }

    private fun activeAccount() {
        `when`(accounts.get("user-1")).thenReturn(UserAccount {
            id = "user-1"
            username = "alice"
            tenantId = "tenant-1"
            active = true
        })
    }

    private fun storedCredential(userId: String = "user-1") = AuthWebAuthnCredential {
        id = "row-1"
        tenantId = "tenant-1"
        this.userId = userId
        credentialId = encoded("credential-1")
        userHandle = WebAuthnUserHandles.derive("tenant-1", "user-1").base64Url
        publicKeyCose = encoded("public-key")
        signatureCount = 8
        transports = "internal"
        aaguid = AAGUID
        backupEligible = true
        backedUp = false
        discoverable = true
        displayName = "Alice passkey"
        createdAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC)
        version = 0
    }

    private fun summary() = WebAuthnCredentialSummary(
        id = "row-1",
        credentialId = encoded("credential-1"),
        transports = setOf("internal"),
        aaguid = null,
        backupEligible = true,
        backedUp = true,
        discoverable = true,
        displayName = "Alice passkey",
        createdAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC),
        lastUsedAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC),
    )

    private fun verifiedAssertion() = VerifiedWebAuthnAssertionResult(
        credentialId = encoded("credential-1"),
        userHandle = WebAuthnUserHandles.derive("tenant-1", "user-1").base64Url,
        username = "alice",
        signatureCount = 9,
        userVerified = true,
        backupEligible = true,
        backedUp = true,
    )

    private fun verificationRequest(state: WebAuthnCeremonyState) = WebAuthnAssertionVerificationRequest(
        request = AssertionRequest.fromJson(state.requestJson),
        credentialResponseJson = "{}",
        rpId = "example.com",
        rpName = "Kudos Test",
        origins = setOf("https://login.example.com"),
        credentialRepository = mock(com.yubico.webauthn.CredentialRepository::class.java),
        clock = Clock.fixed(now, ZoneOffset.UTC),
    )

    private fun fallbackVerifiedCommand() = VerifiedWebAuthnAssertion(
        tenantId = "fallback",
        userId = "fallback",
        credentialId = encoded("fallback"),
        signatureCount = 0,
        backedUp = false,
    )

    private fun fallbackEnforcementCommand(risk: WebAuthnAuthenticatorRiskSummary) =
        WebAuthnAuthenticatorRiskEnforcementCommand(
            tenantId = "fallback",
            userId = "fallback",
            credentialIdFingerprint = "A".repeat(43),
            risk = risk,
        )

    private fun fallbackState() = WebAuthnCeremonyState(
        id = "fallback",
        type = WebAuthnCeremonyTypeEnum.ASSERTION,
        tenantId = "fallback",
        userId = null,
        requestJson = "{}",
        createdAt = now,
        expiresAt = now.plusSeconds(1),
    )

    private fun validProperties() = WebAuthnProviderProperties().apply {
        enabled = true
        rpId = "example.com"
        rpName = "Kudos Test"
        origins = setOf("https://login.example.com")
    }

    private fun encoded(value: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray())

    private fun normalRisk() = WebAuthnAuthenticatorRiskSummary(
        level = WebAuthnAuthenticatorRiskLevelEnum.NORMAL,
        sources = setOf("FIDO_MDS"),
        statusCodes = setOf("FIDO_CERTIFIED_L2"),
    )

    private companion object {
        const val AAGUID = "00112233-4455-6677-8899-aabbccddeeff"
    }
}
