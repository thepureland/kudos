package io.kudos.ms.auth.api.public.controller.authentication

import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionStatusEnum
import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaMethodEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationActionRequest
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationTransactionCreateRequest
import io.kudos.ms.auth.core.authentication.AuthenticationMethodRegistry
import io.kudos.ms.auth.core.authentication.assurance.DefaultAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.method.PasswordAuthenticationMethodProvider
import io.kudos.ms.auth.core.authentication.mfa.AuthenticationSecondFactorRegistry
import io.kudos.ms.auth.core.authentication.mfa.policy.AuthenticationMfaPolicyEnforcer
import io.kudos.ms.auth.core.authentication.mfa.policy.MfaPolicyEnforcementOutcomeEnum
import io.kudos.ms.auth.core.authentication.mfa.policy.MfaPolicyEnforcementResult
import io.kudos.ms.auth.core.authentication.mfa.policy.model.EffectiveTenantMfaPolicy
import io.kudos.ms.auth.core.authentication.mfa.policy.model.MfaPolicyDecision
import io.kudos.ms.auth.core.authentication.service.impl.AuthenticationTransactionService
import io.kudos.ms.auth.core.authentication.service.iservice.IAuthenticationTransactionService
import io.kudos.ms.auth.core.authentication.session.service.impl.AuthenticationSessionService
import io.kudos.ms.auth.core.authentication.session.store.InMemoryAuthenticationSessionStore
import io.kudos.ms.auth.core.authentication.store.InMemoryAuthenticationTransactionStore
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.WebAuthnCredentialSummary
import io.kudos.ms.auth.core.authentication.mfa.webauthn.service.iservice.IWebAuthnCredentialService
import io.kudos.ms.auth.provider.webauthn.authentication.WebAuthnAuthenticationMethodProvider
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnAssertionFinish
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnAssertionFinishCommand
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnAssertionStart
import io.kudos.ms.auth.provider.webauthn.service.WebAuthnAssertionService
import io.kudos.context.core.KudosContext
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult
import io.kudos.ms.user.common.passport.vo.response.UserInfoModel
import io.kudos.ms.user.core.passport.service.iservice.IPassportService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.web.server.ResponseStatusException
import org.springframework.mock.web.MockHttpServletRequest
import java.time.Instant
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class WebAuthnAuthenticationPublicControllerTest {
    private val transactions = mock(IAuthenticationTransactionService::class.java)
    private val assertions = mock(WebAuthnAssertionService::class.java)
    private val controller = WebAuthnAuthenticationPublicController(transactions, assertions)

    @Test
    fun `login assertion should be username-less and bound to transaction`() {
        val transaction = transaction()
        val started = start()
        `when`(transactions.get(transaction.id)).thenReturn(transaction)
        `when`(assertions.begin("tenant-1", null, transaction.id)).thenReturn(started)

        val result = controller.beginAssertion(transaction.id)

        assertEquals(started, result)
        verify(assertions).begin("tenant-1", null, transaction.id)
    }

    @Test
    fun `step-up assertion should be restricted to existing subject`() {
        val transaction = transaction().copy(
            purpose = AuthenticationTransactionPurposeEnum.STEP_UP,
            initiatorUserId = "user-1",
            userId = "user-1",
        )
        `when`(transactions.get(transaction.id)).thenReturn(transaction)
        `when`(assertions.begin("tenant-1", "user-1", transaction.id)).thenReturn(start())

        controller.beginAssertion(transaction.id)

        verify(assertions).begin("tenant-1", "user-1", transaction.id)
    }

    @Test
    fun `second-factor transaction without pinned subject should not create assertion`() {
        val transaction = transaction().copy(method = "password")
        `when`(transactions.get(transaction.id)).thenReturn(transaction)

        assertFailsWith<ResponseStatusException> { controller.beginAssertion(transaction.id) }

        verifyNoInteractions(assertions)
    }

    @Test
    fun `federated second-factor assertion should be restricted to authenticated local subject`() {
        val transaction = transaction().copy(
            method = "external:google",
            userId = "user-1",
            username = "alice",
            nextActions = setOf(
                AuthenticationActionEnum.VERIFY_TOTP,
                AuthenticationActionEnum.VERIFY_PASSKEY,
            ),
        )
        `when`(transactions.get(transaction.id)).thenReturn(transaction)
        `when`(assertions.begin("tenant-1", "user-1", transaction.id)).thenReturn(start())

        controller.beginAssertion(transaction.id)

        verify(assertions).begin("tenant-1", "user-1", transaction.id)
    }

    @Test
    fun `public passwordless flow should issue and bind managed session only after verified action`() {
        val method = WebAuthnAuthenticationMethodProvider(assertions)
        val transactionService = AuthenticationTransactionService(
            InMemoryAuthenticationTransactionStore(),
            AuthenticationMethodRegistry(listOf(method)),
        )
        val sessionService = AuthenticationSessionService(InMemoryAuthenticationSessionStore())
        val transactionController = AuthenticationTransactionPublicController(transactionService, sessionService)
        val assertionController = WebAuthnAuthenticationPublicController(transactionService, assertions)
        val transaction = transactionController.create(
            AuthenticationTransactionCreateRequest("tenant-1", "passkey")
        )
        val started = start()
        `when`(assertions.begin("tenant-1", null, transaction.id)).thenReturn(started)
        assertEquals(started, assertionController.beginAssertion(transaction.id))
        val command = WebAuthnAssertionFinishCommand(started.ceremonyId, "{\"response\":\"signed\"}")
        `when`(assertions.finish("tenant-1", command, null, transaction.id)).thenReturn(finish())
        val servletRequest = MockHttpServletRequest().apply {
            setRemoteAddr("203.0.113.10")
            addHeader("User-Agent", "Mozilla/5.0")
        }

        val completed = transactionController.act(
            transaction.id,
            AuthenticationActionEnum.VERIFY_PASSKEY,
            AuthenticationActionRequest(
                attributes = mapOf(
                    WebAuthnAuthenticationMethodProvider.ATTRIBUTE_CEREMONY_ID to command.ceremonyId,
                    WebAuthnAuthenticationMethodProvider.ATTRIBUTE_CREDENTIAL_RESPONSE to command.credentialResponseJson,
                )
            ),
            servletRequest,
        )

        assertEquals(AuthenticationTransactionStatusEnum.COMPLETED, completed.status)
        val logicalSessionId = completed.context?.sessionId
        assertTrue(!logicalSessionId.isNullOrBlank())
        assertEquals("user-1", sessionService.get(logicalSessionId)?.userId)
        val httpSession = servletRequest.getSession(false)
        assertEquals(logicalSessionId, httpSession?.getAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE))
        val principal = httpSession?.getAttribute(KudosContext.SESSION_KEY_USER)
        assertTrue(principal is SessionUserPrincipal)
        assertEquals("user-1", principal.id)
    }

    @Test
    fun `public password plus passkey flow should retain no password and issue session after second factor`() {
        val passport = mock(IPassportService::class.java)
        val enforcer = mock(AuthenticationMfaPolicyEnforcer::class.java)
        val credentials = mock(IWebAuthnCredentialService::class.java)
        val passkey = WebAuthnAuthenticationMethodProvider(
            assertionService = assertions,
            credentialService = credentials,
        )
        val secondFactors = AuthenticationSecondFactorRegistry(listOf(passkey))
        val password = PasswordAuthenticationMethodProvider(
            passportService = passport,
            mfaPolicyEnforcer = enforcer,
            secondFactorRegistry = secondFactors,
        )
        val transactionService = AuthenticationTransactionService(
            store = InMemoryAuthenticationTransactionStore(),
            methodRegistry = AuthenticationMethodRegistry(listOf(password, passkey)),
            secondFactorRegistry = secondFactors,
        )
        val sessionService = AuthenticationSessionService(InMemoryAuthenticationSessionStore())
        val transactionController = AuthenticationTransactionPublicController(transactionService, sessionService)
        val assertionController = WebAuthnAuthenticationPublicController(transactionService, assertions)
        val allowedMethods = setOf(MfaMethodEnum.WEBAUTHN)
        val user = UserInfoModel(
            id = "user-1",
            username = "alice",
            tenantId = "tenant-1",
            orgId = null,
            accountTypeDictCode = null,
            defaultLocale = null,
            defaultTimezone = null,
            defaultCurrency = null,
            loginTime = LocalDateTime.parse("2026-08-25T10:00:00"),
        )
        `when`(
            passport.login(
                org.mockito.ArgumentMatchers.any(PassportLoginRequest::class.java)
                    ?: PassportLoginRequest("tenant-1", "alice", "unused")
            )
        ).thenReturn(PassportLoginResult.success(user))
        `when`(
            enforcer.enforce(
                AuthenticationTransactionPurposeEnum.LOGIN,
                "tenant-1",
                "user-1",
                PasswordAuthenticationMethodProvider.ACR_PASSWORD,
            )
        ).thenReturn(
            MfaPolicyEnforcementResult(
                MfaPolicyEnforcementOutcomeEnum.REQUIRE_SECOND_FACTOR,
                MfaPolicyDecision(
                    policy = EffectiveTenantMfaPolicy("tenant-1", allowedMethods = allowedMethods),
                    required = true,
                    enrolled = true,
                    enrollmentRequired = false,
                    gracePeriodActive = false,
                    graceExpiresAt = null,
                ),
            )
        )
        `when`(credentials.isEnrolled("user-1", "tenant-1")).thenReturn(true)
        val servletRequest = MockHttpServletRequest()
        val created = transactionController.create(AuthenticationTransactionCreateRequest("tenant-1", "password"))

        val challenged = transactionController.act(
            created.id,
            AuthenticationActionEnum.VERIFY_PASSWORD,
            AuthenticationActionRequest(username = "alice", plainPassword = "browser-secret"),
            servletRequest,
        )

        assertEquals(AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED, challenged.status)
        assertEquals(setOf(AuthenticationActionEnum.VERIFY_PASSKEY), challenged.nextActions)
        assertEquals("user-1", challenged.userId)
        assertEquals(setOf("password"), challenged.amr)
        assertTrue(!challenged.toString().contains("browser-secret"))
        assertNull(servletRequest.getSession(false))
        val started = start()
        `when`(assertions.begin("tenant-1", "user-1", challenged.id)).thenReturn(started)
        assertEquals(started, assertionController.beginAssertion(challenged.id))
        val command = WebAuthnAssertionFinishCommand(started.ceremonyId, "{\"response\":\"signed\"}")
        `when`(assertions.finish("tenant-1", command, "user-1", challenged.id)).thenReturn(finish())

        val completed = transactionController.act(
            challenged.id,
            AuthenticationActionEnum.VERIFY_PASSKEY,
            AuthenticationActionRequest(
                attributes = mapOf(
                    WebAuthnAuthenticationMethodProvider.ATTRIBUTE_CEREMONY_ID to command.ceremonyId,
                    WebAuthnAuthenticationMethodProvider.ATTRIBUTE_CREDENTIAL_RESPONSE to command.credentialResponseJson,
                )
            ),
            servletRequest,
        )

        assertEquals(AuthenticationTransactionStatusEnum.COMPLETED, completed.status)
        assertEquals(setOf("password", "webauthn"), completed.amr)
        assertEquals(DefaultAuthenticationAssurancePolicy.ACR_PHISHING_RESISTANT, completed.acr)
        assertTrue(!completed.context?.sessionId.isNullOrBlank())
        assertEquals("user-1", sessionService.get(completed.context?.sessionId!!)?.userId)
    }

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

    private fun start() = WebAuthnAssertionStart(
        ceremonyId = "A".repeat(43),
        publicKeyCredentialRequestOptions = "{}",
        expiresAt = Instant.parse("2026-08-25T10:05:00Z"),
    )

    private fun finish() = WebAuthnAssertionFinish(
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
        userVerified = true,
        backupEligible = true,
        backedUp = true,
    )
}
