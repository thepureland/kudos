package io.kudos.ability.cache.local.caffeine

import com.github.benmanes.caffeine.cache.Caffeine
import io.kudos.ability.cache.common.support.AbstractCacheManager
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.base.logger.LogFactory
import org.springframework.cache.caffeine.CaffeineCache
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * 本地缓存管理器的Caffeine实现
 */
class CaffeineCacheManager : AbstractCacheManager<CaffeineCache>() {

    override fun createCache(cacheConfig: CacheConfig): CaffeineCache {
        val cacheBuilder = Caffeine.from(properties.caffeine.spec)
        cacheBuilder.expireAfterWrite(cacheConfig.ttl!!.toLong(), TimeUnit.SECONDS)
        var name: String? = cacheConfig.name
        val ignoreVersion: Boolean? = cacheConfig.ignoreVersion
        if (ignoreVersion == null || !ignoreVersion) {
            name = versionConfig.getFinalCacheName(name!!)
        }
        val caffeineCache = CaffeineCache(name, cacheBuilder.build())
        log.debug("初始化本地缓存【{0}】成功！", name)
        return caffeineCache
    }

    override fun evictByPattern(cacheName: String, pattern: String) {
        val cache = getCache(cacheName)
        if (cache == null) {
            return
        }
        val realPattern: String = versionConfig.getFinalCacheName(pattern)
        // 拿到底层 ConcurrentMap
        val nativeCache = (cache as CaffeineCache).nativeCache.asMap()
        val regex = "^" + realPattern.replace("*", ".*") + "$"
        val p = Pattern.compile(regex)
        // 遍历 keySet，匹配后再 evict
        nativeCache.keys.stream()
            .filter { key: Any? -> p.matcher(key.toString()).matches() }
            .forEach { key: Any? -> cache.evict(key) }
    }

    private val log = LogFactory.getLog(this)

}
