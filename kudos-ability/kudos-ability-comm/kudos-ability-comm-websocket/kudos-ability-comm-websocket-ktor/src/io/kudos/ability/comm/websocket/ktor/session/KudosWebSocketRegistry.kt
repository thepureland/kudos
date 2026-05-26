package io.kudos.ability.comm.websocket.ktor.session

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
 * deployments. For multi-instance "global broadcast", the business side must handle it
 * (typical solution: route each business message via Redis pub/sub, and each process's instance
 * of this class re-broadcasts to its own in-process sessions).
 *
 * Thread-safety: all indexes use `ConcurrentHashMap`, and inner `Set`s are also concurrent
 * via [java.util.Collections.newSetFromMap]. Write operations ([register] / [unregister])
 * update all three indexes; for simplicity the overall update is not atomic — there is a very
 * brief window where the primary index is written but the secondary indexes are not, so
 * business broadcast code must not assume strong atomicity.
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
     * Adds a session to the registry. A duplicate [KudosWebSocketSessionRef.sessionId] overwrites
     * the existing entry (not enforced — the business side is expected to ensure sessionId uniqueness).
     */
    fun register(session: KudosWebSocketSessionRef) {
        byId[session.sessionId] = session
        session.userId?.let { uid ->
            byUserId.computeIfAbsent(uid) { newConcurrentSet() }.add(session.sessionId)
        }
        session.tenantId?.let { tid ->
            byTenantId.computeIfAbsent(tid) { newConcurrentSet() }.add(session.sessionId)
        }
        log.debug("Registered WebSocket session sessionId={0} userId={1} tenantId={2} total={3}",
            session.sessionId, session.userId, session.tenantId, size)
    }

    /**
     * Removes a session from the registry. Does **not** actively close the connection — the
     * caller decides when to close it (typical usage: call this method in the route's `finally` block).
     */
    fun unregister(sessionId: String) {
        val session = byId.remove(sessionId) ?: return
        session.userId?.let { uid ->
            byUserId.compute(uid) { _, ids ->
                ids?.remove(sessionId)
                if (ids.isNullOrEmpty()) null else ids
            }
        }
        session.tenantId?.let { tid ->
            byTenantId.compute(tid) { _, ids ->
                ids?.remove(sessionId)
                if (ids.isNullOrEmpty()) null else ids
            }
        }
        log.debug("Unregistered WebSocket session sessionId={0} userId={1} tenantId={2} total={3}",
            sessionId, session.userId, session.tenantId, size)
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

    private fun newConcurrentSet(): MutableSet<String> =
        java.util.Collections.newSetFromMap(ConcurrentHashMap())
}
