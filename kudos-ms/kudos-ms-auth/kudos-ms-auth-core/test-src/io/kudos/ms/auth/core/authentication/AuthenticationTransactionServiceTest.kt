package io.kudos.ms.auth.core.authentication

import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionStatusEnum
import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaMethodEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationActionRequest
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationTransactionCreateRequest
import io.kudos.ms.auth.core.authentication.service.impl.AuthenticationTransactionService
import io.kudos.ms.auth.core.authentication.model.AuthenticationStepUpCreateCommand
import io.kudos.ms.auth.core.authentication.assurance.DefaultAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.mfa.policy.AuthenticationMfaPolicyEnforcer
import io.kudos.ms.auth.core.authentication.mfa.policy.MfaPolicyEnforcementOutcomeEnum
import io.kudos.ms.auth.core.authentication.mfa.policy.MfaPolicyEnforcementResult
import io.kudos.ms.auth.core.authentication.mfa.FederatedSecondFactorVerifier
import io.kudos.ms.auth.core.authentication.mfa.AuthenticationSecondFactorRegistry
import io.kudos.ms.auth.core.authentication.mfa.IAuthenticationSecondFactorProvider
import io.kudos.ms.auth.core.authentication.mfa.SecondFactorVerificationResult
import io.kudos.ms.auth.core.authentication.mfa.policy.model.EffectiveTenantMfaPolicy
import io.kudos.ms.auth.core.authentication.mfa.policy.model.MfaPolicyDecision
import io.kudos.ms.auth.core.authentication.spi.AuthenticationChallenge
import io.kudos.ms.auth.core.authentication.spi.AuthenticationMethodOutcomeEnum
import io.kudos.ms.auth.core.authentication.spi.AuthenticationMethodResult
import io.kudos.ms.auth.core.authentication.spi.IAuthenticationMethodProvider
import io.kudos.ms.auth.core.authentication.store.InMemoryAuthenticationTransactionStore
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pure tests for authentication transaction orchestration and optimistic storage. */
internal class AuthenticationTransactionServiceTest {

    private val provider = object : IAuthenticationMethodProvider {
        override fun method() = "test"

        override fun begin(
            transaction: AuthenticationTransaction,
            request: AuthenticationTransactionCreateRequest,
        ) = if (transaction.tenantId == null) {
            AuthenticationChallenge(
                AuthenticationTransactionStatusEnum.WAITING_FOR_ACTION,
                AuthenticationActionEnum.SELECT_TENANT,
            )
        } else {
            AuthenticationChallenge(
                AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED,
                AuthenticationActionEnum.VERIFY_PASSWORD,
            )
        }

        override fun verify(
            transaction: AuthenticationTransaction,
            action: AuthenticationActionEnum,
            request: AuthenticationActionRequest,
        ) = if (request.plainPassword == "correct" || request.plainPassword == "enrollment") {
            AuthenticationMethodResult(
                outcome = AuthenticationMethodOutcomeEnum.SUCCESS,
                userId = "u-1",
                tenantId = transaction.tenantId,
                username = request.username,
                amr = setOf("test"),
                acr = "urn:test:acr",
                postAuthenticationActions = if (request.plainPassword == "enrollment") {
                    setOf(AuthenticationActionEnum.ENROLL_MFA)
                } else {
                    emptySet()
                },
            )
        } else if (request.plainPassword == "passkey-alternative") {
            AuthenticationMethodResult(
                outcome = AuthenticationMethodOutcomeEnum.CHALLENGE,
                nextAction = AuthenticationActionEnum.VERIFY_PASSKEY,
                userId = "u-1",
                tenantId = transaction.tenantId,
                username = request.username,
                amr = setOf("test-primary"),
                acr = "urn:kudos:acr:password",
            )
        } else if (request.plainPassword == "alternatives") {
            AuthenticationMethodResult(
                outcome = AuthenticationMethodOutcomeEnum.CHALLENGE,
                nextAction = AuthenticationActionEnum.VERIFY_TOTP,
                nextActions = setOf(
                    AuthenticationActionEnum.VERIFY_TOTP,
                    AuthenticationActionEnum.VERIFY_RECOVERY_CODE,
                ),
                username = request.username,
            )
        } else {
            AuthenticationMethodResult(
                outcome = AuthenticationMethodOutcomeEnum.CHALLENGE,
                nextAction = AuthenticationActionEnum.VERIFY_PASSWORD,
                username = request.username,
                errorCode = "INVALID_CREDENTIALS",
            )
        }
    }

