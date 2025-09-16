package io.kudos.ability.cache.common.support

import org.springframework.cache.Cache
import org.springframework.cache.CacheManager

/**
 * 缓存管理器接口
 *
 * @author K
 * @since 1.0.0
 */
interface ICacheManager<T : Cache?> : CacheManager, CacheItemInitializing {
    /**
     * 根据缓存配置创建缓存实例
     *
     * @param cacheConfig 缓存配置
     * @return 缓存实例
     * @author K
     * @since 1.0.0
     */
    fun createCache(cacheConfig: CacheConfig): T?

    /**
     * 按模式删除某个 cacheName 下的所有 key（用 SCAN 替代 KEYS）
     *
     * @param cacheName Spring Cache 名称
     * @param pattern   业务 key 模式，比如 "user:*"
     */
    fun evictByPattern(cacheName: String, pattern: String)
}
