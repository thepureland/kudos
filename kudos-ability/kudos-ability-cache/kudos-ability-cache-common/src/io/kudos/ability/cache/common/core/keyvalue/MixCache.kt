package io.kudos.ability.cache.common.core.keyvalue

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.notify.CacheMessage
import io.kudos.ability.cache.common.notify.ICacheMessageHandler
import io.kudos.base.logger.LogFactory
import io.kudos.context.kit.SpringKit
import jakarta.annotation.Nullable
import org.springframework.cache.Cache
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Mixed cache (two-tier cache: local + remote).
 *
 * @author K
 * @since 1.0.0
 */
class MixCache(
    val strategy: CacheStrategy,
    private val localCache: Cache?,
    private val remoteCache: Cache?
) : Cache {

    override fun getName(): String = when (strategy) {
        CacheStrategy.SINGLE_LOCAL, CacheStrategy.LOCAL_REMOTE -> requireLocal().name
        CacheStrategy.REMOTE -> requireRemote().name
    }

    override fun getNativeCache(): Any {
        return this
    }

    @Nullable
    override fun get(key: Any): Cache.ValueWrapper? {
        return when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> requireLocal().get(key)
            CacheStrategy.REMOTE -> remoteCache?.get(key)
            CacheStrategy.LOCAL_REMOTE -> mixGet(key)
        }
    }

    @Nullable
    override fun <T : Any> get(key: Any, @Nullable type: Class<T>?): T? {
        return when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> requireLocal().get(key, type)
            CacheStrategy.REMOTE -> requireRemote().get(key, type)
            CacheStrategy.LOCAL_REMOTE -> {
                val wrapper = mixGet(key) ?: return null
                val value = wrapper.get()
                if (value != null && type != null && !type.isInstance(value)) {
                    throw IllegalStateException(
                        "Cached value is not of required type [${type.name}]: $value"
                    )
                }
                @Suppress("UNCHECKED_CAST")
                value as T?
            }
        }
    }

    @Nullable
    override fun <T : Any> get(key: Any, valueLoader: Callable<T>): T? {
        return when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> requireLocal().get(key, valueLoader)
            CacheStrategy.REMOTE -> requireRemote().get(key, valueLoader)
            CacheStrategy.LOCAL_REMOTE -> mixGetOrLoad(key, valueLoader)
        }
    }

    /**
     * Two-tier cache read: a local hit (including a cached null, used to prevent cache penetration) is returned directly;
     * only a true local miss queries the remote tier; a remote hit backfills the local tier (preserving null semantics);
     * if both tiers miss, returns null without performing any write.
     */
    private fun mixGet(key: Any): Cache.ValueWrapper? {
        val local = requireLocal()
        local.get(key)?.let { return it }
        val remoteWrapper = remoteCache?.get(key) ?: return null
        local.put(key, remoteWrapper.get())
        return remoteWrapper
    }

    /**
     * Two-tier cache read + load-on-miss: same semantics as [mixGet]; when both tiers miss, invokes valueLoader once and
     * writes into both tiers (does not broadcast evict — aligning with mixGet's backfill behavior, so the read path is not turned into a write path).
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> mixGetOrLoad(key: Any, valueLoader: Callable<T>): T? {
        val local = requireLocal()
        local.get(key)?.let { return it.get() as T? }
        remoteCache?.get(key)?.let { wrapper ->
            local.put(key, wrapper.get())
            return wrapper.get() as T?
        }
        val loaded: T? = valueLoader.call()
        remoteCache?.put(key, loaded)
        local.put(key, loaded)
        return loaded
    }

    /**
     * Returns [localCache]; expected to be non-null based on strategy. If missing, throws an error containing the strategy info for diagnostics.
     *
     * @return local cache
     * @throws IllegalArgumentException when [localCache] is null
     * @author K
     * @since 1.0.0
     */
    private fun requireLocal(): Cache =
        requireNotNull(localCache) { "localCache is null for strategy $strategy" }

    /**
     * Returns [remoteCache]; expected to be non-null based on strategy. If missing, throws an error containing the strategy info for diagnostics.
     *
     * @return remote cache
     * @throws IllegalArgumentException when [remoteCache] is null
     * @author K
     * @since 1.0.0
     */
    private fun requireRemote(): Cache =
        requireNotNull(remoteCache) { "remoteCache is null for strategy $strategy" }

    /**
     * Unified write-through + broadcast template.
     * - SINGLE_LOCAL: only touches local.
     * - REMOTE: only touches remote.
     * - LOCAL_REMOTE: remote first, then local, then broadcasts invalidation to other nodes ([notifyKey] = null means "clear all").
     *
     * **Failure semantics** (contract documented in the README):
     *
     *  | Stage             | Exception handling                                                                |
     *  |-------------------|----------------------------------------------------------------------------------|
     *  | Remote write fails| Exception propagates -> no local write, no broadcast. Cluster keeps the old remote value -> consistent. |
     *  | Local write fails | Exception is **swallowed** + WARN logged -> broadcast still fires. Semantics: "remote is the source of truth and has been updated; a local write failure is a degradation, self-healed by the next miss or TTL". |
     *  | Broadcast fails   | Already fire-and-forget (caught inside [pushMsgRedis]).                          |
     *
     * Write order "remote then local" — if the broadcast/finally link fails or the message is dropped, this node's local copy is at least
     * consistent with the remote version, rather than local-new/remote-old (the latter ordering is much harder to reason about under distributed failure).
     *
     * [opName] is only used for debug logging; the relationship between [notifyKey] and [action] is a contract between callers.
     */
    private inline fun writeThrough(opName: String, notifyKey: Any?, action: (Cache) -> Unit) {
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> action(requireLocal())
            CacheStrategy.REMOTE -> action(requireRemote())
            CacheStrategy.LOCAL_REMOTE -> {
                action(requireRemote())
                // A local failure must not block the "remote already updated" broadcast — otherwise other nodes' local
                // copies would stay stale. Catch + warn, then continue broadcasting so the cluster eventually converges with remote.
                try {
                    action(requireLocal())
                } catch (t: Throwable) {
                    log.warn(
                        "Local cache write failed (remote already succeeded); will continue broadcasting to invalidate local copies on other nodes; this node's local copy will self-heal on next miss / TTL op={0} key={1} cause={2}",
                        opName, notifyKey, t.message
                    )
                }
                val name = getName()
                log.debug("{0} remote cache {1}. key={2}", opName, name, notifyKey)
                pushMsgRedis(name, notifyKey)
            }
        }
    }

    override fun evict(key: Any) = writeThrough("evict", key) { it.evict(key) }

    override fun put(key: Any, @Nullable value: Any?) = writeThrough("put", key) { it.put(key, value) }

    /**
     * The LOCAL_REMOTE branch of putIfAbsent has finer semantics than ordinary writes:
     * - Remote putIfAbsent fails (already present) -> do not broadcast invalidation, but backfill local to avoid going to the source again on this node.
     * - Remote putIfAbsent succeeds (newly inserted) -> synchronously write local + broadcast invalidation.
     * The [writeThrough] template performs action+broadcast unconditionally and cannot express the branching above, so an explicit `when` is preserved here.
     */
    @Nullable
    override fun putIfAbsent(key: Any, @Nullable value: Any?): Cache.ValueWrapper? = when (strategy) {
        CacheStrategy.SINGLE_LOCAL -> requireLocal().putIfAbsent(key, value)
        CacheStrategy.REMOTE -> requireRemote().putIfAbsent(key, value)
        CacheStrategy.LOCAL_REMOTE -> {
            val remote = requireRemote()
            val local = requireLocal()
            val existed = remote.putIfAbsent(key, value)
            if (existed == null) {
                // Remote putIfAbsent succeeded — same failure semantics as [writeThrough]: still broadcast on local failure.
                try {
                    local.putIfAbsent(key, value)
                } catch (t: Throwable) {
                    log.warn("Local cache write failed (remote already succeeded); will continue broadcasting putIfAbsent key={0} cause={1}",
                        key, t.message)
                }
                val name = getName()
                log.debug("putIfAbsent remote cache {0}. key={1}", name, key)
                pushMsgRedis(name, key)
                null
            } else {
                // Remote already contains the value — backfill local to avoid hitting the source again on this node. This is read semantics, not write semantics; no broadcast.
                // A backfill failure is just a degradation; the next miss will go remote again.
                try {
                    local.put(key, existed.get())
                } catch (t: Throwable) {
                    log.warn("Backfilling local cache failed key={0} cause={1}", key, t.message)
                }
                existed
            }
        }
    }

    override fun clear() = writeThrough("clear", null) { it.clear() }

    /**
     * Clears local cache.
     *
     * @param key key
     */
    fun clearLocal(key: Any?) {
        if (key == null) {
            localCache?.clear()
        } else {
            localCache?.evict(key)
        }
    }

    /**
     * Broadcasts a cache-invalidation message to all nodes (fire-and-forget, async).
     *
     * Fetches all [ICacheMessageHandler] beans from the Spring container (typically Redis pub/sub implementations),
     * so that other nodes evict the corresponding local cache upon receiving the notification. A null `key` means "clear all".
     *
     * Reason for asynchronous dispatch: synchronous broadcasts would push the RTT of Redis pub/sub (and similar) onto the write path
     * (one put -> one cross-node broadcast -> at least one network round trip). Going async lets the write path only enqueue the
     * message into [broadcastExecutor]; actual sending happens on a daemon thread.
     *
     * Failure semantics: each handler's send failure is caught individually and WARN-logged (**never retried** — duplicate broadcasts
     * are more dangerous than occasionally dropping one; downstream `MixCache.clearLocal` simply drops a local copy, which will be
     * regenerated by the next source fetch). When the queue is full, falls back to caller-runs, equivalent to synchronous semantics:
     * it is better to slow writes than to drop messages.
     *
     * @param name cache name (with version prefix)
     * @param key key to evict; null means "clear all"
     * @author K
     * @since 1.0.0
     */
    fun pushMsgRedis(name: String, key: Any?) {
        val handlers = SpringKit.getBeansOfType<ICacheMessageHandler>()
        if (handlers.isEmpty()) return
        val msg = CacheMessage(name, key)
        handlers.forEach { (beanName: String, handler: ICacheMessageHandler) ->
            broadcastExecutor.execute {
                try {
                    handler.sendMessage(msg)
                } catch (t: Throwable) {
                    log.error(
                        t,
                        "Cache invalidation broadcast failed [handler={0}, cache={1}, key={2}] — other nodes' local copies may retain stale values until the next TTL or explicit evict converges them",
                        beanName, name, key
                    )
                }
            }
        }
    }

    companion object {
        /** Logger. */
        private val log = LogFactory.getLog(this::class)

        /** Reserved field: intended for future cross-version invalidation notifications; not used by the current implementation. */
        private val cacheVersion: String? = null

        /**
         * Fire-and-forget executor for cache-invalidation broadcasts.
         *
         * Design:
         * - core=1 / max=Runtime CPU count / keep-alive 60s — the vast majority of broadcasts are short tasks like Redis publish,
         *   whose concurrency bottleneck is network RTT rather than CPU, so 1 thread is usually enough; scales up to CPU count under peaks.
         * - Queue capacity 1024 — losing invalidation messages equals stale local copies on nodes; prefer to slow writes briefly rather than drop messages,
         *   so when full it falls back to [java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy] (synchronous).
         * - Daemon thread — does not block JVM shutdown.
         */
        private val broadcastExecutor: Executor = ThreadPoolExecutor(
            1,
            maxOf(2, Runtime.getRuntime().availableProcessors()),
            60L,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(1024),
            object : ThreadFactory {
                private val seq = AtomicInteger(0)
                override fun newThread(r: Runnable): Thread {
                    val t = Thread(r, "kudos-cache-broadcast-${seq.incrementAndGet()}")
                    t.isDaemon = true
                    return t
                }
            },
            ThreadPoolExecutor.CallerRunsPolicy()
        )
    }
}