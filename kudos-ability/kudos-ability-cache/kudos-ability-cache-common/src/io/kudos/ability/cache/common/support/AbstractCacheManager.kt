package io.kudos.ability.cache.common.support

import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.cache.CacheProperties
import org.springframework.cache.Cache
import org.springframework.cache.support.AbstractCacheManager

/**
 * 抽象缓存管理器
 *
 * @author K
 * @since 1.0.0
 */
abstract class AbstractCacheManager<T : Cache> : AbstractCacheManager(), ICacheManager<T> {

    var caches = mutableListOf<T>()

    @Autowired
    protected lateinit var properties: CacheProperties

    @Autowired
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
