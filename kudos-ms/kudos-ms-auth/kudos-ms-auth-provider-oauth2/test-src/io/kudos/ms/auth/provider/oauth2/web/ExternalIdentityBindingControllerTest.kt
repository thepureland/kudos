package io.kudos.ms.auth.provider.oauth2.web

import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionStatusEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.core.authentication.assurance.AuthenticationAssuranceReasonEnum
import io.kudos.ms.auth.core.authentication.assurance.AuthenticationAssuranceRequiredException
import io.kudos.ms.auth.core.authentication.assurance.AuthenticationAssuranceVerifier
import io.kudos.ms.auth.core.authentication.assurance.DefaultAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.service.iservice.IAuthenticationTransactionService
import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.model.po.AuthIdentityProvider
import io.kudos.ms.auth.provider.oauth2.init.ExternalLoginProperties
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class ExternalIdentityBindingControllerTest {

    private val providerDao = mock(AuthIdentityProviderDao::class.java)
    private val transactions = mock(IAuthenticationTransactionService::class.java)
    private val bindings = mock(IUserAccountThirdService::class.java)
    private val now = Instant.parse("2026-08-25T10:00:00Z")
    private val properties = ExternalLoginProperties()
    private val controller = ExternalIdentityBindingController(
        providerDao,
        transactions,
        bindings,
        properties,
        AuthenticationAssuranceVerifier(
            DefaultAuthenticationAssurancePolicy(),
            Clock.fixed(now, ZoneOffset.UTC),
        ),
    )

    @Test
    fun linkCreatesServerPinnedTransactionForCurrentSession() {
        val provider = AuthIdentityProvider {
            id = "provider-1"
            tenantId = "t-1"
            templateId = "google"
            code = "google-main"
            displayName = "Google"
            clientId = "client"
            jitPolicy = "DISABLED"
            linkPolicy = "MANUAL_CONFIRM"
            active = true
        }
        val transaction = AuthenticationTransaction(
            id = "tx-link",
            tenantId = "t-1",
            purpose = AuthenticationTransactionPurposeEnum.LINK_EXTERNAL_IDENTITY,
            initiatorUserId = "u-1",
            status = AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED,
            nextActions = setOf(AuthenticationActionEnum.REDIRECT_EXTERNAL_PROVIDER),
            method = "external:provider-1",
            createdAt = now,
            updatedAt = now,
            expiresAt = now.plusSeconds(60),
        )
        `when`(providerDao.findActiveById("provider-1")).thenReturn(provider)
        `when`(transactions.createExternalLink("u-1", "t-1", "provider-1")).thenReturn(transaction)
        val request = loggedInRequest()
        val response = MockHttpServletResponse()

        controller.link("provider-1", "legacy-tx", request, response)

        verify(transactions).createExternalLink("u-1", "t-1", "provider-1")
        verify(transactions, never()).get("legacy-tx")
        assertContains(response.redirectedUrl!!, "transactionId=tx-link")
    }

    @Test
    fun linkRequiresLocalKudosSession() {
        assertFailsWith<ResponseStatusException> {
            controller.link("provider-1", null, MockHttpServletRequest(), MockHttpServletResponse())
        }
    }

    @Test
    fun linkRejectsMissingAuthoritativeAuthenticationSession() {
        val provider = AuthIdentityProvider {
            id = "provider-1"
            tenantId = "t-1"
            templateId = "google"
            code = "google-main"
            displayName = "Google"
            clientId = "client"
            jitPolicy = "DISABLED"
            linkPolicy = "MANUAL_CONFIRM"
            active = true
        }
        `when`(providerDao.findActiveById("provider-1")).thenReturn(provider)
        val request = loggedInRequest().apply {
            removeAttribute(AuthenticationSession.REQUEST_ATTRIBUTE)
        }

        val error = assertFailsWith<AuthenticationAssuranceRequiredException> {
            controller.link(
                "provider-1",
                null,
                request,
                MockHttpServletResponse(),
            )
        }

        assertEquals(AuthenticationAssuranceReasonEnum.AUTHENTICATION_REQUIRED, error.reason)
        verify(transactions, never()).createExternalLink("u-1", "t-1", "provider-1")
    }

    @Test
    fun linkRejectsStaleAuthenticationWithConfiguredStepUpChallenge() {
        properties.linkReauthenticationMaxAgeSeconds = 10
        val request = loggedInRequest(authTime = now.minusSeconds(11))

        val error = assertFailsWith<AuthenticationAssuranceRequiredException> {
            controller.link("provider-1", null, request, MockHttpServletResponse())
        }

        assertEquals(AuthenticationAssuranceReasonEnum.AUTHENTICATION_TOO_OLD, error.reason)
        assertEquals(DefaultAuthenticationAssurancePolicy.ACR_PASSWORD, error.requiredAcr)
        assertEquals(10, error.maxAgeSeconds)
        verify(transactions, never()).createExternalLink("u-1", "t-1", "provider-1")
    }

    @Test
    fun linkRejectsAuthenticationSessionForAnotherSubject() {
        val request = loggedInRequest().apply {
            val session = getAttribute(AuthenticationSession.REQUEST_ATTRIBUTE) as AuthenticationSession
            setAttribute(AuthenticationSession.REQUEST_ATTRIBUTE, session.copy(userId = "u-2"))
        }

        val error = assertFailsWith<AuthenticationAssuranceRequiredException> {
            controller.link("provider-1", null, request, MockHttpServletResponse())
        }

        assertEquals(AuthenticationAssuranceReasonEnum.AUTHENTICATION_REQUIRED, error.reason)
        verify(transactions, never()).createExternalLink("u-1", "t-1", "provider-1")
    }

    @Test
    fun unlinkNeverAcceptsUserIdFromRequest() {
        `when`(bindings.unbindExternalIdentity("binding-1", "u-1", "t-1")).thenReturn(true)

        val request = loggedInRequest()

        controller.unlink("binding-1", null, request)

        verify(bindings).unbindExternalIdentity("binding-1", "u-1", "t-1")
    }

    private fun loggedInRequest(
        authTime: Instant = now,
        acr: String = DefaultAuthenticationAssurancePolicy.ACR_PASSWORD,
    ) = MockHttpServletRequest().apply {
        getSession(true)!!.setAttribute(
            KudosContext.SESSION_KEY_USER,
            SessionUserPrincipal("u-1", "t-1", "alice"),
        )
        session!!.setAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE, "auth-session-1")
        setAttribute(
            AuthenticationSession.REQUEST_ATTRIBUTE,
            AuthenticationSession(
                id = "auth-session-1",
                tenantId = "t-1",
                userId = "u-1",
                username = "alice",
                authTime = authTime,
                amr = setOf("password"),
                acr = acr,
                createdAt = now.minusSeconds(60),
                lastSeenAt = now,
                idleExpiresAt = now.plusSeconds(600),
                absoluteExpiresAt = now.plusSeconds(3_600),
            ),
        )
    }
}
