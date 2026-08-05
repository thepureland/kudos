package io.kudos.ms.auth.core.platform.authz.enforcement

import io.kudos.ability.security.enforcement.port.IPermissionPointRegistry
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.isNotNull
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.sys.core.resource.dao.SysResourceDao
import io.kudos.ms.sys.core.resource.model.po.SysResource
import jakarta.annotation.PreDestroy
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong


/**
 * Resolves an HTTP endpoint to the permission code guarding it, from the `sys_resource` registry.
 *
 * **URL syntax.** A row's `url` is either a bare path (`/api/admin/auth/role/bindUsers`, guarding
 * every method) or a method-qualified one (`POST:/api/admin/auth/role/bindUsers`). Ant patterns are
 * allowed on both forms — a path prefix followed by a double-star wildcard matches a whole subtree.
 *
 * **Which row wins.** A method-qualified match always beats a method-agnostic one; among equals the
 * longest pattern wins. Both rules exist so the answer never depends on row order — an
 * authorization decision that changes when somebody re-seeds a table is not a decision anyone can
 * reason about.
 *
 * **Freshness.** The snapshot is rebuilt on a TTL rather than driven by events, because permission
 * points are registered at deploy time (code-first, by the module that owns them), not edited by
 * operators during the day. A stale entry can at worst delay a *new* endpoint becoming reachable;
 * it can never resurrect a revoked grant, since the grant side is event-invalidated and consulted
 * separately.
 *
 * **The rebuild happens off the request path**, and that is load-bearing rather than an
 * optimisation. It was measured: with the reload running inline, whichever request found the
 * snapshot stale paid a full datasource connection timeout — 30 seconds against a stopped database —
 * before the filter had even reached the decision. Worse, serving the previous snapshot after a
 * failed reload (the right thing to do for availability) meant no exception escaped, so the
 * enforcement layer's circuit breaker recorded that 30-second request as a **success** and never
 * opened. Fail-soft on the data had quietly become fail-silent on the health signal. A cache whose
 * refresh can block must not refresh on the thread that is serving a request.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class PermissionPointRegistry : IPermissionPointRegistry {

    @Resource
    private lateinit var sysResourceDao: SysResourceDao

    /** How long a snapshot is reused, in milliseconds. */
    @Value("\${kudos.ms.auth.authz.permission-point-ttl-millis:60000}")
    private var ttlMillis: Long = 60_000

    /**
     * How long to wait before retrying a refresh that failed.
     *
     * Deliberately far shorter than the TTL — a failed load must not pin the registry to a stale
     * snapshot for a whole minute — and deliberately non-zero, so a storage outage does not have
     * every single request paying for its own failed attempt.
     */
    @Value("\${kudos.ms.auth.authz.permission-point-retry-backoff-millis:2000}")
    private var retryBackoffMillis: Long = 2_000

    private val pathMatcher = AntPathMatcher()
    private val lastLoadedAt = AtomicLong(0)

    /** Guards against queueing a second rebuild while one is already running. */
    private val refreshing = AtomicBoolean(false)

    /**
     * One daemon thread, so a rebuild can never hold up a request and can never hold up shutdown.
     */
    private val refreshExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "auth-permission-point-refresh").apply { isDaemon = true }
    }

    @Volatile
    private var points: List<PermissionPoint> = emptyList()

    private val log = LogFactory.getLog(this::class)

    override fun resolve(method: String, path: String): String? {
        val candidates = snapshot().filter { point ->
            (point.method == null || point.method.equals(method, ignoreCase = true)) &&
                pathMatcher.match(point.pathPattern, path)
        }
        if (candidates.isEmpty()) return null
        return candidates
            .sortedWith(
                compareByDescending<PermissionPoint> { it.method != null }
                    .thenByDescending { it.pathPattern.length },
            )
            .first()
            .permissionCode
    }

    /**
     * Discards the snapshot so the next [resolve] rebuilds it — for tests and for an admin-triggered
     * refresh.
     *
     * Deliberately synchronous in its effect: the next caller blocks on a real load. "Refresh now"
     * has to mean the answer after it is the fresh one, and an asynchronous invalidate would leave
     * the caller reading the very snapshot they asked to be rid of.
     */
    open fun invalidate() {
        points = emptyList()
        lastLoadedAt.set(0)
    }

    @PreDestroy
    open fun shutdown() {
        refreshExecutor.shutdownNow()
    }

    private fun snapshot(): List<PermissionPoint> {
        val current = points
        // Cold start: there is nothing to serve, so this one caller has to wait — and if the load
        // fails it must throw rather than return empty. "No endpoint is registered" and "the
        // registry could not be read" lead to the same refusal but are not the same fact, and only
        // the exception reaches the breaker as the failure it is.
        if (current.isEmpty() && lastLoadedAt.get() == 0L) return loadNow()
        refreshIfStale()
        return current
    }

    /** The blocking load, taken by exactly one thread; the rest read the result it publishes. */
    @Synchronized
    private fun loadNow(): List<PermissionPoint> {
        points.takeIf { it.isNotEmpty() }?.let { return it }
        val loaded = load()
        points = loaded
        lastLoadedAt.set(System.currentTimeMillis())
        return loaded
    }

    private fun refreshIfStale() {
        if (System.currentTimeMillis() - lastLoadedAt.get() < ttlMillis) return
        // Whoever loses this race simply serves the snapshot: a queue of identical rebuilds helps
        // nobody, and the one in flight will publish for all of them.
        if (!refreshing.compareAndSet(false, true)) return
        refreshExecutor.execute {
            try {
                points = load()
                lastLoadedAt.set(System.currentTimeMillis())
            } catch (e: Exception) {
                // The previous snapshot stays in place — a slightly stale copy of a table that
                // changes at deploy time beats refusing every endpoint as unregistered. The clock is
                // moved forward by a short backoff rather than a full TTL, so recovery is prompt
                // without a dead database being retried as fast as this thread can loop.
                lastLoadedAt.set(System.currentTimeMillis() - ttlMillis + retryBackoffMillis)
                log.warn(
                    "Failed to refresh the permission point registry; keeping the previous " +
                        "${points.size}-point snapshot and retrying in ${retryBackoffMillis}ms.",
                    e,
                )
            } finally {
                refreshing.set(false)
            }
        }
    }

    private fun load(): List<PermissionPoint> {
        val criteria = Criteria.and(
            SysResource::active eq true,
            SysResource::permissionCode.isNotNull(),
        )
        val rows = sysResourceDao.searchAs<SysResourceCacheEntry>(criteria)
        val loaded = rows.mapNotNull { it.toPoint() }
        log.debug("Loaded ${loaded.size} permission point(s) from the registry.")
        return loaded
    }

    private fun SysResourceCacheEntry.toPoint(): PermissionPoint? {
        val code = permissionCode?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val rawUrl = url?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val separatorAt = rawUrl.indexOf(':')
        // Only a leading token that looks like an HTTP method is a method qualifier; a colon inside
        // a path (rare, but legal) must not be mistaken for one.
        return if (separatorAt > 0 && rawUrl.substring(0, separatorAt).uppercase() in HTTP_METHODS) {
            PermissionPoint(rawUrl.substring(0, separatorAt).uppercase(), rawUrl.substring(separatorAt + 1), code)
        } else {
            PermissionPoint(null, rawUrl, code)
        }
    }

    /** One registered endpoint→code mapping. */
    private data class PermissionPoint(
        val method: String?,
        val pathPattern: String,
        val permissionCode: String,
    )

    companion object {
        private val HTTP_METHODS = setOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")
    }
}