    private val store = InMemoryAuthenticationTransactionStore()
    private val service = AuthenticationTransactionService(store, AuthenticationMethodRegistry(listOf(provider)))

    @Test
    fun create_withoutMethod_waitsForMethodSelection() {
        val transaction = service.create(AuthenticationTransactionCreateRequest(tenantId = "t-1"))

        assertEquals(AuthenticationTransactionStatusEnum.WAITING_FOR_ACTION, transaction.status)
        assertEquals(setOf(AuthenticationActionEnum.SELECT_METHOD), transaction.nextActions)
        assertNull(transaction.method)
        assertTrue(transaction.expiresAt.isAfter(transaction.createdAt))
    }

    @Test
    fun create_withMethod_thenVerify_completesWithUnifiedContext() {
        val created = service.create(AuthenticationTransactionCreateRequest("t-1", "TEST"))
        assertEquals(AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED, created.status)
        assertEquals(setOf(AuthenticationActionEnum.VERIFY_PASSWORD), created.nextActions)
        assertEquals("test", created.method)

        val completed = service.act(
            created.id,
            AuthenticationActionEnum.VERIFY_PASSWORD,
            AuthenticationActionRequest(username = "alice", plainPassword = "correct"),
        )

        assertEquals(AuthenticationTransactionStatusEnum.COMPLETED, completed.status)
        assertEquals("u-1", completed.userId)
        assertEquals(setOf("test"), completed.amr)
        val context = assertNotNull(completed.context)
        assertEquals("t-1", context.tenantId)
        assertEquals("urn:test:acr", context.acr)
        assertNull(context.sessionId)
        assertEquals(1, completed.version)
    }

    @Test
    fun methodChallengeCanExposeMultipleExplicitlyAllowedActions() {
        val created = service.create(AuthenticationTransactionCreateRequest("t-1", "test"))

        val challenged = service.act(
            created.id,
            AuthenticationActionEnum.VERIFY_PASSWORD,
            AuthenticationActionRequest(username = "alice", plainPassword = "alternatives"),
        )

        assertEquals(
            setOf(AuthenticationActionEnum.VERIFY_TOTP, AuthenticationActionEnum.VERIFY_RECOVERY_CODE),
            challenged.nextActions,
        )
    }

    @Test
    fun completedTransactionPreservesSecretFreePostAuthenticationActions() {
        val created = service.create(AuthenticationTransactionCreateRequest("t-1", "test"))

        val completed = service.act(
            created.id,
            AuthenticationActionEnum.VERIFY_PASSWORD,
            AuthenticationActionRequest(username = "alice", plainPassword = "enrollment"),
        )

        assertEquals(AuthenticationTransactionStatusEnum.COMPLETED, completed.status)
        assertEquals(setOf(AuthenticationActionEnum.ENROLL_MFA), completed.postAuthenticationActions)
    }

    @Test
    fun stepUp_pinsInitiatorSourceSessionAndRequiredAcr() {
        val created = service.createStepUp(
            AuthenticationStepUpCreateCommand(
                userId = "u-1",
                tenantId = "t-1",
                username = "alice",
                sourceSessionId = "session-1",
                requiredAcr = "urn:test:acr",
                requestedMethod = "test",
            )
        )

        assertEquals(AuthenticationTransactionPurposeEnum.STEP_UP, created.purpose)
        assertEquals("u-1", created.initiatorUserId)
        assertEquals("session-1", created.sourceSessionId)
        assertEquals("urn:test:acr", created.requiredAcr)
        assertEquals(setOf(AuthenticationActionEnum.VERIFY_PASSWORD), created.nextActions)

        val completed = service.act(
            created.id,
            AuthenticationActionEnum.VERIFY_PASSWORD,
            AuthenticationActionRequest(username = "alice", plainPassword = "correct"),
        )

        assertEquals(AuthenticationTransactionStatusEnum.COMPLETED, completed.status)
        assertEquals("urn:test:acr", completed.context?.acr)
    }

    @Test
    fun stepUp_rejectsAuthenticatedSubjectSwap() {
        val created = service.createStepUp(
            AuthenticationStepUpCreateCommand(
                userId = "u-other",
                tenantId = "t-1",
                username = "other",
                sourceSessionId = "session-1",
                requiredAcr = "urn:test:acr",
                requestedMethod = "test",
            )
        )

        val failed = service.act(
            created.id,
            AuthenticationActionEnum.VERIFY_PASSWORD,
            AuthenticationActionRequest(username = "alice", plainPassword = "correct"),
        )

        assertEquals(AuthenticationTransactionStatusEnum.FAILED, failed.status)
        assertEquals("STEP_UP_SUBJECT_MISMATCH", failed.errorCode)
        assertNull(failed.context)
    }

