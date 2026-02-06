package io.kudos.ability.cache.common.core.keyvalue

import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.support.CacheConfig
import jakarta.annotation.Resource
import org.springframework.boot.cache.autoconfigure.CacheProperties
import org.springframework.cache.Cache
import org.springframework.cache.support.AbstractCacheManager

/**
 * 抽象缓存管理器
 *
 * @author K
 * @since 1.0.0
 */
abstract class AbstractKeyValueCacheManager<T : Cache> : AbstractCacheManager(), IKeyValueCacheManager<T> {

    var caches = mutableListOf<T>()

    @Resource
    protected lateinit var properties: CacheProperties

    @Resource
    protected lateinit var versionConfig: CacheVersionConfig

    override fun initCacheAfterSystemInit(cacheConfigMap: Map<String, CacheConfig>) {
        cacheConfigMap.forEach { (_: String, cacheConfig: CacheConfig) ->
            val cache = createCache(cacheConfig)
            add(cache)
        }
        afterPropertiesSet()
    }

    @Synchronized
    fun add(cache: T) {
        this.caches.add(cache)
    }

    override fun loadCaches(): MutableCollection<out Cache> {
        return this.caches
    }
}