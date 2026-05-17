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
        val effectiveSpec = ensureSizeBound(spec)
        val cacheBuilder = Caffeine.from(effectiveSpec)
        val ttlSec = requireNotNull(cacheConfig.ttl) { "cache ttl is required" }.toLong()
        cacheBuilder.expireAfterWrite(ttlSec, TimeUnit.SECONDS)
        var name = requireNotNull(cacheConfig.name) { "cache name is required" }
        val ignoreVersion: Boolean? = cacheConfig.ignoreVersion
        if (ignoreVersion == null || !ignoreVersion) {
            name = versionConfig.getFinalCacheName(name)
        }
        val caffeineCache = DrainingCaffeineCache(name, cacheBuilder.build())
        log.debug("初始化本地缓存【{0}】成功！", name)
        return caffeineCache
    }

    /**
     * 给 caffeine spec 兜底加上大小限制。
     * 不要依赖业务方在每个环境的配置文件里都正确写出 maximumSize：
     * 漏写一次 spec 就是无界 LinkedHashMap，长跑后必 OOM。这里在创建阶段静默兜底，并 warn 提示。
     */
    private fun ensureSizeBound(spec: String): String {
        val hasSizeBound = SIZE_BOUND_REGEX.containsMatchIn(spec)
        if (hasSizeBound) return spec
        log.warn(
            "Caffeine spec 未配置 maximumSize/maximumWeight，按 [{0}] 兜底以避免无界增长；建议显式在 caffeine.spec 里设置上限。原 spec=[{1}]",
            DEFAULT_MAX_SIZE,
            spec
        )
        return if (spec.isBlank()) "maximumSize=$DEFAULT_MAX_SIZE" else "$spec,maximumSize=$DEFAULT_MAX_SIZE"
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

    companion object {
        /** 兜底大小：不严格科学，但比无界好；用户应在 spec 里显式覆盖以匹配自身工作负载。 */
        private const val DEFAULT_MAX_SIZE = 10_000L

        /** 匹配 caffeine spec 中的大小相关配置项（key 后必须紧跟 `=`，避免误匹配 maximumWeightXxx 之类）。 */
        private val SIZE_BOUND_REGEX = Regex("\\b(maximumSize|maximumWeight)\\s*=")
    }

}
