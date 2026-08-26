package io.kudos.ms.auth.provider.webauthn.authentication

import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionStatusEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationActionRequest
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationTransactionCreateRequest
import io.kudos.ms.auth.core.authentication.AuthenticationMethodRegistry
import io.kudos.ms.auth.core.authentication.assurance.DefaultAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.mfa.policy.AuthenticationMfaPolicyEnforcer
import io.kudos.ms.auth.core.authentication.mfa.policy.MfaPolicyEnforcementOutcomeEnum
import io.kudos.ms.auth.core.authentication.mfa.policy.MfaPolicyEnforcementResult
import io.kudos.ms.auth.core.authentication.service.impl.AuthenticationTransactionService
import io.kudos.ms.auth.core.authentication.store.InMemoryAuthenticationTransactionStore
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnAssertionFinish
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnAssertionFinishCommand
import io.kudos.ms.auth.provider.webauthn.service.WebAuthnAssertionService
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.WebAuthnCredentialSummary
import io.kudos.ms.auth.core.authentication.mfa.webauthn.service.iservice.IWebAuthnCredentialService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class WebAuthnAuthenticationMethodProviderTest {
    private val assertions = mock(WebAuthnAssertionService::class.java)

    @Test
    fun `transaction should complete passwordless passkey and retain no browser credential`() {
        val provider = WebAuthnAuthenticationMethodProvider(assertions)
        val service = AuthenticationTransactionService(
            InMemoryAuthenticationTransactionStore(),
            AuthenticationMethodRegistry(listOf(provider)),
        )
        val transaction = service.create(AuthenticationTransactionCreateRequest("tenant-1", "passkey"))
        val command = WebAuthnAssertionFinishCommand("A".repeat(43), "{\"response\":\"browser-owned\"}")
        `when`(assertions.finish("tenant-1", command, null, transaction.id)).thenReturn(finished())

        val result = service.act(
            transaction.id,
            AuthenticationActionEnum.VERIFY_PASSKEY,
            AuthenticationActionRequest(
                attributes = mapOf(
                    WebAuthnAuthenticationMethodProvider.ATTRIBUTE_CEREMONY_ID to command.ceremonyId,
                    WebAuthnAuthenticationMethodProvider.ATTRIBUTE_CREDENTIAL_RESPONSE to command.credentialResponseJson,
                )
            ),
        )

        assertEquals(AuthenticationTransactionStatusEnum.COMPLETED, result.status)
        assertEquals("user-1", result.userId)
        assertEquals("alice", result.username)
        assertEquals(setOf("passkey", "webauthn"), result.amr)
        assertEquals(DefaultAuthenticationAssurancePolicy.ACR_PHISHING_RESISTANT, result.acr)
        assertEquals(result.acr, result.context?.acr)
        assertFalse(result.toString().contains("browser-owned"))
    }

    @Test
    fun `failed assertion should return reusable generic challenge`() {
        val provider = WebAuthnAuthenticationMethodProvider(assertions)
        val transaction = transaction()
        val command = WebAuthnAssertionFinishCommand("A".repeat(43), "{}")
        `when`(assertions.finish("tenant-1", command, null, transaction.id))
            .thenThrow(IllegalArgumentException("WEBAUTHN_CREDENTIAL_SUBJECT_MISMATCH"))

        val result = provider.verify(transaction, AuthenticationActionEnum.VERIFY_PASSKEY, action(command))

        assertEquals(AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED, provider.begin(
            transaction,
            AuthenticationTransactionCreateRequest("tenant-1", "passkey"),
        ).status)
        assertEquals(io.kudos.ms.auth.core.authentication.spi.AuthenticationMethodOutcomeEnum.CHALLENGE, result.outcome)
        assertEquals(AuthenticationActionEnum.VERIFY_PASSKEY, result.nextAction)
        assertEquals("INVALID_PASSKEY", result.errorCode)
    }

    @Test
    fun `step-up should bind assertion to transaction and existing subject`() {
        val provider = WebAuthnAuthenticationMethodProvider(assertions)
        val transaction = transaction().copy(
            purpose = AuthenticationTransactionPurposeEnum.STEP_UP,
            initiatorUserId = "user-1",
            userId = "user-1",
        )
        val command = WebAuthnAssertionFinishCommand("A".repeat(43), "{}")
        `when`(assertions.finish("tenant-1", command, "user-1", transaction.id)).thenReturn(finished())

        provider.verify(transaction, AuthenticationActionEnum.VERIFY_PASSKEY, action(command))

        verify(assertions).finish("tenant-1", command, "user-1", transaction.id)
    }

    @Test
    fun `policy should fail closed when non-uv assertion still requires second factor`() {
        val enforcer = mock(AuthenticationMfaPolicyEnforcer::class.java)
        val provider = WebAuthnAuthenticationMethodProvider(assertions, enforcer)
        val transaction = transaction()
        val command = WebAuthnAssertionFinishCommand("A".repeat(43), "{}")
        `when`(assertions.finish("tenant-1", command, null, transaction.id))
            .thenReturn(finished(userVerified = false))
        `when`(
            enforcer.enforce(
                AuthenticationTransactionPurposeEnum.LOGIN,
                "tenant-1",
                "user-1",
                DefaultAuthenticationAssurancePolicy.ACR_WEBAUTHN,
            )
        ).thenReturn(MfaPolicyEnforcementResult(MfaPolicyEnforcementOutcomeEnum.REQUIRE_SECOND_FACTOR))

        val result = provider.verify(transaction, AuthenticationActionEnum.VERIFY_PASSKEY, action(command))

        assertEquals(io.kudos.ms.auth.core.authentication.spi.AuthenticationMethodOutcomeEnum.FAILURE, result.outcome)
        assertEquals("MFA_REQUIRED", result.errorCode)
        assertEquals(true, result.terminal)
    }

    @Test
    fun `pluggable second factor should require enrollment and pin subject plus transaction`() {
        val credentials = mock(IWebAuthnCredentialService::class.java)
        val provider = WebAuthnAuthenticationMethodProvider(
            assertionService = assertions,
            credentialService = credentials,
        )
        val command = WebAuthnAssertionFinishCommand("A".repeat(43), "{}")
        val request = action(command)
        `when`(credentials.isEnrolled("user-1", "tenant-1")).thenReturn(true)
        `when`(assertions.finish("tenant-1", command, "user-1", "tx-federated"))
            .thenReturn(finished(userVerified = false))

        val result = provider.verify("tx-federated", "user-1", "tenant-1", request)

        assertEquals(true, provider.isAvailable("user-1", "tenant-1"))
        assertEquals(true, result.success)
        assertEquals("webauthn", result.method)
        assertEquals(DefaultAuthenticationAssurancePolicy.ACR_MFA, result.acr)
        verify(assertions).finish("tenant-1", command, "user-1", "tx-federated")
    }

    private fun action(command: WebAuthnAssertionFinishCommand) = AuthenticationActionRequest(
        attributes = mapOf(
            WebAuthnAuthenticationMethodProvider.ATTRIBUTE_CEREMONY_ID to command.ceremonyId,
            WebAuthnAuthenticationMethodProvider.ATTRIBUTE_CREDENTIAL_RESPONSE to command.credentialResponseJson,
        )
    )

    private fun transaction() = AuthenticationTransaction(
        id = "tx-1",
        tenantId = "tenant-1",
        status = AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED,
        nextActions = setOf(AuthenticationActionEnum.VERIFY_PASSKEY),
        method = "passkey",
        createdAt = Instant.parse("2026-08-25T10:00:00Z"),
        updatedAt = Instant.parse("2026-08-25T10:00:00Z"),
        expiresAt = Instant.parse("2026-08-25T10:05:00Z"),
    )

    private fun finished(userVerified: Boolean = true) = WebAuthnAssertionFinish(
        userId = "user-1",
        username = "alice",
        credential = WebAuthnCredentialSummary(
            id = "row-1",
            credentialId = "Y3JlZGVudGlhbC0x",
            transports = setOf("internal"),
            aaguid = null,
            backupEligible = true,
            backedUp = true,
            discoverable = true,
            displayName = "Alice passkey",
            createdAt = LocalDateTime.parse("2026-08-25T09:00:00"),
            lastUsedAt = LocalDateTime.parse("2026-08-25T10:00:00"),
        ),
        userVerified = userVerified,
        backupEligible = true,
        backedUp = true,
    )
}
