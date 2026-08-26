package io.kudos.ms.auth.core.authentication.lifecycle.service.impl

import io.kudos.ms.auth.common.authz.api.IPermissionVersionApi
import io.kudos.ms.auth.core.authentication.lifecycle.model.AuthenticationInvalidationResult
import io.kudos.ms.auth.core.authentication.lifecycle.service.iservice.IAuthenticationLifecycleService
import io.kudos.ms.auth.core.authentication.session.service.iservice.IAuthenticationSessionService
import io.kudos.ms.auth.core.token.refresh.service.iservice.IRefreshTokenService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/** Security boundary for changes that make every previously authenticated client untrusted. */
@Service
open class AuthenticationLifecycleService(
    private val permissionVersionApi: IPermissionVersionApi,
    private val sessionService: IAuthenticationSessionService,
    private val refreshTokenService: IRefreshTokenService,
) : IAuthenticationLifecycleService {

    // Suspend any caller transaction so the epoch bump commits before best-effort secondary-store cleanup starts.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun invalidateAll(
        tenantId: String,
        userId: String,
        reason: String,
    ): AuthenticationInvalidationResult {
        require(tenantId.isNotBlank()) { "Tenant id must not be blank" }
        require(userId.isNotBlank()) { "User id must not be blank" }
        val sanitizedReason = reason.trim().take(MAX_REASON_LENGTH)
        require(sanitizedReason.isNotEmpty()) { "Authentication invalidation reason must not be blank" }

        // Bump first: even if a later store operation fails, access-token freshness and refresh rotation fail closed.
        val tokenEpoch = permissionVersionApi.revokeAllTokens(userId, sanitizedReason)
        val sessions = sessionService.listForUser(tenantId, userId)
        val revokedRefreshTokens = sessions.sumOf {
            refreshTokenService.revokeBySession(it.id, sanitizedReason)
        }
        val revokedSessions = sessionService.revokeAllForUser(tenantId, userId, sanitizedReason)
        return AuthenticationInvalidationResult(
            tokenEpoch = tokenEpoch,
            revokedSessionCount = revokedSessions.size,
            revokedRefreshTokenCount = revokedRefreshTokens,
        )
    }

    private companion object {
        const val MAX_REASON_LENGTH = 128
    }
}
