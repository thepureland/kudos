package io.kudos.ability.cache.local.caffeine

import com.github.benmanes.caffeine.cache.Caffeine
import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheManager
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.base.logger.LogFactory
import org.springframework.cache.caffeine.CaffeineCache
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * 本地缓存管理器的Caffeine实现
 */
class CaffeineKeyValueCacheManager : AbstractKeyValueCacheManager<CaffeineCache>() {

    override fun createCache(cacheConfig: CacheConfig): CaffeineCache {
        val spec = requireNotNull(properties.caffeine.spec) { "caffeine spec is required" }
        val cacheBuilder = Caffeine.from(spec)
        val ttlSec = requireNotNull(cacheConfig.ttl) { "cache ttl is required" }.toLong()
        cacheBuilder.expireAfterWrite(ttlSec, TimeUnit.SECONDS)
        var name = requireNotNull(cacheConfig.name) { "cache name is required" }
        val ignoreVersion: Boolean? = cacheConfig.ignoreVersion
        if (ignoreVersion == null || !ignoreVersion) {
            name = versionConfig.getFinalCacheName(name)
        }
        val caffeineCache = CaffeineCache(name, cacheBuilder.build())
        log.debug("初始化本地缓存【{0}】成功！", name)
        return caffeineCache
    }

    override fun evictByPattern(cacheName: String, pattern: String) {
        val cache = getCache(cacheName) ?: return
        // 入参可能是逻辑模式（如 user:*）或已带版本前缀的实际模式，先去版本再统一加版本，避免重复前缀。
        val realPattern: String = versionConfig.getFinalCacheName(versionConfig.getRealCacheName(pattern))
        // 拿到底层 ConcurrentMap
        val nativeCache = (cache as CaffeineCache).nativeCache.asMap()
        val regex = "^" + realPattern.replace("*", ".*") + "$"
        val p = Pattern.compile(regex)
        for (key in nativeCache.keys) {
            if (p.matcher(key.toString()).matches()) {
                cache.evict(key)
            }
        }
    }

    override fun existsKey(cacheName: String, key: Any): Boolean {
        val cache = getCache(cacheName) ?: return false
        return (cache as CaffeineCache).nativeCache.asMap().containsKey(key)
    }

    private val log = LogFactory.getLog(this::class)

}
