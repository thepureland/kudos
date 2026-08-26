package io.kudos.ms.auth.api.public.controller.authentication

import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.common.authentication.vo.RecoveryCodeSet
import io.kudos.ms.auth.core.authentication.assurance.AuthenticationAssuranceReasonEnum
import io.kudos.ms.auth.core.authentication.assurance.AuthenticationAssuranceRequiredException
import io.kudos.ms.auth.core.authentication.assurance.AuthenticationAssuranceVerifier
import io.kudos.ms.auth.core.authentication.assurance.DefaultAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.mfa.recovery.RecoveryCodeProperties
import io.kudos.ms.auth.core.authentication.mfa.recovery.service.iservice.IRecoveryCodeService
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

internal class RecoveryCodePublicControllerTest {

    private val now = Instant.parse("2026-08-25T10:00:00Z")
    private val service = mock(IRecoveryCodeService::class.java)
    private val properties = RecoveryCodeProperties()
    private val controller = RecoveryCodePublicController(
        service,
        AuthenticationAssuranceVerifier(
            DefaultAuthenticationAssurancePolicy(),
            Clock.fixed(now, ZoneOffset.UTC),
        ),
        properties,
    )

    @Test
    fun generationIsPinnedToCurrentManagedSessionPrincipal() {
        val codes = RecoveryCodeSet(listOf("2345-6789-ABCD-EFGH"), now)
        `when`(service.generate("u-1", "t-1")).thenReturn(codes)

        assertEquals(codes, controller.generate(request()))

        verify(service).generate("u-1", "t-1")
    }

    @Test
    fun staleAuthenticationRequiresStepUpBeforeRotation() {
        properties.operationReauthenticationMaxAgeSeconds = 10

        val error = assertFailsWith<AuthenticationAssuranceRequiredException> {
            controller.generate(request(authTime = now.minusSeconds(11)))
        }

        assertEquals(AuthenticationAssuranceReasonEnum.AUTHENTICATION_TOO_OLD, error.reason)
    }

    @Test
    fun mismatchedAuthenticationSessionFailsClosed() {
        val request = request().apply {
            val session = getAttribute(AuthenticationSession.REQUEST_ATTRIBUTE) as AuthenticationSession
            setAttribute(AuthenticationSession.REQUEST_ATTRIBUTE, session.copy(userId = "u-other"))
        }

        val error = assertFailsWith<AuthenticationAssuranceRequiredException> {
            controller.revoke(request)
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