    @Test
    fun stepUp_failsClosedWhenMethodDoesNotReachRequiredAcr() {
        val created = service.createStepUp(
            AuthenticationStepUpCreateCommand(
                userId = "u-1",
                tenantId = "t-1",
                username = "alice",
                sourceSessionId = "session-1",
                requiredAcr = "urn:kudos:acr:mfa",
                requestedMethod = "test",
            )
        )

        val failed = service.act(
            created.id,
            AuthenticationActionEnum.VERIFY_PASSWORD,
            AuthenticationActionRequest(username = "alice", plainPassword = "correct"),
        )

        assertEquals(AuthenticationTransactionStatusEnum.FAILED, failed.status)
        assertEquals("REQUIRED_ACR_NOT_SATISFIED", failed.errorCode)
        assertNull(failed.context)
    }

    @Test
    fun failedCredential_remainsRetryableWithoutPersistingSecret() {
        val created = service.create(AuthenticationTransactionCreateRequest("t-1", "test"))

        val challenged = service.act(
            created.id,
            AuthenticationActionEnum.VERIFY_PASSWORD,
            AuthenticationActionRequest(username = "alice", plainPassword = "wrong"),
        )

        assertEquals(AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED, challenged.status)
        assertEquals("INVALID_CREDENTIALS", challenged.errorCode)
        assertEquals("alice", challenged.username)
        assertEquals(setOf(AuthenticationActionEnum.VERIFY_PASSWORD), challenged.nextActions)
        // The public transaction has no field capable of retaining the password.
        assertTrue(challenged.toString().contains("wrong").not())
    }

    @Test
    fun missingTenant_isCollectedBeforeMethodChallenge() {
        val created = service.create(AuthenticationTransactionCreateRequest(requestedMethod = "test"))
        assertEquals(setOf(AuthenticationActionEnum.SELECT_TENANT), created.nextActions)

        val selected = service.act(
            created.id,
            AuthenticationActionEnum.SELECT_TENANT,
            AuthenticationActionRequest(tenantId = "t-selected"),
        )

        assertEquals("t-selected", selected.tenantId)
        assertEquals(setOf(AuthenticationActionEnum.VERIFY_PASSWORD), selected.nextActions)
    }

    @Test
    fun bindSession_versionsCompletedTransaction() {
        val created = service.create(AuthenticationTransactionCreateRequest("t-1", "test"))
        val completed = service.act(
            created.id,
            AuthenticationActionEnum.VERIFY_PASSWORD,
            AuthenticationActionRequest(username = "alice", plainPassword = "correct"),
        )

        val bound = service.bindSession(completed.id, "session-1")

        assertEquals("session-1", bound.context?.sessionId)
        assertEquals(completed.version + 1, bound.version)
    }

    @Test
    fun unsupportedRequestedMethod_createsTerminalFailure() {
        val transaction = service.create(AuthenticationTransactionCreateRequest("t-1", "missing"))

        assertEquals(AuthenticationTransactionStatusEnum.FAILED, transaction.status)
        assertEquals("UNSUPPORTED_AUTHENTICATION_METHOD", transaction.errorCode)
        assertTrue(transaction.nextActions.isEmpty())
    }

    @Test
    fun store_rejectsStaleVersion() {
        val created = service.create(AuthenticationTransactionCreateRequest("t-1"))
        val first = store.save(created.copy(errorCode = "first"), created.version)
        assertNotNull(first)

        val stale = store.save(created.copy(errorCode = "stale"), created.version)

        assertNull(stale)
        assertEquals("first", store.get(created.id)?.errorCode)
    }

    @Test
    fun externalProviderFlow_isTenantBoundAndProducesFederatedContext() {
        val created = service.create(AuthenticationTransactionCreateRequest("t-1"))
        val prepared = service.prepareExternal(created.id, "provider-1", "t-1", "invitation-1")

        assertEquals("external:provider-1", prepared.method)
        assertEquals("invitation-1", prepared.externalInvitationId)
        assertEquals(setOf(AuthenticationActionEnum.REDIRECT_EXTERNAL_PROVIDER), prepared.nextActions)

        val completed = service.completeExternal(
            prepared.id,
            "provider-1",
            "u-1",
            "t-1",
            "alice",
            "GOOGLE",
        )

        assertEquals(AuthenticationTransactionStatusEnum.COMPLETED, completed.status)
        assertEquals(setOf("federated", "google"), completed.amr)
        assertEquals("urn:kudos:acr:federated", completed.acr)
        assertEquals("u-1", completed.context?.userId)
    }

