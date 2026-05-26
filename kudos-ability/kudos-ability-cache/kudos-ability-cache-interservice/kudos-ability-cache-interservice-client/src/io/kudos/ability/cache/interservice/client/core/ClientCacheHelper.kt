package io.kudos.ability.cache.interservice.client.core

import io.kudos.ability.cache.common.core.keyvalue.IKeyValueCacheManager
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.interservice.client.init.InterServiceCacheClientProperties
import io.kudos.ability.cache.interservice.common.ClientCacheItem
import io.kudos.ability.cache.interservice.common.ClientCacheKey
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.cache.Cache

/**
 * Feign inter-service cache helper (client side).
 *
 * Caches Feign responses locally (default TTL 600s) when invoking remote services, reducing cross-service
 * call frequency. Silently disables itself when no local cache manager is available — avoiding startup
 * failures in downstream applications that have not pulled in caffeine/redis.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class ClientCacheHelper(
    private val properties: InterServiceCacheClientProperties = InterServiceCacheClientProperties(),
    private val cacheManager: IKeyValueCacheManager<*>? = null,
) : InitializingBean {

    /**
     * Tells whether a local cache implementation is available.
     * @return true if a local cache manager is present and caching can operate
     * @author K
     * @since 1.0.0
     */
    open fun hasLocalCache(): Boolean {
        return cacheManager != null
    }

    /**
     * Legacy misspelling, deprecated; use [hasLocalCache].
     * @author K
     * @since 1.0.0
     */
    @Deprecated("Use hasLocalCache()")
    fun havaLocalCache(): Boolean = hasLocalCache()

    /**
     * Initializes the Feign cache region after the Spring container is assembled.
     *
     * - Sets `ignoreVersion = true`: inter-service caches do not participate in version prefixing,
     *   avoiding key misses caused by upstream/downstream version drift.
     * - Default TTL of 600s balances freshness against hit ratio; configurable via
     *   `kudos.ability.cache.interservice.client.ttl-seconds`.
     *
     * @throws Exception forwarded to Spring on cache initialization failure
     * @author K
     * @since 1.0.0
     */
    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        if (!hasLocalCache()) {
            log.info("No local cache implementation found; feature disabled...")
            return
        }
        log.info("Initializing Feign cache region...")
        val cacheName = ClientCacheKey.FEIGN_CACHE_PREFIX
        require(properties.ttlSeconds > 0) {
            "kudos.ability.cache.interservice.client.ttl-seconds must be greater than 0"
        }
        val cacheConfig = CacheConfig().apply {
            name = cacheName
            ignoreVersion = true
            ttl = properties.ttlSeconds
        }
        requireNotNull(cacheManager) { "localCacheManager not available" }
            .initCacheAfterSystemInit(mapOf(cacheName to cacheConfig))
        log.debug("Feign cache region {0} initialized", ClientCacheKey.FEIGN_CACHE_PREFIX)
    }

    /**
     * Loads data from the local cache.
     *
     * @param cacheKey cacheKey
     * @return Object
     */
    open fun loadFromLocalCache(cacheKey: String): ClientCacheItem? {
        // Consider switching to CacheKit.
        val value = feignCache().get(cacheKey)?.get()
        return when (value) {
            null -> null
            is ClientCacheItem -> value
            else -> {
                log.warn(
                    "Feign cache item type mismatch, evict local entry. key={0}, actualType={1}",
                    cacheKey,
                    value::class.java.name
                )
                feignCache().evict(cacheKey)
                null
            }
        }
    }

    /**
     * Writes data into the local cache.
     *
     * @param cacheKey local cache key
     * @param data     cache data
     */
    open fun writeToLocalCache(cacheKey: String, data: ClientCacheItem?) {
        // Consider switching to CacheKit.
        feignCache().put(cacheKey, data)
    }

    /**
     * Returns the Feign-dedicated cache region.
     * Fails fast if the cache manager or region is uninitialized, avoiding hard-to-trace NPEs downstream.
     *
     * @return the Feign cache region
     * @throws IllegalStateException when the cache is uninitialized
     * @author K
     * @since 1.0.0
     */
    private fun feignCache(): Cache {
        val mgr = cacheManager ?: error("localCacheManager not available")
        return mgr.getCache(ClientCacheKey.FEIGN_CACHE_PREFIX)
            ?: error("Feign cache region ${ClientCacheKey.FEIGN_CACHE_PREFIX} not initialized")
    }

    /** Logger. */
    private val log = LogFactory.getLog(this::class)

}
