package io.kudos.ms.auth.provider.webauthn.service

import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.EffectiveWebAuthnAttestationPolicy
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.WebAuthnAttestationPolicyException
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.service.iservice.IWebAuthnAttestationPolicyService
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.VerifiedWebAuthnCredentialRegistration
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.WebAuthnCredentialSummary
import io.kudos.ms.auth.core.authentication.mfa.webauthn.service.iservice.IWebAuthnCredentialService
import io.kudos.ms.auth.provider.webauthn.WebAuthnProviderProperties
import io.kudos.ms.auth.provider.webauthn.WebAuthnProviderConfiguration
import io.kudos.ms.auth.provider.webauthn.ceremony.IWebAuthnCeremonyStore
import io.kudos.ms.auth.provider.webauthn.ceremony.WebAuthnCeremonyState
import io.kudos.ms.auth.provider.webauthn.ceremony.WebAuthnCeremonyTypeEnum
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnProviderException
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnRegistrationFinishCommand
import io.kudos.ms.auth.provider.webauthn.protocol.IWebAuthnRegistrationVerifier
import io.kudos.ms.auth.provider.webauthn.protocol.VerifiedWebAuthnRegistrationResult
import io.kudos.ms.auth.provider.webauthn.protocol.WebAuthnRegistrationVerificationRequest
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.never
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class WebAuthnRegistrationServiceTest {
    private val now = Instant.parse("2026-08-25T10:00:00Z")
    private val accounts = mock(IUserAccountService::class.java)
    private val credentials = mock(IWebAuthnCredentialService::class.java)
    private val store = mock(IWebAuthnCeremonyStore::class.java)
    private val verifier = mock(IWebAuthnRegistrationVerifier::class.java)
    private val properties = validProperties()
    private val service = WebAuthnRegistrationService(
        accounts,
        credentials,
        store,
        verifier,
        WebAuthnProviderConfiguration(properties),
        Clock.fixed(now, ZoneOffset.UTC),
    )

    @Test
    fun `should start registration with server controlled subject and rp configuration`() {
        activeAccount()
        `when`(credentials.listActive("tenant-1", "user-1")).thenReturn(listOf(credential()))
        `when`(store.create(org.mockito.ArgumentMatchers.any() ?: fallbackState())).thenReturn(true)

        val result = service.begin("user-1", "tenant-1")

        val captor = ArgumentCaptor.forClass(WebAuthnCeremonyState::class.java)
        verify(store).create(captor.capture() ?: fallbackState())
        val state = captor.value
        assertEquals(result.ceremonyId, state.id)
        assertEquals(WebAuthnCeremonyTypeEnum.REGISTRATION, state.type)
        assertEquals("tenant-1", state.tenantId)
        assertEquals("user-1", state.userId)
        assertEquals(now, state.createdAt)
        assertEquals(now.plusSeconds(300), state.expiresAt)
        assertEquals(state.expiresAt, result.expiresAt)
        assertTrue(state.requestJson.contains("example.com"))
        assertTrue(result.publicKeyCredentialCreationOptions.contains("Kudos Test"))
        assertTrue(result.publicKeyCredentialCreationOptions.contains("alice"))
        assertTrue(result.publicKeyCredentialCreationOptions.contains(encoded("credential-1")))
        assertTrue(result.publicKeyCredentialCreationOptions.contains("\"attestation\":\"none\""))
    }

    @Test
    fun `should request direct attestation only when tenant admission policy needs authenticator evidence`() {
        val policy = mock(IWebAuthnAttestationPolicyService::class.java)
        val policyAwareService = WebAuthnRegistrationService(
            accounts,
            credentials,
            store,
            verifier,
            WebAuthnProviderConfiguration(properties),
            Clock.fixed(now, ZoneOffset.UTC),
            attestationPolicyService = policy,
        )
        activeAccount()
        `when`(credentials.listActive("tenant-1", "user-1")).thenReturn(emptyList())
        `when`(store.create(org.mockito.ArgumentMatchers.any() ?: fallbackState())).thenReturn(true)
        `when`(policy.getEffective("tenant-1")).thenReturn(
            EffectiveWebAuthnAttestationPolicy(
                tenantId = "tenant-1",
                requireTrustedAttestation = true,
                configured = true,
            )
        )

        val result = policyAwareService.begin("user-1", "tenant-1")

        assertTrue(result.publicKeyCredentialCreationOptions.contains("\"attestation\":\"direct\""))
    }

    @Test
    fun `should reject origin with path before creating ceremony`() {
        properties.origins = setOf("https://login.example.com/callback")

        val error = assertFailsWith<WebAuthnProviderException> {
            service.begin("user-1", "tenant-1")
        }

        assertEquals("WEBAUTHN_ORIGIN_INVALID", error.errorCode)
        verifyNoInteractions(accounts, credentials, store)
    }

    @Test
    fun `should reject origin outside configured rp id`() {
        properties.origins = setOf("https://login.attacker.example.net")

        val error = assertFailsWith<WebAuthnProviderException> {
            service.begin("user-1", "tenant-1")
        }

        assertEquals("WEBAUTHN_ORIGIN_RP_MISMATCH", error.errorCode)
        verifyNoInteractions(accounts, credentials, store)
    }

    @Test
    fun `should reject account owned by another tenant`() {
        activeAccount(tenantId = "tenant-2")

        val error = assertFailsWith<WebAuthnProviderException> {
            service.begin("user-1", "tenant-1")
        }

        assertEquals("WEBAUTHN_ACCOUNT_NOT_FOUND", error.errorCode)
        verifyNoInteractions(credentials, store)
    }

    @Test
    fun `should verify consumed ceremony and persist only verified public material`() {
        activeAccount()
        `when`(credentials.listActive("tenant-1", "user-1")).thenReturn(emptyList())
        `when`(store.create(org.mockito.ArgumentMatchers.any() ?: fallbackState())).thenReturn(true)
        val start = service.begin("user-1", "tenant-1")
        val stateCaptor = ArgumentCaptor.forClass(WebAuthnCeremonyState::class.java)
        verify(store).create(stateCaptor.capture() ?: fallbackState())
        val state = stateCaptor.value
        `when`(store.consume(start.ceremonyId)).thenReturn(state)
        val fallbackVerification = verificationRequest(state)
        `when`(verifier.verify(org.mockito.ArgumentMatchers.any() ?: fallbackVerification))
            .thenReturn(verifiedRegistration())
        `when`(credentials.findActive("tenant-1", encoded("credential-new"))).thenReturn(null)
        `when`(
            credentials.registerVerified(
                org.mockito.ArgumentMatchers.any() ?: fallbackVerifiedCommand()
            )
        ).thenReturn(credential(credentialId = encoded("credential-new")))

        val result = service.finish(
            "user-1",
            "tenant-1",
            WebAuthnRegistrationFinishCommand(start.ceremonyId, "{}", "Alice passkey"),
        )

        val verificationCaptor = ArgumentCaptor.forClass(WebAuthnRegistrationVerificationRequest::class.java)
        verify(verifier).verify(verificationCaptor.capture() ?: fallbackVerification)
        assertEquals("example.com", verificationCaptor.value.rpId)
        assertEquals(setOf("https://login.example.com"), verificationCaptor.value.origins)
        assertEquals(state.requestJson, verificationCaptor.value.request.toJson())
        val commandCaptor = ArgumentCaptor.forClass(VerifiedWebAuthnCredentialRegistration::class.java)
        verify(credentials).registerVerified(commandCaptor.capture() ?: fallbackVerifiedCommand())
        val persisted = commandCaptor.value
        assertEquals("tenant-1", persisted.tenantId)
        assertEquals("user-1", persisted.userId)
        assertEquals(encoded("credential-new"), persisted.credentialId)
        assertEquals(verificationCaptor.value.request.user.id.base64Url, persisted.userHandle)
        assertEquals(encoded("cose-key"), persisted.publicKeyCose)
        assertEquals(setOf("hybrid", "internal"), persisted.transports)
        assertEquals("Alice passkey", persisted.displayName)
        assertTrue(result.userVerified)
        assertEquals(encoded("credential-new"), result.credential.credentialId)
    }

    @Test
    fun `should consume failed verification so response cannot be replayed`() {
        activeAccount()
        `when`(credentials.listActive("tenant-1", "user-1")).thenReturn(emptyList())
        `when`(store.create(org.mockito.ArgumentMatchers.any() ?: fallbackState())).thenReturn(true)
        val start = service.begin("user-1", "tenant-1")
        val stateCaptor = ArgumentCaptor.forClass(WebAuthnCeremonyState::class.java)
        verify(store).create(stateCaptor.capture() ?: fallbackState())
        val state = stateCaptor.value
        `when`(store.consume(start.ceremonyId)).thenReturn(state, null)
        val fallbackVerification = verificationRequest(state)
        `when`(verifier.verify(org.mockito.ArgumentMatchers.any() ?: fallbackVerification))
            .thenThrow(IllegalArgumentException("invalid challenge"))
        val command = WebAuthnRegistrationFinishCommand(start.ceremonyId, "{}", "Alice passkey")

        val first = assertFailsWith<WebAuthnProviderException> {
            service.finish("user-1", "tenant-1", command)
        }
        val replay = assertFailsWith<WebAuthnProviderException> {
            service.finish("user-1", "tenant-1", command)
        }

        assertEquals("WEBAUTHN_REGISTRATION_VERIFICATION_FAILED", first.errorCode)
        assertEquals("WEBAUTHN_CEREMONY_INVALID", replay.errorCode)
        verify(credentials, never()).registerVerified(
            org.mockito.ArgumentMatchers.any() ?: fallbackVerifiedCommand()
        )
    }

    @Test
    fun `should enforce tenant attestation policy after protocol verification before persistence`() {
        val policy = mock(IWebAuthnAttestationPolicyService::class.java)
        val constrainedService = WebAuthnRegistrationService(
            userAccountService = accounts,
            credentialService = credentials,
            ceremonyStore = store,
            registrationVerifier = verifier,
            providerConfiguration = WebAuthnProviderConfiguration(properties),
            clock = Clock.fixed(now, ZoneOffset.UTC),
            attestationPolicyService = policy,
        )
        activeAccount()
        `when`(credentials.listActive("tenant-1", "user-1")).thenReturn(emptyList())
        `when`(store.create(org.mockito.ArgumentMatchers.any() ?: fallbackState())).thenReturn(true)
        val start = constrainedService.begin("user-1", "tenant-1")
        val stateCaptor = ArgumentCaptor.forClass(WebAuthnCeremonyState::class.java)
        verify(store).create(stateCaptor.capture() ?: fallbackState())
        `when`(store.consume(start.ceremonyId)).thenReturn(stateCaptor.value)
        val fallbackVerification = verificationRequest(stateCaptor.value)
        `when`(verifier.verify(org.mockito.ArgumentMatchers.any() ?: fallbackVerification))
            .thenReturn(verifiedRegistration())
        org.mockito.Mockito.doThrow(WebAuthnAttestationPolicyException("WEBAUTHN_AAGUID_NOT_ALLOWED"))
            .`when`(policy).enforce(
                "tenant-1",
                "00000000-0000-0000-0000-000000000000",
                "none",
                false,
            )

        val error = assertFailsWith<WebAuthnProviderException> {
            constrainedService.finish(
                "user-1",
                "tenant-1",
                WebAuthnRegistrationFinishCommand(start.ceremonyId, "{}", "Alice passkey"),
            )
        }

        assertEquals("WEBAUTHN_AAGUID_NOT_ALLOWED", error.errorCode)
        verify(credentials, never()).registerVerified(
            org.mockito.ArgumentMatchers.any() ?: fallbackVerifiedCommand()
        )
    }

    @Test
    fun `should reject ceremony bound to another subject after consuming it`() {
        val ceremonyId = "A".repeat(43)
        `when`(store.consume(ceremonyId)).thenReturn(
            fallbackState().copy(id = ceremonyId, userId = "user-2")
        )

        val error = assertFailsWith<WebAuthnProviderException> {
            service.finish(
                "user-1",
                "tenant-1",
                WebAuthnRegistrationFinishCommand(ceremonyId, "{}", "Alice passkey"),
            )
        }

        assertEquals("WEBAUTHN_CEREMONY_INVALID", error.errorCode)
        verifyNoInteractions(accounts, credentials, verifier)
    }

    @Test
    fun `should reject malformed ceremony id before store lookup`() {
        val error = assertFailsWith<WebAuthnProviderException> {
            service.finish(
                "user-1",
                "tenant-1",
                WebAuthnRegistrationFinishCommand("not-a-server-id", "{}", "Alice passkey"),
            )
        }

        assertEquals("WEBAUTHN_CEREMONY_INVALID", error.errorCode)
        verifyNoInteractions(store, accounts, credentials, verifier)
    }

    private fun activeAccount(tenantId: String = "tenant-1") {
        `when`(accounts.get("user-1")).thenReturn(UserAccount {
            id = "user-1"
            username = "alice"
            this.tenantId = tenantId
            active = true
        })
    }

    private fun credential(credentialId: String = encoded("credential-1")) = WebAuthnCredentialSummary(
        id = "row-1",
        credentialId = credentialId,
        transports = setOf("internal"),
        aaguid = null,
        backupEligible = false,
        backedUp = false,
        discoverable = false,
        displayName = "Platform authenticator",
        createdAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC),
        lastUsedAt = null,
    )

    private fun validProperties() = WebAuthnProviderProperties().apply {
        enabled = true
        rpId = "example.com"
        rpName = "Kudos Test"
        origins = setOf("https://login.example.com")
    }

    private fun encoded(value: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray())

    private fun fallbackState() = WebAuthnCeremonyState(
        id = "fallback",
        type = WebAuthnCeremonyTypeEnum.REGISTRATION,
        tenantId = "fallback",
        userId = "fallback",
        requestJson = "{}",
        createdAt = now,
        expiresAt = now.plusSeconds(1),
    )

    private fun verificationRequest(state: WebAuthnCeremonyState) = WebAuthnRegistrationVerificationRequest(
        request = PublicKeyCredentialCreationOptions.fromJson(state.requestJson),
        credentialResponseJson = "{}",
        rpId = "example.com",
        rpName = "Kudos Test",
        origins = setOf("https://login.example.com"),
        credentialRepository = mock(com.yubico.webauthn.CredentialRepository::class.java),
        clock = Clock.fixed(now, ZoneOffset.UTC),
    )

    private fun verifiedRegistration() = VerifiedWebAuthnRegistrationResult(
        credentialId = encoded("credential-new"),
        publicKeyCose = encoded("cose-key"),
        signatureCount = 0,
        transports = setOf("internal", "hybrid"),
        aaguid = "00000000-0000-0000-0000-000000000000",
        attestationFormat = "none",
        backupEligible = true,
        backedUp = true,
        discoverable = true,
        userVerified = true,
        attestationTrusted = false,
    )

    private fun fallbackVerifiedCommand() = VerifiedWebAuthnCredentialRegistration(
        tenantId = "fallback",
        userId = "fallback",
        credentialId = encoded("fallback-credential"),
        userHandle = encoded("fallback-handle"),
        publicKeyCose = encoded("fallback-key"),
        signatureCount = 0,
        displayName = "fallback",
    )
}