    @Test
    fun externalProviderFlowCarriesEnrollmentAdvisoryDuringGrace() {
        val enforcer = mock(AuthenticationMfaPolicyEnforcer::class.java)
        val isolated = AuthenticationTransactionService(
            InMemoryAuthenticationTransactionStore(),
            AuthenticationMethodRegistry(listOf(provider)),
            DefaultAuthenticationAssurancePolicy(),
            enforcer,
        )
        `when`(
            enforcer.enforce(
                AuthenticationTransactionPurposeEnum.LOGIN,
                "t-1",
                "u-1",
                DefaultAuthenticationAssurancePolicy.ACR_FEDERATED,
            )
        ).thenReturn(MfaPolicyEnforcementResult(MfaPolicyEnforcementOutcomeEnum.ALLOW_ENROLLMENT_REQUIRED))
        val created = isolated.create(AuthenticationTransactionCreateRequest("t-1"))
        val prepared = isolated.prepareExternal(created.id, "provider-1", "t-1")

        val completed = isolated.completeExternal(
            prepared.id, "provider-1", "u-1", "t-1", "alice", "google",
        )

        assertEquals(AuthenticationTransactionStatusEnum.COMPLETED, completed.status)
        assertEquals(setOf(AuthenticationActionEnum.ENROLL_MFA), completed.postAuthenticationActions)
    }

    @Test
    fun externalProviderCannotBypassRequiredSecondFactor() {
        val enforcer = mock(AuthenticationMfaPolicyEnforcer::class.java)
        val isolated = AuthenticationTransactionService(
            InMemoryAuthenticationTransactionStore(),
            AuthenticationMethodRegistry(listOf(provider)),
            DefaultAuthenticationAssurancePolicy(),
            enforcer,
        )
        `when`(
            enforcer.enforce(
                AuthenticationTransactionPurposeEnum.LOGIN,
                "t-1",
                "u-1",
                DefaultAuthenticationAssurancePolicy.ACR_FEDERATED,
            )
        ).thenReturn(MfaPolicyEnforcementResult(MfaPolicyEnforcementOutcomeEnum.REQUIRE_SECOND_FACTOR))
        val created = isolated.create(AuthenticationTransactionCreateRequest("t-1"))
        val prepared = isolated.prepareExternal(created.id, "provider-1", "t-1")

        val failed = isolated.completeExternal(
            prepared.id, "provider-1", "u-1", "t-1", "alice", "google",
        )

        assertEquals(AuthenticationTransactionStatusEnum.FAILED, failed.status)
        assertEquals("MFA_REQUIRED", failed.errorCode)
        assertNull(failed.context)
    }

    @Test
    fun externalProviderCanContinueWithPinnedLocalSecondFactor() {
        val enforcer = mock(AuthenticationMfaPolicyEnforcer::class.java)
        val secondFactor = mock(FederatedSecondFactorVerifier::class.java)
        val isolated = AuthenticationTransactionService(
            InMemoryAuthenticationTransactionStore(),
            AuthenticationMethodRegistry(listOf(provider)),
            DefaultAuthenticationAssurancePolicy(),
            enforcer,
            secondFactor,
        )
        `when`(
            enforcer.enforce(
                AuthenticationTransactionPurposeEnum.LOGIN,
                "t-1",
                "u-1",
                DefaultAuthenticationAssurancePolicy.ACR_FEDERATED,
            )
        ).thenReturn(MfaPolicyEnforcementResult(MfaPolicyEnforcementOutcomeEnum.REQUIRE_SECOND_FACTOR))
        `when`(secondFactor.availableActions("u-1", "t-1")).thenReturn(
            setOf(AuthenticationActionEnum.VERIFY_TOTP, AuthenticationActionEnum.VERIFY_RECOVERY_CODE)
        )
        val secondFactorRequest = AuthenticationActionRequest(code = "123456", loginIp = 0x7F000001L)
        `when`(
            secondFactor.verify(
                "u-1", "t-1", "alice", AuthenticationActionEnum.VERIFY_TOTP, secondFactorRequest,
            )
        ).thenReturn(SecondFactorVerificationResult(success = true, method = "totp"))
        val created = isolated.create(AuthenticationTransactionCreateRequest("t-1"))
        val prepared = isolated.prepareExternal(created.id, "provider-1", "t-1")

        val challenged = isolated.completeExternal(
            prepared.id, "provider-1", "u-1", "t-1", "alice", "google",
        )
        val completed = isolated.act(
            challenged.id,
            AuthenticationActionEnum.VERIFY_TOTP,
            secondFactorRequest,
        )

        assertEquals(AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED, challenged.status)
        assertEquals(
            setOf(AuthenticationActionEnum.VERIFY_TOTP, AuthenticationActionEnum.VERIFY_RECOVERY_CODE),
            challenged.nextActions,
        )
        assertEquals(AuthenticationTransactionStatusEnum.COMPLETED, completed.status)
        assertEquals(setOf("federated", "google", "totp"), completed.amr)
        assertEquals(DefaultAuthenticationAssurancePolicy.ACR_MFA, completed.acr)
        assertEquals("u-1", completed.context?.userId)
    }

