package io.kudos.ability.cache.common.notify

/**
 * Cache message handler SPI.
 *
 * After a remote write under the `LOCAL_REMOTE` strategy, `MixCache` broadcasts a [CacheMessage] through
 * every implementation of this interface, asking other nodes to evict their local copies. The transport
 * is implemented by each cache-interservice sub-module (Redis pub/sub, Nacos config, MQ, etc.).
 *
 * Applications typically register a single handler; when multiple coexist, the framework invokes each (fan-out).
 * This "duplicate broadcasting" should be avoided unless tests intentionally use two transports to verify consistency.
 *
 * @author K
 * @since 1.0.0
 */
interface ICacheMessageHandler {
    /**
     * Sends a locally produced cache-invalidation message to other nodes.
     */
    fun sendMessage(message: CacheMessage)

    /**
     * Receives a message from another node — a typical implementation delegates the message to a local
     * cleanup path such as `MixCache.clearLocal(...)`.
     */
    fun receiveMessage(message: CacheMessage)
}
