package io.kudos.ms.auth.core.authentication.session.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.common.authentication.vo.AuthenticationContext
import io.kudos.ms.auth.core.authentication.assurance.DefaultAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.assurance.spi.IAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.session.model.AuthenticationSessionIssueCommand
import io.kudos.ms.auth.core.authentication.session.service.iservice.IAuthenticationSessionService
import io.kudos.ms.auth.core.authentication.session.spi.IAuthenticationContainerSessionPurger
import io.kudos.ms.auth.core.authentication.session.store.IAuthenticationSessionStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/** Issues and manages protocol-neutral Kudos session metadata. */
@Service
open class AuthenticationSessionService(
    private val store: IAuthenticationSessionStore,
    private val assurancePolicy: IAuthenticationAssurancePolicy = DefaultAuthenticationAssurancePolicy(),
    private val containerSessionPurger: IAuthenticationContainerSessionPurger? = null,
) : IAuthenticationSessionService {

    private val log = LogFactory.getLog(this::class)

    @Value($$"${kudos.ms.auth.session.idle-timeout-seconds:1800}")
    protected var idleTimeoutSeconds: Long = 1800

    @Value($$"${kudos.ms.auth.session.absolute-timeout-seconds:43200}")
    protected var absoluteTimeoutSeconds: Long = 43200

    override fun issue(command: AuthenticationSessionIssueCommand): AuthenticationSession {
        val context = command.context
        require(context.userId.isNotBlank()) { "Session user id must not be blank" }
        require(context.tenantId.isNotBlank()) { "Session tenant id must not be blank" }
        require(context.amr.isNotEmpty()) { "Session authentication methods must not be empty" }
        require(context.acr.isNotBlank()) { "Session authentication context class must not be blank" }
        val now = Instant.now()
        val effectiveAbsoluteTimeout = command.absoluteTimeoutSeconds
            ?.coerceIn(1, MAX_ABSOLUTE_TIMEOUT_SECONDS)
            ?: absoluteTimeoutSeconds.coerceAtLeast(1)
        val effectiveIdleTimeout = command.idleTimeoutSeconds
            ?.coerceIn(1, effectiveAbsoluteTimeout)
            ?: idleTimeoutSeconds.coerceAtLeast(1)
        val absoluteExpiresAt = now.plus(effectiveAbsoluteTimeout, ChronoUnit.SECONDS)
        val idleExpiresAt = minOf(
            now.plus(effectiveIdleTimeout, ChronoUnit.SECONDS),
            absoluteExpiresAt,
        )
        repeat(MAX_ID_GENERATION_ATTEMPTS) {
            val session = AuthenticationSession(
                id = UUID.randomUUID().toString(),
                tenantId = context.tenantId,
                userId = context.userId,
                username = command.username,
                clientId = command.clientId,
                deviceId = command.deviceId,
                authTime = context.authTime,
                amr = context.amr,
                acr = context.acr,
                riskLevel = context.riskLevel,
                credentialVersion = context.credentialVersion,
                loginIp = command.loginIp,
                loginDevice = command.loginDevice,
                loginBrowser = command.loginBrowser,
                loginOs = command.loginOs,
                userAgent = command.userAgent,
                createdAt = now,
                lastSeenAt = now,
                idleExpiresAt = idleExpiresAt,
                absoluteExpiresAt = absoluteExpiresAt,
            )
            if (store.create(session)) return session
        }
        error("Unable to allocate a unique authentication session id")
    }

    override fun get(id: String): AuthenticationSession? {
        val current = store.get(id) ?: return null
        if (current.revokedAt != null || current.isActive()) return current
        val now = Instant.now()
        val reason = if (!now.isBefore(current.absoluteExpiresAt)) ABSOLUTE_TIMEOUT else IDLE_TIMEOUT
        return store.save(
            current.copy(revokedAt = now, revokeReason = reason),
            current.version,
        ) ?: store.get(id)
    }

    override fun touch(id: String): AuthenticationSession? {
        val current = get(id)?.takeIf { it.isActive() } ?: return null
        val now = Instant.now()
        val idleExpiresAt = minOf(
            now.plus(idleTimeoutSeconds.coerceAtLeast(1), ChronoUnit.SECONDS),
            current.absoluteExpiresAt,
        )
        return store.save(
            current.copy(lastSeenAt = now, idleExpiresAt = idleExpiresAt),
            current.version,
        ) ?: store.get(id)?.takeIf { it.isActive() }
    }

    override fun elevateForUser(
        id: String,
        tenantId: String,
        userId: String,
        context: AuthenticationContext,
    ): AuthenticationSession? {
        require(tenantId.isNotBlank()) { "Session tenant id must not be blank" }
        require(userId.isNotBlank()) { "Session user id must not be blank" }
        require(context.tenantId == tenantId && context.userId == userId) {
            "Step-up authentication context does not match the session owner"
        }
        repeat(MAX_ELEVATE_ATTEMPTS) {
            val current = get(id)?.takeIf {
                it.isActive() && it.tenantId == tenantId && it.userId == userId
            } ?: return null
            require(assurancePolicy.isSatisfied(context.acr, current.acr)) {
                "Step-up authentication cannot reduce session assurance"
            }
            val now = Instant.now()
            val elevated = current.copy(
                authTime = context.authTime,
                amr = current.amr + context.amr,
                acr = context.acr,
                riskLevel = context.riskLevel ?: current.riskLevel,
                credentialVersion = maxOf(current.credentialVersion, context.credentialVersion),
                lastSeenAt = now,
                idleExpiresAt = minOf(
                    now.plus(idleTimeoutSeconds.coerceAtLeast(1), ChronoUnit.SECONDS),
                    current.absoluteExpiresAt,
                ),
            )
            store.save(elevated, current.version)?.let { return it }
        }
        return store.get(id)?.takeIf {
            it.tenantId == tenantId && it.userId == userId && it.isActive() &&
                assurancePolicy.isSatisfied(it.acr, context.acr)
        }
    }

    override fun listForUser(tenantId: String, userId: String): List<AuthenticationSession> {
        require(tenantId.isNotBlank()) { "Session tenant id must not be blank" }
        require(userId.isNotBlank()) { "Session user id must not be blank" }
        return store.findByUser(tenantId, userId)
            .mapNotNull { get(it.id) }
            .filter { it.isActive() && it.tenantId == tenantId && it.userId == userId }
            .sortedByDescending(AuthenticationSession::lastSeenAt)
    }

    override fun revokeForUser(
        id: String,
        tenantId: String,
        userId: String,
        reason: String,
    ): AuthenticationSession? = revokeOne(id, tenantId, userId, reason)
        ?.also { purgeContainerSessions(tenantId, userId, setOf(it.id)) }

    private fun revokeOne(
        id: String,
        tenantId: String,
        userId: String,
        reason: String,
    ): AuthenticationSession? {
        require(tenantId.isNotBlank()) { "Session tenant id must not be blank" }
        require(userId.isNotBlank()) { "Session user id must not be blank" }
        require(reason.isNotBlank()) { "Session revoke reason must not be blank" }
        repeat(MAX_REVOKE_ATTEMPTS) {
            val current = get(id)?.takeIf { it.tenantId == tenantId && it.userId == userId } ?: return null
            if (current.revokedAt != null) return current
            store.save(
                current.copy(
                    revokedAt = Instant.now(),
                    revokeReason = reason.trim().take(MAX_REVOKE_REASON_LENGTH),
                ),
                current.version,
            )?.let { return it }
        }
        return store.get(id)?.takeIf {
            it.tenantId == tenantId && it.userId == userId && it.revokedAt != null
        }
    }

    override fun revokeAllForUser(
        tenantId: String,
        userId: String,
        reason: String,
    ): List<AuthenticationSession> {
        require(tenantId.isNotBlank()) { "Session tenant id must not be blank" }
        require(userId.isNotBlank()) { "Session user id must not be blank" }
        require(reason.isNotBlank()) { "Session revoke reason must not be blank" }
        // One purge for the whole batch rather than one per session: the store lookup an implementation has to
        // make is per user, not per session, so paying it N times would be N-1 lookups for nothing.
        return store.findByUser(tenantId, userId)
            .mapNotNull { get(it.id)?.takeIf(AuthenticationSession::isActive) }
            .mapNotNull { revokeOne(it.id, tenantId, userId, reason) }
            .also { revoked ->
                if (revoked.isNotEmpty()) {
                    purgeContainerSessions(tenantId, userId, revoked.map { it.id }.toSet())
                }
            }
    }

    /**
     * Best-effort, and deliberately swallowing failures.
     *
     * The revocation is already durable and every request re-validates its session, so a revoked session is
     * dead whether or not the container's copy was deleted. Letting a purge failure propagate would turn an
     * unreachable session store into a failed revocation — trading the guarantee that matters for the one
     * that does not.
     */
    private fun purgeContainerSessions(tenantId: String, userId: String, logicalSessionIds: Set<String>) {
        val purger = containerSessionPurger ?: return
        try {
            purger.purge(tenantId, userId, logicalSessionIds)
        } catch (e: Exception) {
            log.error(
                e,
                "Failed to purge container sessions for user $userId; the revocation itself stands and the " +
                    "container records will be invalidated on their next request",
            )
        }
    }

    companion object {
        private const val MAX_ID_GENERATION_ATTEMPTS = 3
        private const val MAX_REVOKE_ATTEMPTS = 5
        private const val MAX_ELEVATE_ATTEMPTS = 5
        private const val MAX_REVOKE_REASON_LENGTH = 128
        private const val MAX_ABSOLUTE_TIMEOUT_SECONDS = 31_536_000L
        private const val IDLE_TIMEOUT = "IDLE_TIMEOUT"
        private const val ABSOLUTE_TIMEOUT = "ABSOLUTE_TIMEOUT"
    }
}