    @Test
    fun externalProviderCanContinueWithTransactionBoundPluggableSecondFactor() {
        val enforcer = mock(AuthenticationMfaPolicyEnforcer::class.java)
        val passkey = mock(IAuthenticationSecondFactorProvider::class.java)
        `when`(passkey.action()).thenReturn(AuthenticationActionEnum.VERIFY_PASSKEY)
        `when`(passkey.policyMethod()).thenReturn(MfaMethodEnum.WEBAUTHN)
        `when`(passkey.isAvailable("u-1", "t-1")).thenReturn(true)
        val secondFactors = AuthenticationSecondFactorRegistry(listOf(passkey))
        val isolated = AuthenticationTransactionService(
            store = InMemoryAuthenticationTransactionStore(),
            methodRegistry = AuthenticationMethodRegistry(listOf(provider)),
            assurancePolicy = DefaultAuthenticationAssurancePolicy(),
            mfaPolicyEnforcer = enforcer,
            secondFactorRegistry = secondFactors,
        )
        `when`(
            enforcer.enforce(
                AuthenticationTransactionPurposeEnum.LOGIN,
                "t-1",
                "u-1",
                DefaultAuthenticationAssurancePolicy.ACR_FEDERATED,
            )
        ).thenReturn(
            MfaPolicyEnforcementResult(
                MfaPolicyEnforcementOutcomeEnum.REQUIRE_SECOND_FACTOR,
                MfaPolicyDecision(
                    policy = EffectiveTenantMfaPolicy(
                        tenantId = "t-1",
                        allowedMethods = setOf(MfaMethodEnum.WEBAUTHN),
                    ),
                    required = true,
                    enrolled = true,
                    enrollmentRequired = false,
                    gracePeriodActive = false,
                    graceExpiresAt = null,
                ),
            )
        )
        val request = AuthenticationActionRequest(attributes = mapOf("signed" to "response"))
        val created = isolated.create(AuthenticationTransactionCreateRequest("t-1"))
        val prepared = isolated.prepareExternal(created.id, "provider-1", "t-1")
        val challenged = isolated.completeExternal(
            prepared.id, "provider-1", "u-1", "t-1", "alice", "google",
        )
        `when`(passkey.verify(challenged.id, "u-1", "t-1", request)).thenReturn(
            SecondFactorVerificationResult(
                success = true,
                method = "webauthn",
                acr = DefaultAuthenticationAssurancePolicy.ACR_PHISHING_RESISTANT,
            )
        )

        val completed = isolated.act(challenged.id, AuthenticationActionEnum.VERIFY_PASSKEY, request)

        assertEquals(AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED, challenged.status)
        assertEquals(setOf(AuthenticationActionEnum.VERIFY_PASSKEY), challenged.nextActions)
        assertEquals(AuthenticationTransactionStatusEnum.COMPLETED, completed.status)
        assertEquals(setOf("federated", "google", "webauthn"), completed.amr)
        assertEquals(DefaultAuthenticationAssurancePolicy.ACR_PHISHING_RESISTANT, completed.acr)
        assertEquals("u-1", completed.context?.userId)
    }

