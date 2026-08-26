package io.kudos.ms.auth.core.authentication.mfa.webauthn

import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.IWebAuthnAuthenticatorRiskEvaluator
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskAssessment
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskLevelEnum
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.service.impl.WebAuthnAuthenticatorRiskService
import io.kudos.ms.auth.core.authentication.mfa.webauthn.dao.AuthWebAuthnCredentialDao
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.VerifiedWebAuthnAssertion
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.VerifiedWebAuthnCredentialRegistration
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.WebAuthnCredentialException
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.po.AuthWebAuthnCredential
import io.kudos.ms.auth.core.authentication.mfa.webauthn.service.impl.WebAuthnCredentialService
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.event.UserAuthenticationInvalidated
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class WebAuthnCredentialServiceTest {
    private val now = Instant.parse("2026-08-25T10:00:00Z")
    private val dao = mock(AuthWebAuthnCredentialDao::class.java)
    private val accounts = mock(IUserAccountService::class.java)
    private val events = mock(ApplicationEventPublisher::class.java)
    private val service = WebAuthnCredentialService(dao, accounts, Clock.fixed(now, ZoneOffset.UTC), events)

    @Test
    fun verifiedRegistrationPersistsOnlyPublicMaterialAndNormalizesTransports() {
        account()

        val result = service.registerVerified(registration())

        val captor = ArgumentCaptor.forClass(AuthWebAuthnCredential::class.java)
        verify(dao).insert(captor.capture() ?: fallbackCredential())
        val stored = captor.value
        assertEquals("hybrid,internal,usb", stored.transports)
        assertEquals("Security key", stored.displayName)
        assertEquals(LocalDateTime.ofInstant(now, ZoneOffset.UTC), stored.createdAt)
        assertEquals(0, stored.version)
        assertEquals(setOf("hybrid", "internal", "usb"), result.transports)
        val eventCaptor = ArgumentCaptor.forClass(UserAuthenticationInvalidated::class.java)
        verify(events).publishEvent(eventCaptor.capture() ?: fallbackEvent())
        assertEquals(UserAuthenticationInvalidated.Reason.WEBAUTHN_CREDENTIAL_CHANGED, eventCaptor.value.reason)
    }

    @Test
    fun assertionRejectsCounterReplayBeforeStorageMutation() {
        `when`(dao.findActiveByCredentialId("t-1", encoded("credential-1")))
            .thenReturn(credential(signatureCount = 8))

        val error = assertFailsWith<WebAuthnCredentialException> {
            service.recordVerifiedAssertion(assertion(signatureCount = 8))
        }

        assertEquals("WEBAUTHN_SIGNATURE_COUNTER_REPLAY", error.errorCode)
    }

    @Test
    fun assertionUsesCounterCasAndReturnsUpdatedPublicSummary() {
        `when`(dao.findActiveByCredentialId("t-1", encoded("credential-1")))
            .thenReturn(credential(signatureCount = 8))
        `when`(
            dao.updateAssertionState(
                "t-1",
                "u-1",
                encoded("credential-1"),
                8,
                9,
                true,
                LocalDateTime.ofInstant(now, ZoneOffset.UTC),
            )
        ).thenReturn(true)

        val result = service.recordVerifiedAssertion(assertion(signatureCount = 9, backedUp = true))

        assertTrue(result.backedUp)
        assertEquals(LocalDateTime.ofInstant(now, ZoneOffset.UTC), result.lastUsedAt)
    }

    @Test
    fun assertionCannotUseCredentialOwnedByAnotherSubject() {
        `when`(dao.findActiveByCredentialId("t-1", encoded("credential-1")))
            .thenReturn(credential(userId = "u-2"))

        val error = assertFailsWith<WebAuthnCredentialException> {
            service.recordVerifiedAssertion(assertion(signatureCount = 9))
        }

        assertEquals("WEBAUTHN_CREDENTIAL_SUBJECT_MISMATCH", error.errorCode)
    }

    @Test
    fun userHandleLookupIsTenantScopedAndReturnsUniqueSubject() {
        val handle = encoded("user-handle-1")
        `when`(dao.findActiveByUserHandle("t-1", handle)).thenReturn(
            listOf(credential(), credential().apply { id = "credential-row-2" })
        )

        assertEquals("u-1", service.findActiveUserIdByUserHandle("t-1", handle))

        verify(dao).findActiveByUserHandle("t-1", handle)
    }

    @Test
    fun userHandleLookupFailsClosedWhenHandleMapsToMultipleSubjects() {
        val handle = encoded("user-handle-1")
        `when`(dao.findActiveByUserHandle("t-1", handle)).thenReturn(
            listOf(credential(userId = "u-1"), credential(userId = "u-2"))
        )

        val error = assertFailsWith<WebAuthnCredentialException> {
            service.findActiveUserIdByUserHandle("t-1", handle)
        }

        assertEquals("WEBAUTHN_USER_HANDLE_AMBIGUOUS", error.errorCode)
    }

    @Test
    fun auditListIncludesRevokedCredentialsWithoutVerificationMaterial() {
        val older = credential().apply {
            id = "wc-older"
            createdAt = LocalDateTime.ofInstant(now.minusSeconds(120), ZoneOffset.UTC)
        }
        val newerRevoked = credential().apply {
            id = "wc-newer"
            credentialId = encoded("credential-2")
            createdAt = LocalDateTime.ofInstant(now.minusSeconds(30), ZoneOffset.UTC)
            revokedAt = LocalDateTime.ofInstant(now.minusSeconds(10), ZoneOffset.UTC)
            attestationFormat = "packed"
        }
        `when`(dao.findByUser("t-1", "u-1")).thenReturn(listOf(older, newerRevoked))

        val result = service.listForAudit("t-1", "u-1")

        assertEquals(listOf("wc-newer", "wc-older"), result.map { it.id })
        assertEquals("packed", result.first().attestationFormat)
        assertEquals(newerRevoked.revokedAt, result.first().revokedAt)
        assertEquals(43, result.first().credentialIdFingerprint.length)
        assertEquals(WebAuthnAuthenticatorRiskLevelEnum.NOT_EVALUATED, result.first().authenticatorRiskLevel)
        assertTrue(result.first().authenticatorRiskSources.isEmpty())
        assertTrue(result.first().authenticatorRiskStatusCodes.isEmpty())
        val exposedNames = result.first()::class.java.declaredFields.map { it.name }.toSet()
        assertFalse("credentialId" in exposedNames)
        assertFalse("publicKeyCose" in exposedNames)
        assertFalse("userHandle" in exposedNames)
        assertFalse("signatureCount" in exposedNames)
    }

    @Test
    fun auditAggregatesAuthenticatorRiskFromAllConfiguredSources() {
        val credential = credential().apply { aaguid = "00000000-0000-0000-0000-000000000001" }
        `when`(dao.findByUser("t-1", "u-1")).thenReturn(listOf(credential))
        val riskService = serviceWithRiskEvaluators(
            evaluator("ENTERPRISE", WebAuthnAuthenticatorRiskLevelEnum.WARNING, "UPDATE_REQUIRED"),
            evaluator("FIDO_MDS", WebAuthnAuthenticatorRiskLevelEnum.CRITICAL, "REVOKED"),
        )

        val result = riskService.listForAudit("t-1", "u-1").single()

        assertEquals(WebAuthnAuthenticatorRiskLevelEnum.CRITICAL, result.authenticatorRiskLevel)
        assertEquals(setOf("ENTERPRISE", "FIDO_MDS"), result.authenticatorRiskSources)
        assertEquals(setOf("REVOKED", "UPDATE_REQUIRED"), result.authenticatorRiskStatusCodes)
    }

    @Test
    fun auditContainsEvaluatorFailureWithoutFailingTheReadOnlyReport() {
        val credential = credential().apply { aaguid = "00000000-0000-0000-0000-000000000001" }
        `when`(dao.findByUser("t-1", "u-1")).thenReturn(listOf(credential))
        val failing = object : IWebAuthnAuthenticatorRiskEvaluator {
            override val source = "UNAVAILABLE_SOURCE"
            override fun evaluate(aaguid: String): WebAuthnAuthenticatorRiskAssessment {
                throw IllegalStateException("metadata unavailable")
            }
        }

        val result = serviceWithRiskEvaluators(failing).listForAudit("t-1", "u-1").single()

        assertEquals(WebAuthnAuthenticatorRiskLevelEnum.NOT_EVALUATED, result.authenticatorRiskLevel)
        assertEquals(setOf("UNAVAILABLE_SOURCE"), result.authenticatorRiskSources)
        assertEquals(setOf("EVALUATION_FAILED"), result.authenticatorRiskStatusCodes)
    }

    @Test
    fun registrationRejectsAccountFromAnotherTenant() {
        `when`(accounts.get("u-1")).thenReturn(UserAccount {
            id = "u-1"
            tenantId = "t-2"
            username = "alice"
        })

        val error = assertFailsWith<WebAuthnCredentialException> {
            service.registerVerified(registration())
        }

        assertEquals("WEBAUTHN_ACCOUNT_TENANT_MISMATCH", error.errorCode)
    }

    @Test
    fun successfulRevocationPublishesCredentialLifecycleInvalidation() {
        `when`(dao.revoke("t-1", "u-1", encoded("credential-1"), LocalDateTime.ofInstant(now, ZoneOffset.UTC)))
            .thenReturn(true)

        assertTrue(service.revoke("t-1", "u-1", encoded("credential-1")))

        val eventCaptor = ArgumentCaptor.forClass(UserAuthenticationInvalidated::class.java)
        verify(events).publishEvent(eventCaptor.capture() ?: fallbackEvent())
        assertEquals("u-1", eventCaptor.value.id)
        assertEquals("t-1", eventCaptor.value.tenantId)
        assertEquals(UserAuthenticationInvalidated.Reason.WEBAUTHN_CREDENTIAL_CHANGED, eventCaptor.value.reason)
    }

    @Test
    fun renameIsOwnerScopedNormalizesNameAndDoesNotInvalidateAuthentication() {
        val credential = credential()
        `when`(dao.findActiveByCredentialId("t-1", encoded("credential-1"))).thenReturn(credential)
        `when`(dao.rename("t-1", "u-1", encoded("credential-1"), "Work laptop")).thenReturn(true)

        val result = service.rename("t-1", "u-1", encoded("credential-1"), "  Work laptop  ")

        assertEquals("Work laptop", result.displayName)
        verify(dao).rename("t-1", "u-1", encoded("credential-1"), "Work laptop")
        verifyNoInteractions(events)
    }

    @Test
    fun renameHidesCredentialOwnedByAnotherUser() {
        `when`(dao.findActiveByCredentialId("t-1", encoded("credential-1")))
            .thenReturn(credential(userId = "u-2"))

        val error = assertFailsWith<WebAuthnCredentialException> {
            service.rename("t-1", "u-1", encoded("credential-1"), "Work laptop")
        }

        assertEquals("WEBAUTHN_CREDENTIAL_NOT_FOUND", error.errorCode)
    }

    @Test
    fun renameFailsClosedWhenCredentialChangesConcurrently() {
        `when`(dao.findActiveByCredentialId("t-1", encoded("credential-1"))).thenReturn(credential())
        `when`(dao.rename("t-1", "u-1", encoded("credential-1"), "Work laptop")).thenReturn(false)

        val error = assertFailsWith<WebAuthnCredentialException> {
            service.rename("t-1", "u-1", encoded("credential-1"), "Work laptop")
        }

        assertEquals("WEBAUTHN_CREDENTIAL_CONCURRENTLY_CHANGED", error.errorCode)
        verifyNoInteractions(events)
    }

    @Test
    fun renameRejectsControlCharactersBeforeStorageLookup() {
        val error = assertFailsWith<WebAuthnCredentialException> {
            service.rename("t-1", "u-1", encoded("credential-1"), "Laptop\nInjected")
        }

        assertEquals("WEBAUTHN_DISPLAY_NAME_INVALID", error.errorCode)
        verifyNoInteractions(dao)
    }

    private fun account() {
        `when`(accounts.get("u-1")).thenReturn(UserAccount {
            id = "u-1"
            tenantId = "t-1"
            username = "alice"
        })
    }

    private fun serviceWithRiskEvaluators(
        vararg evaluators: IWebAuthnAuthenticatorRiskEvaluator,
    ) = WebAuthnCredentialService(
        dao = dao,
        userAccountService = accounts,
        clock = Clock.fixed(now, ZoneOffset.UTC),
        eventPublisher = events,
        authenticatorRiskService = WebAuthnAuthenticatorRiskService(evaluators.toList()),
    )

    private fun evaluator(
        source: String,
        level: WebAuthnAuthenticatorRiskLevelEnum,
        vararg statuses: String,
    ) = object : IWebAuthnAuthenticatorRiskEvaluator {
        override val source = source
        override fun evaluate(aaguid: String) = WebAuthnAuthenticatorRiskAssessment(level, statuses.toSet())
    }

    private fun registration() = VerifiedWebAuthnCredentialRegistration(
        tenantId = "t-1",
        userId = "u-1",
        credentialId = encoded("credential-1"),
        userHandle = encoded("user-handle-1"),
        publicKeyCose = encoded("cose-public-key"),
        signatureCount = 0,
        transports = setOf(" USB ", "internal", "HYBRID"),
        backupEligible = true,
        backedUp = false,
        discoverable = true,
        displayName = " Security key ",
    )

    private fun assertion(signatureCount: Long, backedUp: Boolean = false) = VerifiedWebAuthnAssertion(
        tenantId = "t-1",
        userId = "u-1",
        credentialId = encoded("credential-1"),
        signatureCount = signatureCount,
        backedUp = backedUp,
    )

    private fun credential(userId: String = "u-1", signatureCount: Long = 8) = AuthWebAuthnCredential {
        id = "wc-1"
        tenantId = "t-1"
        this.userId = userId
        credentialId = encoded("credential-1")
        userHandle = encoded("user-handle-1")
        publicKeyCose = encoded("cose-public-key")
        this.signatureCount = signatureCount
        transports = "internal"
        aaguid = null
        attestationFormat = null
        backupEligible = true
        backedUp = false
        discoverable = true
        displayName = "Security key"
        createdAt = LocalDateTime.ofInstant(now.minusSeconds(60), ZoneOffset.UTC)
        lastUsedAt = null
        revokedAt = null
        version = 0
    }

    private fun fallbackCredential() = credential()

    private fun fallbackEvent() = UserAuthenticationInvalidated(
        "fallback",
        "fallback",
        UserAuthenticationInvalidated.Reason.WEBAUTHN_CREDENTIAL_CHANGED,
    )

    private fun encoded(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())

}
