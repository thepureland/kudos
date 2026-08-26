package io.kudos.ms.auth.api.public.controller.authentication

import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.core.authentication.assurance.AuthenticationAssuranceReasonEnum
import io.kudos.ms.auth.core.authentication.assurance.AuthenticationAssuranceRequiredException
import io.kudos.ms.auth.core.authentication.assurance.AuthenticationAssuranceVerifier
import io.kudos.ms.auth.core.authentication.assurance.DefaultAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.WebAuthnCredentialRenameRequest
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.WebAuthnCredentialSummary
import io.kudos.ms.auth.core.authentication.mfa.webauthn.service.iservice.IWebAuthnCredentialService
import io.kudos.ms.auth.provider.webauthn.WebAuthnProviderProperties
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnRegistrationFinish
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnRegistrationFinishCommand
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnRegistrationFinishRequest
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnRegistrationStart
import io.kudos.ms.auth.provider.webauthn.service.WebAuthnRegistrationService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class WebAuthnEnrollmentPublicControllerTest {
    private val now = Instant.parse("2026-08-25T10:00:00Z")
    private val registrations = mock(WebAuthnRegistrationService::class.java)
    private val credentials = mock(IWebAuthnCredentialService::class.java)
    private val properties = WebAuthnProviderProperties()
    private val controller = WebAuthnEnrollmentPublicController(
        registrations,
        credentials,
        AuthenticationAssuranceVerifier(
            DefaultAuthenticationAssurancePolicy(),
            Clock.fixed(now, ZoneOffset.UTC),
        ),
        properties,
    )

    @Test
    fun beginPinsRegistrationToCurrentManagedSessionPrincipal() {
        val started = WebAuthnRegistrationStart("A".repeat(43), "{}", now.plusSeconds(300))
        `when`(registrations.begin("u-1", "t-1")).thenReturn(started)

        assertEquals(started, controller.begin(request()))

        verify(registrations).begin("u-1", "t-1")
    }

    @Test
    fun finishUsesPathCeremonyAndNeverAcceptsUserOrTenantFromBody() {
        val body = WebAuthnRegistrationFinishRequest("{\"credential\":\"signed\"}", "Work laptop")
        val command = WebAuthnRegistrationFinishCommand("A".repeat(43), body.credentialResponseJson, body.displayName)
        val finished = mock(WebAuthnRegistrationFinish::class.java)
        `when`(registrations.finish("u-1", "t-1", command)).thenReturn(finished)

        assertEquals(finished, controller.finish(command.ceremonyId, body, request()))

        verify(registrations).finish("u-1", "t-1", command)
    }

    @Test
    fun listAndRevokeAreRestrictedToCurrentTenantUser() {
        val summary = mock(WebAuthnCredentialSummary::class.java)
        `when`(credentials.listActive("t-1", "u-1")).thenReturn(listOf(summary))
        `when`(credentials.revoke("t-1", "u-1", "credential-1")).thenReturn(true)
        val request = request()

        assertEquals(listOf(summary), controller.credentials(request))
        assertEquals(true, controller.revoke("credential-1", request))

        verify(credentials).listActive("t-1", "u-1")
        verify(credentials).revoke("t-1", "u-1", "credential-1")
    }

    @Test
    fun renameAcceptsOnlyDisplayNameAndPinsCredentialToCurrentTenantUser() {
        val summary = mock(WebAuthnCredentialSummary::class.java)
        val body = WebAuthnCredentialRenameRequest("Work laptop")
        `when`(credentials.rename("t-1", "u-1", "credential-1", body.displayName)).thenReturn(summary)

        assertEquals(summary, controller.rename("credential-1", body, request()))

        verify(credentials).rename("t-1", "u-1", "credential-1", "Work laptop")
    }

    @Test
    fun staleAuthenticationRequiresStepUpBeforeRegistration() {
        properties.operationReauthenticationMaxAgeSeconds = 10

        val error = assertFailsWith<AuthenticationAssuranceRequiredException> {
            controller.begin(request(authTime = now.minusSeconds(11)))
        }

        assertEquals(AuthenticationAssuranceReasonEnum.AUTHENTICATION_TOO_OLD, error.reason)
        assertEquals(10, error.maxAgeSeconds)
        verifyNoInteractions(registrations)
    }

    @Test
    fun staleAuthenticationCannotListCredentialMetadata() {
        properties.operationReauthenticationMaxAgeSeconds = 10

        val error = assertFailsWith<AuthenticationAssuranceRequiredException> {
            controller.credentials(request(authTime = now.minusSeconds(11)))
        }

        assertEquals(AuthenticationAssuranceReasonEnum.AUTHENTICATION_TOO_OLD, error.reason)
        verifyNoInteractions(credentials)
    }

    @Test
    fun mismatchedAuthenticationSessionFailsClosedBeforeRevocation() {
        val request = request().apply {
            val session = getAttribute(AuthenticationSession.REQUEST_ATTRIBUTE) as AuthenticationSession
            setAttribute(AuthenticationSession.REQUEST_ATTRIBUTE, session.copy(userId = "u-other"))
        }

        val error = assertFailsWith<AuthenticationAssuranceRequiredException> {
            controller.revoke("credential-1", request)
        }

        assertEquals(AuthenticationAssuranceReasonEnum.AUTHENTICATION_REQUIRED, error.reason)
        verifyNoInteractions(credentials)
    }

    private fun request(authTime: Instant = now) = MockHttpServletRequest().apply {
        getSession(true)!!.setAttribute(
            KudosContext.SESSION_KEY_USER,
            SessionUserPrincipal("u-1", "t-1", "alice"),
        )
        setAttribute(
            AuthenticationSession.REQUEST_ATTRIBUTE,
            AuthenticationSession(
                id = "s-1",
                tenantId = "t-1",
                userId = "u-1",
                username = "alice",
                authTime = authTime,
                amr = setOf("password"),
                acr = DefaultAuthenticationAssurancePolicy.ACR_PASSWORD,
                createdAt = now.minusSeconds(60),
                lastSeenAt = now,
                idleExpiresAt = now.plusSeconds(600),
                absoluteExpiresAt = now.plusSeconds(3_600),
            ),
        )
    }
}
