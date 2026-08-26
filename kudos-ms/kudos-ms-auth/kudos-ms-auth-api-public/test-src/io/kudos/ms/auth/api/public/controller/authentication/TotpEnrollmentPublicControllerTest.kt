package io.kudos.ms.auth.api.public.controller.authentication

import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.common.authentication.vo.TotpEnrollmentChallenge
import io.kudos.ms.auth.common.authentication.vo.request.ConfirmTotpEnrollmentRequest
import io.kudos.ms.auth.core.authentication.assurance.AuthenticationAssuranceReasonEnum
import io.kudos.ms.auth.core.authentication.assurance.AuthenticationAssuranceRequiredException
import io.kudos.ms.auth.core.authentication.assurance.AuthenticationAssuranceVerifier
import io.kudos.ms.auth.core.authentication.assurance.DefaultAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.mfa.TotpEnrollmentProperties
import io.kudos.ms.auth.core.authentication.mfa.service.iservice.ITotpEnrollmentService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class TotpEnrollmentPublicControllerTest {

    private val now = Instant.parse("2026-08-25T10:00:00Z")
    private val service = mock(ITotpEnrollmentService::class.java)
    private val properties = TotpEnrollmentProperties()
    private val controller = TotpEnrollmentPublicController(
        service,
        AuthenticationAssuranceVerifier(
            DefaultAuthenticationAssurancePolicy(),
            Clock.fixed(now, ZoneOffset.UTC),
        ),
        properties,
    )

    @Test
    fun beginPinsEnrollmentToCurrentManagedSessionPrincipal() {
        val challenge = TotpEnrollmentChallenge("e-1", "SECRET", "otpauth://totp/x", now.plusSeconds(300))
        `when`(service.begin("u-1", "t-1", "alice")).thenReturn(challenge)

        assertEquals(challenge, controller.begin(request()))

        verify(service).begin("u-1", "t-1", "alice")
    }

    @Test
    fun confirmNeverAcceptsUserOrTenantFromRequestBody() {
        `when`(service.confirm("e-1", "u-1", "t-1", 123456)).thenReturn(true)

        controller.confirm("e-1", ConfirmTotpEnrollmentRequest(123456), request())

        verify(service).confirm("e-1", "u-1", "t-1", 123456)
    }

    @Test
    fun staleAuthenticationReturnsStepUpRequirementBeforeEnrollment() {
        properties.operationReauthenticationMaxAgeSeconds = 10

        val error = assertFailsWith<AuthenticationAssuranceRequiredException> {
            controller.begin(request(authTime = now.minusSeconds(11)))
        }

        assertEquals(AuthenticationAssuranceReasonEnum.AUTHENTICATION_TOO_OLD, error.reason)
        assertEquals(10, error.maxAgeSeconds)
    }

    @Test
    fun mismatchedAuthenticationSessionFailsClosed() {
        val request = request().apply {
            val session = getAttribute(AuthenticationSession.REQUEST_ATTRIBUTE) as AuthenticationSession
            setAttribute(AuthenticationSession.REQUEST_ATTRIBUTE, session.copy(tenantId = "t-other"))
        }

        val error = assertFailsWith<AuthenticationAssuranceRequiredException> {
            controller.disable(request)
        }

        assertEquals(AuthenticationAssuranceReasonEnum.AUTHENTICATION_REQUIRED, error.reason)
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
