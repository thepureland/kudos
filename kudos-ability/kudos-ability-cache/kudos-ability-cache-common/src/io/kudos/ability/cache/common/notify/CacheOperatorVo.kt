package io.kudos.ability.cache.common.notify

import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.notify.common.support.NotifyTool
import io.kudos.base.logger.LogFactory
import io.kudos.context.kit.SpringKit
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import java.io.Serial
import java.io.Serializable
import java.util.UUID

/**
 * Cache operation value object.
 *
 * Wraps a cache-operation payload (clear or evict) and supports distributed cache synchronization via the
 * notification mechanism.
 *
 * Core properties:
 * - type: operation type (TYPE_CLEAR clears the whole cache, TYPE_EVICT evicts a specific key).
 * - cacheName: cache name identifying the target.
 * - key: cache key; used for TYPE_EVICT, may be null for TYPE_CLEAR.
 *
 * Operation types:
 * - TYPE_CLEAR: clears the entire cache; all keys are removed.
 * - TYPE_EVICT: evicts the entry for the given key.
 *
 * Notification mechanism:
 * - doNotify sends a notification message via MQ.
 * - Other nodes process the notification via CacheNotifyListener.
 * - Achieves synchronous invalidation in a distributed environment.
 *
 * Fallback handling:
 * - If sending fails, performs local cleanup as a fallback.
 * - Ensures local caches are still cleaned even when the notification mechanism is unavailable.
 * - Prevents cache inconsistencies.
 *
 * Use cases:
 * - Distributed cache consistency.
 * - Proactive cache invalidation.
 * - Multi-node cache synchronization.
 *
 * Caveats:
 * - When the notification succeeds, no local cleanup is performed (the other nodes handle it).
 * - When the notification fails, local cleanup runs as a fallback.
 * - For TYPE_EVICT, key must not be null.
 *
 * @since 1.0.0
 */
class CacheOperatorVo(
    var type: String, // operation type
    var cacheName: String, // cache name
    var key: Any? // cache key
) : Serializable {

    /**
     * Unique message id, used for cross-node log tracing and (when necessary) deduplication.
     * Generated when the object is constructed; null when deserialized from an old node (Java serialization
     * compatibility for new fields). Consumers must handle null.
     */
    var messageId: String? = UUID.randomUUID().toString()

    /**
     * Sender node id; populated in [doNotify]. Helps consumers identify loopback / troubleshoot cross-node issues.
     * The current implementation does not filter loopback based on nodeId (the SINGLE_LOCAL flow relies on
     * the loop-back to clear the sender's local cache); it is used only for logging.
     */
    var nodeId: String? = null

    /** Send timestamp in milliseconds. Useful for consumers to investigate "delayed message" or out-of-order issues in logs. */
    var timestamp: Long = System.currentTimeMillis()

    /**
     * Sends the cache-operation notification.
     *
     * Sends the cache-operation message via the notification mechanism, achieving synchronous invalidation
     * across the distributed cache.
     *
     * Workflow:
     * 1. Build the message: wrap this CacheOperatorVo in a NotifyMessageVo.
     * 2. Attempt to send: forward via NotifyTool to MQ.
     * 3. Failure handling: if sending fails (NotifyTool missing or send failure), perform local cleanup.
     *
     * Notification mechanism:
     * - Prefers the MQ-based notification mechanism for distributed sync.
     * - The message is processed by CacheNotifyListener.
     * - Supports synchronous invalidation across multiple nodes.
     *
     * Fallback handling:
     * - If the NotifyTool bean is missing, logs a warning and performs local cleanup.
     * - If sending fails, local cleanup is also performed.
     * - Ensures local caches are cleaned even when the notification mechanism is unavailable.
     *
     * Cleanup actions:
     * - TYPE_CLEAR: clears the entire cache.
     * - TYPE_EVICT: evicts the given key.
     *
     * Caveats:
     * - On success, no local cleanup is performed (the other nodes handle it).
     * - On failure, local cleanup runs as a fallback.
     * - Ensure the NotifyTool bean is present; otherwise the flow degrades to local cleanup only.
     */
    fun doNotify() {
        // Populate nodeId at send time so consumers can troubleshoot; falls back to null when the cacheNodeId
        // bean is missing, without affecting the main flow.
        if (nodeId == null) {
            nodeId = runCatching { SpringKit.getBeanOrNull("cacheNodeId") as? String }.getOrNull()
        }
        val messageVo: NotifyMessageVo<*> = NotifyMessageVo<CacheOperatorVo>(CacheNotifyListener.CACHE_OPERATOR, this)
        var notify = false
        try {
            val notifyTool = SpringKit.getBean<NotifyTool>()
            notify = notifyTool.notify(messageVo)
        } catch (e: NoSuchBeanDefinitionException) {
            LOG.warn(e.message)
        }
        if (!notify) {
            if (TYPE_CLEAR == this.type) {
                KeyValueCacheKit.doClear(this.cacheName)
            }
            if (TYPE_EVICT == this.type) {
                KeyValueCacheKit.doEvict(this.cacheName, this.key!!)
            }
        }
    }

    companion object {
        private val LOG = LogFactory.getLog(this::class)

        @Serial
        private val serialVersionUID = -1233873328202104930L

        const val TYPE_CLEAR: String = "clear"
        const val TYPE_EVICT: String = "evict"
    }
}
