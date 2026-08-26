package io.kudos.ms.auth.core.token.refresh.service.impl

import io.kudos.ms.auth.common.authentication.vo.AuthenticationContext
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.core.authentication.session.model.AuthenticationSessionIssueCommand
import io.kudos.ms.auth.core.authentication.session.service.iservice.IAuthenticationSessionService
import io.kudos.ms.auth.core.token.refresh.dao.AuthRefreshTokenDao
import io.kudos.ms.auth.core.token.refresh.model.IssuedRefreshToken
import io.kudos.ms.auth.core.token.refresh.model.RefreshTokenException
import io.kudos.ms.auth.core.token.refresh.model.po.AuthRefreshToken
import io.kudos.ms.auth.core.token.refresh.service.iservice.IRefreshTokenService
import io.kudos.ms.auth.core.version.dao.AuthPrincipalVersionDao
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID

/** Hash-only refresh-token families with one-time rotation and replay containment. */
@Service
@Transactional
open class RefreshTokenService(
    private val dao: AuthRefreshTokenDao,
    private val sessionService: IAuthenticationSessionService,
    private val principalVersionDao: AuthPrincipalVersionDao,
) : IRefreshTokenService {

    @Value($$"${kudos.ms.auth.token.refresh-ttl-seconds:2592000}")
    protected var refreshTtlSeconds: Long = 2_592_000

    private val secureRandom = SecureRandom()

    override fun issue(
        sourceSessionId: String,
        clientId: String?,
        deviceId: String?,
    ): IssuedRefreshToken {
        val source = sessionService.get(sourceSessionId)?.takeIf(AuthenticationSession::isActive)
            ?: invalid()
        val ttl = refreshTtlSeconds.coerceIn(1, MAX_REFRESH_TTL_SECONDS)
        val tokenSession = sessionService.issue(
            AuthenticationSessionIssueCommand(
                context = AuthenticationContext(
                    userId = source.userId,
                    tenantId = source.tenantId,
                    authTime = source.authTime,
                    amr = source.amr,
                    acr = source.acr,
                    credentialVersion = source.credentialVersion,
                    riskLevel = source.riskLevel,
                ),
                username = source.username,
                clientId = clientId?.trim()?.takeIf(String::isNotEmpty)?.take(MAX_CLIENT_ID_LENGTH),
                deviceId = deviceId?.trim()?.takeIf(String::isNotEmpty)?.take(MAX_DEVICE_ID_LENGTH),
                loginIp = source.loginIp,
                loginDevice = source.loginDevice,
                loginBrowser = source.loginBrowser,
                loginOs = source.loginOs,
                userAgent = source.userAgent,
                idleTimeoutSeconds = ttl,
                absoluteTimeoutSeconds = ttl,
            )
        )
        try {
            return createInitial(tokenSession)
        } catch (e: Exception) {
            sessionService.revokeForUser(
                tokenSession.id,
                tokenSession.tenantId,
                tokenSession.userId,
                TOKEN_ISSUE_FAILED,
            )
            throw e
        }
    }

    @Transactional(noRollbackFor = [RefreshTokenException::class])
    override fun rotate(token: String): IssuedRefreshToken {
        val raw = normalize(token)
        val parent = dao.findByTokenHash(sha256(raw)) ?: invalid()
        val now = utcNow()
        if (parent.consumedAt != null) compromise(parent, now)
        if (parent.revokedAt != null || !parent.expiresAt.isAfter(now)) invalid()
        val session = sessionService.touch(parent.sessionId)?.takeIf(AuthenticationSession::isActive)
            ?: run {
                dao.revokeFamily(parent.familyId, SESSION_INACTIVE, now)
                invalid()
            }
        if (currentTokenEpoch(session.userId) != parent.tokenEpoch) {
            dao.revokeFamily(parent.familyId, TOKEN_EPOCH_STALE, now)
            sessionService.revokeForUser(
                session.id,
                session.tenantId,
                session.userId,
                TOKEN_EPOCH_STALE,
            )
            invalid()
        }

        val rawChild = newToken()
        val child = newRecord(
            sessionId = parent.sessionId,
            familyId = parent.familyId,
            parentId = parent.id,
            rawToken = rawChild,
            tokenEpoch = parent.tokenEpoch,
            issuedAt = now,
            expiresAt = parent.expiresAt,
        )
        if (!dao.consume(parent.id, child.id, now)) compromise(parent, now)
        dao.insert(child)
        return issued(rawChild, child, session)
    }

    override fun revoke(token: String, reason: String): Boolean {
        val record = dao.findByTokenHash(sha256(normalize(token))) ?: return false
        val sanitizedReason = sanitizeReason(reason)
        val revoked = dao.revokeFamily(record.familyId, sanitizedReason, utcNow()) > 0
        sessionService.get(record.sessionId)?.let { session ->
            sessionService.revokeForUser(
                session.id,
                session.tenantId,
                session.userId,
                sanitizedReason,
            )
        }
        return revoked
    }

    override fun revokeBySession(sessionId: String, reason: String): Int {
        require(sessionId.isNotBlank()) { "Session id must not be blank" }
        return dao.revokeBySession(sessionId, sanitizeReason(reason), utcNow())
    }

    private fun createInitial(session: AuthenticationSession): IssuedRefreshToken {
        val now = utcNow()
        val raw = newToken()
        val familyId = UUID.randomUUID().toString()
        val record = newRecord(
            sessionId = session.id,
            familyId = familyId,
            parentId = null,
            rawToken = raw,
            tokenEpoch = currentTokenEpoch(session.userId),
            issuedAt = now,
            expiresAt = LocalDateTime.ofInstant(session.absoluteExpiresAt, ZoneOffset.UTC),
        )
        dao.insert(record)
        return issued(raw, record, session)
    }

    private fun newRecord(
        sessionId: String,
        familyId: String,
        parentId: String?,
        rawToken: String,
        tokenEpoch: Long,
        issuedAt: LocalDateTime,
        expiresAt: LocalDateTime,
    ): AuthRefreshToken = AuthRefreshToken {
        id = UUID.randomUUID().toString()
        this.sessionId = sessionId
        this.familyId = familyId
        this.parentId = parentId
        tokenHash = sha256(rawToken)
        this.tokenEpoch = tokenEpoch
        this.issuedAt = issuedAt
        this.expiresAt = expiresAt
    }

    private fun issued(
        rawToken: String,
        record: AuthRefreshToken,
        session: AuthenticationSession,
    ): IssuedRefreshToken = IssuedRefreshToken(
        token = rawToken,
        session = session,
        familyId = record.familyId,
        expiresAt = record.expiresAt.toInstant(ZoneOffset.UTC),
    )

    private fun compromise(record: AuthRefreshToken, now: LocalDateTime): Nothing {
        dao.revokeFamily(record.familyId, REFRESH_TOKEN_REUSE, now)
        sessionService.get(record.sessionId)?.let { session ->
            sessionService.revokeForUser(
                session.id,
                session.tenantId,
                session.userId,
                REFRESH_TOKEN_REUSE,
            )
        }
        throw RefreshTokenException(INVALID_REFRESH_TOKEN, reuseDetected = true)
    }

    private fun normalize(token: String): String {
        val normalized = token.trim()
        if (!normalized.startsWith(TOKEN_PREFIX) || normalized.length != TOKEN_LENGTH) invalid()
        return normalized
    }

    private fun sanitizeReason(reason: String): String {
        require(reason.isNotBlank()) { "Refresh-token revoke reason must not be blank" }
        return reason.trim().take(MAX_REASON_LENGTH)
    }

    private fun newToken(): String = TOKEN_PREFIX + ByteArray(TOKEN_BYTES)
        .also(secureRandom::nextBytes)
        .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun utcNow(): LocalDateTime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)

    private fun currentTokenEpoch(userId: String): Long =
        principalVersionDao.findByPrincipalId(userId)?.epoch ?: 0L

    private fun invalid(): Nothing = throw RefreshTokenException(INVALID_REFRESH_TOKEN)

    private companion object {
        const val TOKEN_PREFIX = "krt_"
        const val TOKEN_BYTES = 32
        const val TOKEN_LENGTH = 47
        const val MAX_CLIENT_ID_LENGTH = 128
        const val MAX_DEVICE_ID_LENGTH = 128
        const val MAX_REASON_LENGTH = 128
        const val MAX_REFRESH_TTL_SECONDS = 31_536_000L
        const val INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN"
        const val REFRESH_TOKEN_REUSE = "REFRESH_TOKEN_REUSE"
        const val SESSION_INACTIVE = "SESSION_INACTIVE"
        const val TOKEN_EPOCH_STALE = "TOKEN_EPOCH_STALE"
        const val TOKEN_ISSUE_FAILED = "TOKEN_ISSUE_FAILED"
    }
}
