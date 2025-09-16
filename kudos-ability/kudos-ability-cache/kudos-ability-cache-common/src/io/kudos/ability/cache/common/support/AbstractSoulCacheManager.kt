package io.kudos.ability.cache.common.support

import org.soul.ability.cache.common.starter.properties.CacheVersionConfig
import org.soul.ability.cache.common.support.CacheConfig
import org.soul.ability.cache.common.support.ICacheManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.cache.CacheProperties
import org.springframework.cache.Cache
import org.springframework.cache.support.AbstractCacheManager
import java.util.*

/**
 * 抽象的soul缓存管理器
 *
 * @author K
 * @since 5.0.0.0
 */
abstract class AbstractSoulCacheManager<T : Cache?> : AbstractCacheManager(), ICacheManager<T?> {
    var caches: MutableList<T?> = LinkedList<T?>()

    @Autowired
    protected var properties: CacheProperties? = null

    @Autowired
    protected var versionConfig: CacheVersionConfig? = null

    override fun initCacheAfterSystemInit(cacheConfigMap: MutableMap<String?, CacheConfig?>) {
        cacheConfigMap.forEach { (key: String?, cacheConfig: CacheConfig?) ->
            val cache = createCache(cacheConfig)
            add(cache)
        }
        afterPropertiesSet()
    }

    @Synchronized
    fun add(cache: T?) {
        this.caches.add(cache)
    }

    override fun loadCaches(): MutableCollection<out Cache?> {
        return this.caches
    }
}
