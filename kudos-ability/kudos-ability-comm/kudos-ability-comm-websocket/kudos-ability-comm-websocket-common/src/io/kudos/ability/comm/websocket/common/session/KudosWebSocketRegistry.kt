package io.kudos.ability.comm.websocket.common.session

import io.kudos.base.logger.LogFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-level WebSocket session registry.
 *
 * Maintains three indexes in sync:
 *  - `sessionId → session` (primary index)
 *  - `userId → Set<sessionId>` (a user may be online on multiple devices)
 *  - `tenantId → Set<sessionId>` (broadcast by tenant)
 *
 * **Not** persisted and **not** synchronized across processes — sufficient for single-instance
 * deployments. For multi-instance "global broadcast" see
 * [io.kudos.ability.comm.websocket.common.distributed.DistributedWebSocketBroadcaster].
 *
 * ## Thread-safety
 *
 * All three indexes are `ConcurrentHashMap`s, and every mutation of a secondary bucket happens
 * inside [ConcurrentHashMap.compute] / [ConcurrentHashMap.computeIfPresent] on the bucket's key.
 * That matters: `ConcurrentHashMap` serializes compute calls for the same key, so an "add the
 * session" and a "the bucket is now empty, drop it" cannot interleave. Mutating the set outside
 * the compute lambda instead would let a concurrent removal drop the bucket between the lookup
 * and the add, leaving the new session in a set no longer reachable from the map — permanently
 * invisible to [findByUserId] / [findByTenantId] even though the connection is alive.
 *
 * The buckets themselves stay concurrent sets because readers iterate them outside the lock.
 *
 * Across the three indexes a write is still not atomic as a whole: [register] publishes to the
 * primary index first, so there is a brief window where `findById` resolves a session that
 * `findByUserId` does not yet list. Broadcasts must not assume otherwise.
 *
 * @author K
 * @since 1.0.0
 */
class KudosWebSocketRegistry {

    private val log = LogFactory.getLog(this::class)

    private val byId = ConcurrentHashMap<String, KudosWebSocketSessionRef>()
    private val byUserId = ConcurrentHashMap<String, MutableSet<String>>()
    private val byTenantId = ConcurrentHashMap<String, MutableSet<String>>()

    /** Total number of registered sessions. */
    val size: Int get() = byId.size

    /**
     * Adds a session to the registry.
     *
     * A duplicate [KudosWebSocketSessionRef.sessionId] replaces the existing entry. The replaced
     * session's `userId` / `tenantId` buckets are cleaned up when they differ from the new
     * session's, so a stale bucket cannot resolve the reused sessionId to the *new* session —
     * which would hand one user another user's connection.
     */
    fun register(session: KudosWebSocketSessionRef) {
        val replaced = byId.put(session.sessionId, session)
        if (replaced != null && replaced !== session) {
            replaced.userId?.takeIf { it != session.userId }?.let { detach(byUserId, it, session.sessionId) }
            replaced.tenantId?.takeIf { it != session.tenantId }?.let { detach(byTenantId, it, session.sessionId) }
        }
        session.userId?.let { attach(byUserId, it, session.sessionId) }
        session.tenantId?.let { attach(byTenantId, it, session.sessionId) }
        log.debug("Registered WebSocket session sessionId={0} userId={1} tenantId={2} total={3}",
            session.sessionId, session.userId, session.tenantId, size)
    }

    /**
     * Removes a session from the registry by id. Does **not** actively close the connection — the
     * caller decides when to close it.
     *
     * Prefer the [unregister] overload taking the session itself: it will not remove an entry that
     * a concurrent [register] has already replaced with a newer session under the same id.
     */
    fun unregister(sessionId: String) {
        val session = byId.remove(sessionId) ?: return
        detachSecondary(session, sessionId)
        log.debug("Unregistered WebSocket session sessionId={0} userId={1} tenantId={2} total={3}",
            sessionId, session.userId, session.tenantId, size)
    }

    /**
     * Removes [session] only if it is still the registered instance for its sessionId. A no-op when
     * the id has already been taken over by a different session — the typical usage is a route's
     * `finally` block, which must not evict a successor connection.
     */
    fun unregister(session: KudosWebSocketSessionRef) {
        if (!byId.remove(session.sessionId, session)) return
        detachSecondary(session, session.sessionId)
        log.debug("Unregistered WebSocket session sessionId={0} userId={1} tenantId={2} total={3}",
            session.sessionId, session.userId, session.tenantId, size)
    }

    /** Returns the session for the given sessionId, or `null` if not found. */
    fun findById(sessionId: String): KudosWebSocketSessionRef? = byId[sessionId]

    /** Returns a snapshot of all sessions for the given userId (not a live view, safe to iterate under concurrent modification). */
    fun findByUserId(userId: String): List<KudosWebSocketSessionRef> =
        byUserId[userId].orEmpty().mapNotNull { byId[it] }

    /** Returns a snapshot of all sessions for the given tenantId. */
    fun findByTenantId(tenantId: String): List<KudosWebSocketSessionRef> =
        byTenantId[tenantId].orEmpty().mapNotNull { byId[it] }

    /** Snapshot of all current sessions. */
    fun all(): List<KudosWebSocketSessionRef> = byId.values.toList()

    private fun detachSecondary(session: KudosWebSocketSessionRef, sessionId: String) {
        session.userId?.let { detach(byUserId, it, sessionId) }
        session.tenantId?.let { detach(byTenantId, it, sessionId) }
    }

    /** Adds [sessionId] to `index[key]`, creating the bucket if needed. See the class-level note on why this runs inside `compute`. */
    private fun attach(index: ConcurrentHashMap<String, MutableSet<String>>, key: String, sessionId: String) {
        index.compute(key) { _, ids ->
            (ids ?: newConcurrentSet()).apply { add(sessionId) }
        }
    }

    /** Removes [sessionId] from `index[key]`, dropping the bucket once it is empty. */
    private fun detach(index: ConcurrentHashMap<String, MutableSet<String>>, key: String, sessionId: String) {
        index.computeIfPresent(key) { _, ids ->
            ids.remove(sessionId)
            if (ids.isEmpty()) null else ids
        }
    }

    private fun newConcurrentSet(): MutableSet<String> =
        java.util.Collections.newSetFromMap(ConcurrentHashMap())
}
