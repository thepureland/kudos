package io.kudos.ability.cache.common.support

/**
 * Cache configuration provider interface.
 *
 * @author K
 * @since 1.0.0
 */
interface ICacheConfigProvider {
    /**
     * Returns the cache configuration for the given name.
     *
     * @param name cache name
     * @return cache configuration; null if not found
     * @author K
     * @since 1.0.0
     */
    fun getCacheConfig(name: String): CacheConfig?

    /**
     * Returns all cache configurations.
     *
     * @return Map(String, cache configuration)
     * @author K
     * @since 1.0.0
     */
    fun getAllCacheConfigs(): Map<String, CacheConfig>

    /**
     * Returns the local (first-tier) cache configurations.
     *
     * @return Map(String, local cache configuration)
     * @author K
     * @since 1.0.0
     */
    fun getLocalCacheConfigs(): Map<String, CacheConfig>

    /**
     * Returns the remote (second-tier) cache configurations.
     *
     * @return Map(String, remote cache configuration)
     * @author K
     * @since 1.0.0
     */
    fun getRemoteCacheConfigs(): Map<String, CacheConfig>

    /**
     * Returns the local-remote (first- and second-tier) cache configurations.
     *
     * @return Map(String, local-remote (first- and second-tier) cache configuration)
     * @author K
     * @since 1.0.0
     */
    fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig>

    /**
     * Returns the Hash cache configurations (those with hash==true), used to initialize MixHashCacheManager.
     * Keys are cacheName; values contain strategy etc. Shares the same configuration source as key-value caches.
     *
     * @return Map(cacheName, CacheConfig)
     * @since 1.0.0
     */
    fun getHashCacheConfigs(): Map<String, CacheConfig> = getAllCacheConfigs().filter { (_, config) -> config.hash }
}
