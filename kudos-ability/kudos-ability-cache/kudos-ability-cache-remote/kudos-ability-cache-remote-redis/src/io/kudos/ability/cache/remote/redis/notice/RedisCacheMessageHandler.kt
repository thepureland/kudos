package io.kudos.ability.cache.remote.redis.notice

import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.notify.CacheMessage
import io.kudos.ability.cache.common.notify.ICacheMessageHandler
import io.kudos.ability.cache.common.support.CacheCleanRegister
import io.kudos.ability.cache.common.support.IHashCacheSync
import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

/**
 * Redis cache message handler.
 *
 * Listens on Redis pub/sub to drive distributed cache sync and invalidation notifications.
 *
 * Core features:
 * 1. Send: publish cache operation messages to a Redis channel so other nodes are notified.
 * 2. Receive: subscribe to the Redis channel and pick up cache operation messages from other nodes.
 * 3. Node isolation: use a node-id mechanism so the current node ignores messages it published (no duplicate clearing).
 * 4. Local cache clearing: clear the local cache when receiving an invalidation from another node.
 * 5. Listener notification: trigger registered cache-clean listeners for custom logic.
 *
 * Workflow:
 * - Send: serialize the CacheMessage, publish to the Redis channel with the sender node id set.
 * - Receive: read messages from the Redis channel and deserialize back into CacheMessage.
 * - Node check: compare the message's node id with the current node id; only foreign messages are processed.
 * - Local clear: invoke MixCacheManager to clear the local cache.
 * - Listener fire: notify registered cache-clean listeners to execute clearing logic.
 *
 * Node-id mechanism:
 * - Each app instance generates a unique node id (UUID) at startup.
 * - The node id is attached when sending, and compared when receiving.
 * - Only foreign messages trigger local cache clearing, preventing duplicate work.
 *
 * Notes:
 * - Deserialization failures are logged at warning level but do not interrupt the flow.
 * - Cache version isolation is honored; cache names in messages are version-converted.
 * - Redis connectivity must be healthy; otherwise messages cannot be sent or received properly.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class RedisCacheMessageHandler(
    private val nodeId: String
) : ICacheMessageHandler, MessageListener {
    @Value($$"${kudos.ability.cache.remoteStore}")
    private lateinit var remoteStore: String

    @Autowired
    private lateinit var redisTemplates: RedisTemplates

    @Autowired(required = false)
    @Qualifier("mixCacheManager")
    private lateinit var mixCacheManager: MixCacheManager

    @Autowired
    protected lateinit var versionConfig: CacheVersionConfig

    /**
     * Local sync hook for the Hash cache; optional — absent when no hash cache module is present.
     * Every Redis notification needs to find it; the old implementation called
     * `SpringKit.getBeansOfType<IHashCacheSync>().values.firstOrNull()` every time, triggering a full
     * bean scan on a hot path. Switched to a single Spring injection (`required = false`).
     */
    @Autowired(required = false)
    private var hashCacheSync: IHashCacheSync? = null

    /**
     * Sends a cache operation message.
     *
     * Publishes the cache operation message to the Redis pub/sub channel so other nodes can clear their caches.
     *
     * Workflow:
     * 1. Stamp the message with the current node id.
     * 2. Serialize and publish to the Redis channel (using the versioned channel name).
     *
     * Node-id usage:
     * - Receivers compare node ids to decide whether to handle the message.
     * - Prevents the current node from processing its own publications (no duplicate clearing).
     *
     * @param message the cache operation message
     */
     override fun sendMessage(message: CacheMessage) {
        // Stamp the sending node id
        message.nodeId = nodeId
        redisTemplates.getRedisTemplate(remoteStore)!!.convertAndSend(versionConfig.realMsgChannel, message)
    }

    /**
     * Receives a Redis pub/sub message.
     *
     * Reads a cache operation message from the Redis channel, deserializes it, and forwards to `receiveMessage`.
     *
     * Workflow:
     * 1. Read the byte array from the message body.
     * 2. Deserialize into a CacheMessage using RedisTemplate's ValueSerializer.
     * 3. If deserialization succeeds, invoke `receiveMessage`.
     * 4. If deserialization fails, log a warning but do not interrupt processing.
     *
     * Error handling:
     * - Deserialization failures usually occur because the cache key uses a method-parameter type that
     *   cannot be resolved during serialization.
     * - Recommend specifying the key explicitly in cache annotations, e.g. key='ALL', to avoid type-resolution issues.
     * - Deserialization failures are not propagated; only logged.
     *
     * @param message the Redis message (body + channel info)
     * @param pattern optional channel pattern
     */
    override fun onMessage(message: Message, pattern: ByteArray?) {
        logger.debug("Received Redis message notification: clearing local cache")
        val deserialized: CacheMessage? = try {
            redisTemplates.getRedisTemplate(remoteStore)!!.valueSerializer
                .deserialize(message.body) as CacheMessage?
        } catch (e: Exception) {
            // Deserialization failure is a distributed-cache consistency incident: this node will not receive
            // the invalidation -> the local cache may hold stale data indefinitely. Raise to ERROR with the
            // stack trace so operators can detect it; do not rethrow so the listener thread is not killed.
            // Common causes: CacheMessage class signatures (serialVersionUID) diverge between sender/receiver,
            // or the key class is missing from the classpath.
            logger.error(e, "Redis cache invalidation deserialization failed; this node's local cache may be inconsistent. Check that CacheMessage and key classes are compatible across sender/receiver.")
            null
        }
        val cacheMessage = deserialized ?: return
        try {
            receiveMessage(cacheMessage)
        } catch (e: Exception) {
            // Spring dispatches listener calls serially on a single thread; an unhandled exception would be
            // swallowed by the container but stop this message from being processed. Explicit catch + log
            // prevents subsequent messages from being misread as "all failed".
            logger.error(e, "Error processing Redis cache invalidation: cacheName={0}, key={1}", cacheMessage.cacheName, cacheMessage.key)
        }
    }

    /**
     * Processes a received cache operation message.
     *
     * Performs local cache clearing and fires registered listeners based on the message.
     *
     * Workflow:
     * 1. Node check: compare the message node id with the current node id.
     *    - Different: a foreign message; clear the local cache.
     *    - Same: a self-published message; already cleared locally; skip.
     * 2. Local cache clearing: invoke MixCacheManager to clear the local cache.
     * 3. Cache-name conversion: convert the cache name in the message to the actual name (strip the version prefix).
     * 4. Listener firing: fetch registered clean listeners and fire them one by one.
     *
     * Node isolation:
     * - Only foreign messages trigger local cache clearing.
     * - Self-published messages do not trigger local clearing, avoiding duplicate work.
     * - Ensures cache-clearing correctness in distributed environments.
     *
     * Clean listeners:
     * - Custom cache-clean listeners can be registered.
     * - Listeners can perform extra cleanup, e.g. clearing related caches, notifying other systems, etc.
     *
     * @param message cache operation message (cache name, key, node id, etc.)
     */
    override fun receiveMessage(message: CacheMessage) {
        val cacheName = message.cacheName!!
        // Only foreign clears need to delete the local cache; this node has already cleared its own.
        if (message.nodeId != nodeId) {
            if (message.cacheType == "hash") {
                hashCacheSync?.let { sync ->
                    when (val k = message.key) {
                        null -> sync.clearLocal(cacheName)
                        // Batch operations bundle id lists into one message (e.g. saveBatch) to avoid N+1 publish storms.
                        is Collection<*> -> k.filterNotNull().forEach { sync.evictLocal(cacheName, it) }
                        else -> sync.evictLocal(cacheName, k)
                    }
                }
            } else {
                mixCacheManager.clearLocal(cacheName, message.key)
            }
        }
        val realCacheKey = versionConfig.getRealCacheName(cacheName)
        CacheCleanRegister.getCleanListener(realCacheKey)
            ?.forEach { it.cleanCache(realCacheKey, message.key) }
    }

    val redisTemplate: RedisTemplate<*, *>
        get() = redisTemplates.getRedisTemplate(remoteStore) as RedisTemplate<*, *>

    companion object {
        private val logger = LogFactory.getLog(this::class)
    }
}
