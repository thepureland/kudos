package io.kudos.ms.auth.core.authentication.assurance

import io.kudos.ms.auth.common.authentication.annotation.RequiresAuthenticationAssurance
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class AuthenticationAssuranceVerifierTest {

    private val now = Instant.parse("2026-08-25T10:00:00Z")
    private val verifier = AuthenticationAssuranceVerifier(
        DefaultAuthenticationAssurancePolicy(),
        Clock.fixed(now, ZoneOffset.UTC),
    )

    @Test
    fun strongerAndFreshSession_isAccepted() {
        verifier.verify(session(acr = DefaultAuthenticationAssurancePolicy.ACR_MFA),
            DefaultAuthenticationAssurancePolicy.ACR_PASSWORD, 300)
    }

    @Test
    fun exactFreshnessBoundary_isAccepted() {
        verifier.verify(session(authTime = now.minusSeconds(300)),
            DefaultAuthenticationAssurancePolicy.ACR_PASSWORD, 300)
    }

    @Test
    fun disabledFreshness_acceptsOldAuthentication() {
        verifier.verify(session(authTime = now.minusSeconds(86_400)),
            DefaultAuthenticationAssurancePolicy.ACR_PASSWORD,
            RequiresAuthenticationAssurance.NO_MAX_AGE)
    }

    @Test
    fun missingSession_requiresAuthentication() {
        val error = assertFailsWith<AuthenticationAssuranceRequiredException> {
            verifier.verify(null, DefaultAuthenticationAssurancePolicy.ACR_MFA, 300)
        }

        assertEquals(AuthenticationAssuranceReasonEnum.AUTHENTICATION_REQUIRED, error.reason)
        assertEquals(DefaultAuthenticationAssurancePolicy.ACR_MFA, error.requiredAcr)
        assertEquals(300, error.maxAgeSeconds)
    }

    @Test
    fun weakerSession_requiresHigherAcr() {
        val error = assertFailsWith<AuthenticationAssuranceRequiredException> {
            verifier.verify(session(), DefaultAuthenticationAssurancePolicy.ACR_MFA, 300)
        }

        assertEquals(AuthenticationAssuranceReasonEnum.INSUFFICIENT_ACR, error.reason)
    }

    @Test
    fun staleAuthentication_requiresRecentAuthentication() {
        val error = assertFailsWith<AuthenticationAssuranceRequiredException> {
            verifier.verify(session(authTime = now.minusSeconds(301)),
                DefaultAuthenticationAssurancePolicy.ACR_PASSWORD, 300)
        }

        assertEquals(AuthenticationAssuranceReasonEnum.AUTHENTICATION_TOO_OLD, error.reason)
    }

    @Test
    fun inactiveSession_isTreatedAsUnauthenticated() {
        val error = assertFailsWith<AuthenticationAssuranceRequiredException> {
            verifier.verify(session(idleExpiresAt = now), DefaultAuthenticationAssurancePolicy.ACR_PASSWORD, -1)
        }

        assertEquals(AuthenticationAssuranceReasonEnum.AUTHENTICATION_REQUIRED, error.reason)
    }

    private fun session(
        acr: String = DefaultAuthenticationAssurancePolicy.ACR_PASSWORD,
        authTime: Instant = now,
        idleExpiresAt: Instant = now.plusSeconds(600),
    ) = AuthenticationSession(
        id = "s-1",
        tenantId = "t-1",
        userId = "u-1",
        authTime = authTime,
        amr = setOf("password"),
        acr = acr,
        createdAt = now.minusSeconds(600),
        lastSeenAt = now,
        idleExpiresAt = idleExpiresAt,
        absoluteExpiresAt = now.plusSeconds(3_600),
    )
}
