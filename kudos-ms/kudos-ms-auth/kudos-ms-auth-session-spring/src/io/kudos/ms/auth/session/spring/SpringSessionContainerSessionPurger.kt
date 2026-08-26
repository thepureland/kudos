package io.kudos.ms.auth.session.spring

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.core.authentication.session.spi.IAuthenticationContainerSessionPurger
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.session.Session

/**
 * Deletes the container's session records for logical sessions that were just revoked.
 *
 * **How the two ids are matched without storing either against the other.** A container session id is, in
 * effect, the session cookie — a live credential. Recording it in the auth tables would put a credential in a
 * place that management APIs read from, so the mapping is taken from where it already exists: each container
 * session carries the logical session id as an attribute, written when the session was issued. The purger asks
 * the principal-indexed repository for the user's sessions and deletes the ones whose attribute matches.
 *
 * A deployment whose store does not populate the principal index will simply find nothing here; sessions are
 * then invalidated on their next request as before, which is what happens without this module at all.
 */
open class SpringSessionContainerSessionPurger(
    private val sessionRepository: FindByIndexNameSessionRepository<out Session>,
) : IAuthenticationContainerSessionPurger {
    private val log = LogFactory.getLog(this::class)

    override fun purge(tenantId: String, userId: String, logicalSessionIds: Set<String>) {
        if (logicalSessionIds.isEmpty()) return
        val candidates = sessionRepository.findByPrincipalName(userId)
        if (candidates.isEmpty()) return
        var deleted = 0
        candidates.forEach { (containerSessionId, session) ->
            val logicalId = session.getAttribute<Any>(AuthenticationSession.HTTP_SESSION_ATTRIBUTE) as? String
            // Only sessions this revocation actually covers: the principal index answers "this user's
            // sessions", which is a superset when a single session was revoked.
            if (logicalId != null && logicalId in logicalSessionIds) {
                sessionRepository.deleteById(containerSessionId)
                deleted++
            }
        }
        log.debug(
            "Purged $deleted container session(s) for user $userId out of ${logicalSessionIds.size} " +
                "revoked logical session(s)"
        )
    }
}
