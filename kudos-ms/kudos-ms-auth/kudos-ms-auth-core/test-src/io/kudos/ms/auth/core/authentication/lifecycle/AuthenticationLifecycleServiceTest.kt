package io.kudos.ms.auth.core.authentication.lifecycle

import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.common.authz.api.IPermissionVersionApi
import io.kudos.ms.auth.core.authentication.lifecycle.service.impl.AuthenticationLifecycleService
import io.kudos.ms.auth.core.authentication.session.service.iservice.IAuthenticationSessionService
import io.kudos.ms.auth.core.token.refresh.service.iservice.IRefreshTokenService
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AuthenticationLifecycleServiceTest {

    private val permissionVersionApi = mock(IPermissionVersionApi::class.java)
    private val sessionService = mock(IAuthenticationSessionService::class.java)
    private val refreshTokenService = mock(IRefreshTokenService::class.java)
    private val service = AuthenticationLifecycleService(permissionVersionApi, sessionService, refreshTokenService)

    @Test
    fun invalidateAll_bumpsEpochRevokesRefreshFamiliesAndLogicalSessions() {
        val sessions = listOf(session("s-1"), session("s-2"))
        whenCalled(permissionVersionApi.revokeAllTokens("u-1", "ACCOUNT_DISABLED")).thenReturn(9)
        whenCalled(sessionService.listForUser("t-1", "u-1")).thenReturn(sessions)
        whenCalled(refreshTokenService.revokeBySession("s-1", "ACCOUNT_DISABLED")).thenReturn(2)
        whenCalled(refreshTokenService.revokeBySession("s-2", "ACCOUNT_DISABLED")).thenReturn(1)
        whenCalled(sessionService.revokeAllForUser("t-1", "u-1", "ACCOUNT_DISABLED"))
            .thenReturn(sessions.map { it.copy(revokedAt = Instant.now()) })

        val result = service.invalidateAll("t-1", "u-1", " ACCOUNT_DISABLED ")

        assertEquals(9, result.tokenEpoch)
        assertEquals(2, result.revokedSessionCount)
        assertEquals(3, result.revokedRefreshTokenCount)
        val ordered = inOrder(permissionVersionApi, sessionService, refreshTokenService)
        ordered.verify(permissionVersionApi).revokeAllTokens("u-1", "ACCOUNT_DISABLED")
        ordered.verify(sessionService).listForUser("t-1", "u-1")
        ordered.verify(refreshTokenService).revokeBySession("s-1", "ACCOUNT_DISABLED")
        ordered.verify(refreshTokenService).revokeBySession("s-2", "ACCOUNT_DISABLED")
        ordered.verify(sessionService).revokeAllForUser("t-1", "u-1", "ACCOUNT_DISABLED")
    }

    private fun session(id: String): AuthenticationSession {
        val now = Instant.now()
        return AuthenticationSession(
            id = id,
            tenantId = "t-1",
            userId = "u-1",
            authTime = now,
            amr = setOf("password"),
            acr = "urn:kudos:acr:password",
            createdAt = now,
            lastSeenAt = now,
            idleExpiresAt = now.plusSeconds(60),
            absoluteExpiresAt = now.plusSeconds(120),
        )
    }
}