    @Test
    fun verifiedLocalPrimaryCanContinueWithPluggableSecondFactorWithoutStoredCredential() {
        val passkey = mock(IAuthenticationSecondFactorProvider::class.java)
        `when`(passkey.action()).thenReturn(AuthenticationActionEnum.VERIFY_PASSKEY)
        `when`(passkey.policyMethod()).thenReturn(MfaMethodEnum.WEBAUTHN)
        val secondFactors = AuthenticationSecondFactorRegistry(listOf(passkey))
        val isolated = AuthenticationTransactionService(
            store = InMemoryAuthenticationTransactionStore(),
            methodRegistry = AuthenticationMethodRegistry(listOf(provider)),
            secondFactorRegistry = secondFactors,
        )
        val created = isolated.create(AuthenticationTransactionCreateRequest("t-1", "test"))
        val challenged = isolated.act(
            created.id,
            AuthenticationActionEnum.VERIFY_PASSWORD,
            AuthenticationActionRequest(username = "alice", plainPassword = "passkey-alternative"),
        )
        val signed = AuthenticationActionRequest(attributes = mapOf("signed" to "response"))
        `when`(passkey.verify(challenged.id, "u-1", "t-1", signed)).thenReturn(
            SecondFactorVerificationResult(success = true, method = "webauthn")
        )

        val completed = isolated.act(challenged.id, AuthenticationActionEnum.VERIFY_PASSKEY, signed)

        assertEquals(AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED, challenged.status)
        assertEquals("u-1", challenged.userId)
        assertEquals(setOf("test-primary"), challenged.amr)
        assertEquals("urn:kudos:acr:password", challenged.acr)
        assertTrue(!challenged.toString().contains("passkey-alternative"))
        assertEquals(AuthenticationTransactionStatusEnum.COMPLETED, completed.status)
        assertEquals(setOf("test-primary", "webauthn"), completed.amr)
        assertEquals(DefaultAuthenticationAssurancePolicy.ACR_MFA, completed.acr)
    }

    @Test
    fun preparedExternalInvitationCannotBeReplaced() {
        val created = service.create(AuthenticationTransactionCreateRequest("t-1"))
        service.prepareExternal(created.id, "provider-1", "t-1", "invitation-1")

        kotlin.test.assertFailsWith<IllegalStateException> {
            service.prepareExternal(created.id, "provider-1", "t-1", "invitation-2")
        }
    }

    @Test
    fun externalProviderFlow_rejectsCrossTenantAndProviderSwap() {
        val created = service.create(AuthenticationTransactionCreateRequest("t-1"))
        kotlin.test.assertFailsWith<IllegalStateException> {
            service.prepareExternal(created.id, "provider-1", "t-2")
        }
        val prepared = service.prepareExternal(created.id, "provider-1", "t-1")
        kotlin.test.assertFailsWith<IllegalStateException> {
            service.completeExternal(prepared.id, "provider-2", "u-1", "t-1", "alice", "google")
        }
    }

    @Test
    fun failExternal_terminatesPreparedTransaction() {
        val created = service.create(AuthenticationTransactionCreateRequest("t-1"))
        val prepared = service.prepareExternal(created.id, "provider-1", "t-1")

        val failed = service.failExternal(prepared.id, "provider-1", "EXTERNAL_IDENTITY_NOT_BOUND")

        assertEquals(AuthenticationTransactionStatusEnum.FAILED, failed?.status)
        assertEquals("EXTERNAL_IDENTITY_NOT_BOUND", failed?.errorCode)
    }

    @Test
    fun externalLinkPinsInitiatorProviderAndTenant() {
        val created = service.createExternalLink("u-1", "t-1", "provider-1")

        assertEquals(AuthenticationTransactionPurposeEnum.LINK_EXTERNAL_IDENTITY, created.purpose)
        assertEquals("u-1", created.initiatorUserId)
        assertEquals("t-1", created.tenantId)
        assertEquals("external:provider-1", created.method)

        val completed = service.completeExternalLink(created.id, "provider-1", "u-1")
        assertEquals(AuthenticationTransactionStatusEnum.COMPLETED, completed.status)
        assertEquals("u-1", completed.userId)
    }

    @Test
    fun externalLinkRejectsInitiatorOrProviderSwap() {
        val created = service.createExternalLink("u-1", "t-1", "provider-1")

        kotlin.test.assertFailsWith<IllegalStateException> {
            service.completeExternalLink(created.id, "provider-1", "u-2")
        }
        kotlin.test.assertFailsWith<IllegalStateException> {
            service.completeExternalLink(created.id, "provider-2", "u-1")
        }
    }
}
