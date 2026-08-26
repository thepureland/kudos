package io.kudos.ms.auth.core.authentication.session.spi

/**
 * Deletes the servlet container's own record of a session that has just been revoked.
 *
 * Revocation is already effective without this: every request re-validates its logical session, so a revoked
 * one stops working immediately whatever the container still holds. What the container record costs while it
 * lingers is storage, and the fact that a store operator can still read its attributes — which is why a
 * deployment that keeps sessions in a shared store usually wants them gone at the moment of revocation rather
 * than at the user's next request.
 *
 * Protocol-neutral on purpose: core neither picks a session store nor depends on Spring Session. An
 * implementation lives in an optional module, and deployments without one keep the lazy behaviour.
 *
 * **Failure semantics.** Purging is best-effort and must not throw back into the revocation path: the
 * revocation is the security act and has already been recorded, so failing it because a store was unreachable
 * would trade a real guarantee for a cosmetic one.
 */
fun interface IAuthenticationContainerSessionPurger {

    /**
     * @param tenantId the tenant the revoked sessions belong to
     * @param userId the subject whose sessions were revoked
     * @param logicalSessionIds the logical session ids revoked, never container session ids
     */
    fun purge(tenantId: String, userId: String, logicalSessionIds: Set<String>)
}
