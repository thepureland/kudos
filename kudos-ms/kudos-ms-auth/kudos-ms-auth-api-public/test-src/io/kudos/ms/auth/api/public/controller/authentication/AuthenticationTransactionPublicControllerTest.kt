package io.kudos.ms.auth.api.public.controller.authentication

import io.kudos.base.net.IpKit
import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionStatusEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationContext
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationActionRequest
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationTransactionCreateRequest
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationStepUpCreateRequest
import io.kudos.ms.auth.core.authentication.AuthenticationMethodRegistry
import io.kudos.ms.auth.core.authentication.service.impl.AuthenticationTransactionService
import io.kudos.ms.auth.core.authentication.spi.AuthenticationChallenge
import io.kudos.ms.auth.core.authentication.spi.AuthenticationMethodOutcomeEnum
import io.kudos.ms.auth.core.authentication.spi.AuthenticationMethodResult
import io.kudos.ms.auth.core.authentication.spi.IAuthenticationMethodProvider
import io.kudos.ms.auth.core.authentication.store.InMemoryAuthenticationTransactionStore
import io.kudos.ms.auth.core.authentication.service.iservice.IAuthenticationTransactionService
import io.kudos.ms.auth.core.authentication.session.service.impl.AuthenticationSessionService
import io.kudos.ms.auth.core.authentication.session.store.InMemoryAuthenticationSessionStore
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class AuthenticationTransactionPublicControllerTest {

    @Test
    fun stepUp_elevatesTheBoundSessionWithoutCreatingOrExtendingIt() {
        val method = object : IAuthenticationMethodProvider {
            override fun method() = "test-mfa"

            override fun begin(
                transaction: AuthenticationTransaction,
                request: AuthenticationTransactionCreateRequest,
            ) = AuthenticationChallenge(
                AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED,
                AuthenticationActionEnum.VERIFY_PASSWORD,
            )

            override fun verify(
                transaction: AuthenticationTransaction,
                action: AuthenticationActionEnum,
                request: AuthenticationActionRequest,
            ) = AuthenticationMethodResult(
                outcome = AuthenticationMethodOutcomeEnum.SUCCESS,
                userId = transaction.initiatorUserId,
                tenantId = transaction.tenantId,
                username = transaction.username,
                amr = setOf("password", "totp"),
                acr = "urn:kudos:acr:mfa",
            )
        }
        val transactionService = AuthenticationTransactionService(
            InMemoryAuthenticationTransactionStore(),
            AuthenticationMethodRegistry(listOf(method)),
        )
        val sessionService = AuthenticationSessionService(InMemoryAuthenticationSessionStore())
        val source = sessionService.issue(
            io.kudos.ms.auth.core.authentication.session.model.AuthenticationSessionIssueCommand(
                context = AuthenticationContext(
                    userId = "user-1",
                    tenantId = "tenant-1",
                    authTime = Instant.now().minusSeconds(60),
                    amr = setOf("password"),
                    acr = "urn:kudos:acr:password",
                ),
                username = "alice",
            )
        )
        val controller = AuthenticationTransactionPublicController(transactionService, sessionService)
        val servletRequest = MockHttpServletRequest().apply {
            setRemoteAddr("203.0.113.9")
            setSession(MockHttpSession().apply {
                setAttribute(
                    KudosContext.SESSION_KEY_USER,
                    SessionUserPrincipal("user-1", "tenant-1", "alice"),
                )
                setAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE, source.id)
            })
        }
        val oldHttpSessionId = servletRequest.getSession(false)!!.id

        val created = controller.createStepUp(
            AuthenticationStepUpCreateRequest("urn:kudos:acr:mfa", "test-mfa"),
            servletRequest,
        )

        assertEquals(AuthenticationTransactionPurposeEnum.STEP_UP, created.purpose)
        assertEquals("user-1", created.initiatorUserId)
        assertEquals(source.id, created.sourceSessionId)
        assertEquals("urn:kudos:acr:mfa", created.requiredAcr)

        val attackerRequest = MockHttpServletRequest().apply {
            setSession(MockHttpSession().apply {
                setAttribute(
                    KudosContext.SESSION_KEY_USER,
                    SessionUserPrincipal("attacker", "tenant-1", "mallory"),
                )
                setAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE, "attacker-session")
            })
        }
        assertFailsWith<ResponseStatusException> {
            controller.act(
                created.id,
                AuthenticationActionEnum.VERIFY_PASSWORD,
                AuthenticationActionRequest(plainPassword = "secret"),
                attackerRequest,
            )
        }
        assertEquals(AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED, transactionService.get(created.id)?.status)

        val completed = controller.act(
            created.id,
            AuthenticationActionEnum.VERIFY_PASSWORD,
            AuthenticationActionRequest(plainPassword = "secret"),
            servletRequest,
        )

        assertEquals(AuthenticationTransactionStatusEnum.COMPLETED, completed.status)
        assertEquals(source.id, completed.context?.sessionId)
        assertNotEquals(oldHttpSessionId, servletRequest.getSession(false)!!.id)
        assertEquals(
            source.id,
            servletRequest.getSession(false)!!.getAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE),
        )
        val elevated = sessionService.listForUser("tenant-1", "user-1")
        assertEquals(1, elevated.size)
        assertEquals(source.id, elevated.single().id)
        assertEquals("urn:kudos:acr:mfa", elevated.single().acr)
        assertEquals(source.absoluteExpiresAt, elevated.single().absoluteExpiresAt)
    }

    @Test
    fun federatedSecondFactorAction_issuesAndBindsLocalSessionOnlyAfterCompletion() {
        val now = Instant.now()
        val challenged = AuthenticationTransaction(
            id = "tx-federated-mfa",
            tenantId = "tenant-1",
            userId = "user-1",
            username = "alice",
            status = AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED,
            nextActions = setOf(AuthenticationActionEnum.VERIFY_TOTP),
            method = "google",
            amr = setOf("federated", "google"),
            acr = "urn:kudos:acr:federated",
            createdAt = now,
            updatedAt = now,
            expiresAt = now.plusSeconds(300),
        )
        val completed = challenged.copy(
            status = AuthenticationTransactionStatusEnum.COMPLETED,
            nextActions = emptySet(),
            amr = setOf("federated", "google", "totp"),
            acr = "urn:kudos:acr:mfa",
            context = AuthenticationContext(
                userId = "user-1",
                tenantId = "tenant-1",
                authTime = now,
                amr = setOf("federated", "google", "totp"),
                acr = "urn:kudos:acr:mfa",
            ),
            updatedAt = now.plusSeconds(1),
        )
        var boundSessionId: String? = null
        val service = object : IAuthenticationTransactionService {
            override fun create(request: AuthenticationTransactionCreateRequest) = error("not used")
            override fun get(id: String): AuthenticationTransaction = challenged
            override fun act(
                id: String,
                action: AuthenticationActionEnum,
                request: AuthenticationActionRequest,
            ): AuthenticationTransaction {
                assertEquals(challenged.id, id)
                assertEquals(AuthenticationActionEnum.VERIFY_TOTP, action)
                assertEquals("123456", request.code)
                return completed
            }

            override fun bindSession(id: String, sessionId: String): AuthenticationTransaction {
                assertEquals(challenged.id, id)
                boundSessionId = sessionId
                return completed.copy(context = completed.context?.copy(sessionId = sessionId))
            }

            override fun cancel(id: String): AuthenticationTransaction? = error("not used")
            override fun prepareExternal(
                id: String,
                providerId: String,
                tenantId: String,
                externalInvitationId: String?,
            ) = error("not used")
            override fun createExternalLink(userId: String, tenantId: String, providerId: String) = error("not used")
            override fun completeExternal(
                id: String,
                providerId: String,
                userId: String,
                tenantId: String,
                username: String,
                providerCode: String,
            ) = error("not used")
            override fun failExternal(id: String, providerId: String, errorCode: String) = error("not used")
            override fun completeExternalLink(id: String, providerId: String, userId: String) = error("not used")
        }
        val sessionService = AuthenticationSessionService(InMemoryAuthenticationSessionStore())
        val controller = AuthenticationTransactionPublicController(service, sessionService)
        val servletRequest = MockHttpServletRequest().apply {
            setRemoteAddr("203.0.113.10")
            addHeader("User-Agent", "Mozilla/5.0 (Linux) Chrome/126.0.0.0")
        }

        val result = controller.act(
            challenged.id,
            AuthenticationActionEnum.VERIFY_TOTP,
            AuthenticationActionRequest(code = "123456"),
            servletRequest,
        )

        val httpSession = servletRequest.getSession(false)
        assertTrue(httpSession != null)
        assertEquals(AuthenticationTransactionStatusEnum.COMPLETED, result.status)
        assertEquals("urn:kudos:acr:mfa", result.context?.acr)
        assertEquals(boundSessionId, result.context?.sessionId)
        assertEquals(boundSessionId, httpSession.getAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE))
        assertEquals("user-1", sessionService.get(requireNotNull(boundSessionId))?.userId)
        val principal = httpSession.getAttribute(KudosContext.SESSION_KEY_USER)
        assertTrue(principal is SessionUserPrincipal)
        assertEquals("user-1", principal.id)
    }

    @Test
    fun completedAction_overwritesMetadataRotatesSessionAndBindsPrincipal() {
        val now = Instant.now()
        val completed = AuthenticationTransaction(
            id = "tx-1",
            tenantId = "tenant-1",
            userId = "user-1",
            username = "alice",
            status = AuthenticationTransactionStatusEnum.COMPLETED,
            method = "password",
            amr = setOf("password"),
            acr = "urn:kudos:acr:password",
            context = AuthenticationContext(
                userId = "user-1",
                tenantId = "tenant-1",
                authTime = now,
                amr = setOf("password"),
                acr = "urn:kudos:acr:password",
            ),
            createdAt = now,
            updatedAt = now,
            expiresAt = now.plusSeconds(300),
        )
        lateinit var capturedAction: AuthenticationActionRequest
        val service = object : IAuthenticationTransactionService {
            override fun create(request: AuthenticationTransactionCreateRequest) = error("not used")
            override fun get(id: String): AuthenticationTransaction = completed
            override fun act(
                id: String,
                action: AuthenticationActionEnum,
                request: AuthenticationActionRequest,
            ): AuthenticationTransaction {
                capturedAction = request
                return completed
            }

            override fun bindSession(id: String, sessionId: String): AuthenticationTransaction =
                completed.copy(context = completed.context?.copy(sessionId = sessionId))

            override fun cancel(id: String): AuthenticationTransaction? = error("not used")
            override fun prepareExternal(
                id: String,
                providerId: String,
                tenantId: String,
                externalInvitationId: String?,
            ) = error("not used")
            override fun createExternalLink(userId: String, tenantId: String, providerId: String) = error("not used")
            override fun completeExternal(
                id: String,
                providerId: String,
                userId: String,
                tenantId: String,
                username: String,
                providerCode: String,
            ) = error("not used")
            override fun failExternal(id: String, providerId: String, errorCode: String) = error("not used")
            override fun completeExternalLink(id: String, providerId: String, userId: String) = error("not used")
        }
        val sessionService = AuthenticationSessionService(InMemoryAuthenticationSessionStore())
        val controller = AuthenticationTransactionPublicController(service, sessionService)
        val servletRequest = MockHttpServletRequest().apply {
            setRemoteAddr("203.0.113.9")
            addHeader("User-Agent", "Mozilla/5.0 (Linux) Chrome/126.0.0.0")
        }
        val session = MockHttpSession()
        servletRequest.setSession(session)
        val previousSessionId = session.id
        val clientAction = AuthenticationActionRequest(
            username = "alice",
            plainPassword = "secret",
            loginIp = 1L,
            loginDevice = "forged",
            userAgent = "forged",
        )

        val result = controller.act(
            "tx-1",
            AuthenticationActionEnum.VERIFY_PASSWORD,
            clientAction,
            servletRequest,
        )

        assertEquals(IpKit.ipv4StringToLong("203.0.113.9"), capturedAction.loginIp)
        assertEquals("PC", capturedAction.loginDevice)
        assertEquals("Chrome 126.0.0.0", capturedAction.loginBrowser)
        assertEquals("Linux", capturedAction.loginOs)
        assertEquals("Mozilla/5.0 (Linux) Chrome/126.0.0.0", capturedAction.userAgent)
        assertNotEquals(previousSessionId, session.id)
        val logicalSessionId = result.context?.sessionId
        assertTrue(!logicalSessionId.isNullOrBlank())
        assertNotEquals(session.id, logicalSessionId)
        assertEquals(logicalSessionId, session.getAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE))
        assertEquals("user-1", sessionService.get(logicalSessionId)?.userId)
        val principal = session.getAttribute(KudosContext.SESSION_KEY_USER)
        assertTrue(principal is SessionUserPrincipal)
        assertEquals("user-1", principal.id)
    }
}
