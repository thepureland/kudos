package io.kudos.ability.cache.common.notify

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ability.distributed.notify.common.api.INotifyListener
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.base.logger.LogFactory
import java.io.Serializable

/**
 * Cache notification listener.
 *
 * Listens for cache-operation notifications and drives synchronous invalidation/cleanup for distributed caches.
 *
 * Core capabilities:
 * 1. Message listening: subscribes to notifications of type CACHE_OPERATOR.
 * 2. Strategy gating: decides whether to handle the message based on the cache strategy.
 *    - SINGLE_LOCAL: single-node local cache; messages must be processed here.
 *    - Other strategies: handled by other mechanisms, not here.
 * 3. Cache operation: executes different cache operations based on the operation type.
 *    - TYPE_CLEAR: clears the entire cache.
 *    - TYPE_EVICT: evicts the entry for the given key.
 *
 * Workflow:
 * - Receives a NotifyMessageVo and extracts the CacheOperatorVo payload.
 * - Checks whether the cache is active; returns immediately if not.
 * - Fetches the cache configuration to determine the strategy.
 * - For SINGLE_LOCAL, performs the cache cleanup.
 *
 * Cache strategies:
 * - SINGLE_LOCAL: single-node local cache; requires the notification mechanism for multi-node synchronization.
 * - Other strategies (e.g. distributed caches): handled by Redis pub/sub or other mechanisms.
 *
 * Caveats:
 * - Only SINGLE_LOCAL caches are handled by this listener.
 * - Notifications sent via MQ only need to be processed by a single application instance (avoid duplicate cleanup).
 * - If the cache is not active, nothing is performed.
 */
class CacheNotifyListener : INotifyListener {

    private val log = LogFactory.getLog(this::class)

    override fun notifyType(): String {
        return CACHE_OPERATOR
    }

    /**
     * Handles a cache-operation notification.
     *
     * Receives a cache-operation notification, decides whether to handle it based on the cache strategy,
     * and executes the corresponding cleanup.
     *
     * Workflow:
     * 1. Extract payload: pull CacheOperatorVo out of the NotifyMessageVo.
     * 2. Activation check: return immediately if the cache is not active.
     * 3. Logging: log the received cleanup notification.
     * 4. Strategy check: fetch the cache config to determine the strategy.
     * 5. Cleanup: for SINGLE_LOCAL, call doClear.
     *
     * Cache strategies:
     * - SINGLE_LOCAL: single-node local cache; requires the notification mechanism to synchronize across nodes.
     *   - When one node clears its cache, it broadcasts via MQ.
     *   - Other nodes receive the message and clear their local caches.
     *   - Only one application instance needs to process it (to avoid duplicate cleanup).
     * - Other strategies (e.g. distributed caches): handled by Redis pub/sub or other mechanisms, not here.
     *
     * Caveats:
     * - Only SINGLE_LOCAL caches are handled by this listener.
     * - If the cache is not active, nothing is performed.
     * - Notifications sent via MQ only need to be processed by a single application instance.
     *
     * @param notifyMessageVo the notification message, containing the cache-operation payload
     */
    override fun notifyProcess(notifyMessageVo: NotifyMessageVo<out Serializable>) {
        val messageBody = notifyMessageVo.messageBody as CacheOperatorVo
        val cacheName = messageBody.cacheName
        val kvActive = KeyValueCacheKit.isCacheActive(cacheName)
        val hashActive = HashCacheKit.isCacheActive(cacheName)
        if (!kvActive && !hashActive) {
            return
        }
        log.info(
            "Received cache-clean notify: cacheName={0},type={1},key={2},msgId={3},fromNode={4},ts={5}",
            cacheName,
            messageBody.type,
            messageBody.key,
            messageBody.messageId,
            messageBody.nodeId,
            messageBody.timestamp
        )
        val resolvedStrategy = KeyValueCacheKit.getCacheConfig(cacheName)?.resolvedStrategy
            ?: HashCacheKit.getCacheConfig(cacheName)?.resolvedStrategy
        if (resolvedStrategy == CacheStrategy.SINGLE_LOCAL) {
            doClear(messageBody)
        }
    }

    /**
     * Executes the cache cleanup.
     *
     * Performs different cleanup actions based on the operation type.
     *
     * Operation types:
     * - TYPE_CLEAR: clears the entire cache; calls CacheKit.doClear to wipe all entries.
     * - TYPE_EVICT: evicts a specific key; calls CacheKit.doEvict.
     *
     * Caveats:
     * - Both operation types can coexist and will both be executed.
     * - TYPE_EVICT requires a non-null key; otherwise an exception is thrown.
     * - The cleanup triggers an invalidation notification, asking other nodes to synchronize.
     *
     * @param messageBody the cache-operation payload, containing operation type, cache name, key, etc.
     */
    private fun doClear(messageBody: CacheOperatorVo) {
        val cacheName = messageBody.cacheName
        if (CacheOperatorVo.TYPE_CLEAR == messageBody.type) {
            if (KeyValueCacheKit.isCacheActive(cacheName)) {
                KeyValueCacheKit.doClear(cacheName)
            }
            if (HashCacheKit.isCacheActive(cacheName)) {
                HashCacheKit.doClear(cacheName)
            }
        }
        if (CacheOperatorVo.TYPE_EVICT == messageBody.type && messageBody.key != null) {
            if (KeyValueCacheKit.isCacheActive(cacheName)) {
                KeyValueCacheKit.doEvict(cacheName, messageBody.key!!)
            }
            if (HashCacheKit.isCacheActive(cacheName)) {
                HashCacheKit.doEvict(cacheName, messageBody.key!!)
            }
        }
    }

    companion object {
        const val CACHE_OPERATOR: String = "_CACHE_OPERATOR"
    }
}
