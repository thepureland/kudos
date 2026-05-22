package io.kudos.ability.cache.local.caffeine

import com.github.benmanes.caffeine.cache.Caffeine
import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheManager
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.base.logger.LogFactory
import org.springframework.cache.caffeine.CaffeineCache
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * K-V 本地缓存管理器的 Caffeine 实现。
 *
 * 把 [CacheConfig] 翻译成 Caffeine spec + 单缓存级别的 `expireAfterWrite(ttl)`，
 * 包装成 [DrainingCaffeineCache] 以解决 `invalidateAll()` 异步生效引起的"清完还能读到"问题。
 *
 * 通过 [ensureSizeBound] 给 caffeine spec 兜底 `maximumSize`，避免业务漏配上限导致
 * 长跑 OOM——这是产线遇到过的真实问题，看到 warn 日志请补全 spec。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class CaffeineKeyValueCacheManager : AbstractKeyValueCacheManager<CaffeineCache>() {

    /**
     * 按 [cacheConfig] 构造单个 [CaffeineCache]：合并全局 spec、覆盖 ttl、按版本配置加前缀。
     * 失败的前置条件用 `requireNotNull` 立刻抛——典型是 yml 缺 `spring.cache.caffeine.spec`
     * 或 cache item 缺 `ttl`，启动期失败优于运行期发现缓存配错。
     */
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

    /**
     * 按通配符 `*` 模式清除缓存项。
     *
     * 入参可能是逻辑模式（`user:*`）或已带版本前缀的实际模式（`v2:user:*`）——
     * 统一先剥版本再加回版本，避免重复前缀。把 `*` 转为正则 `.*` 后扫整张 nativeMap，
     * 命中的 key 走 `cache.evict(key)` 立即生效（[DrainingCaffeineCache] 会同步 cleanUp）。
     *
     * **性能注意**：全表扫描 + 单 key evict；缓存条目数大时不便宜。生产使用前评估热点。
     */
    override fun evictByPattern(cacheName: String, pattern: String) {
        val cache = getCache(cacheName) ?: return
        val realPattern: String = versionConfig.getFinalCacheName(versionConfig.getRealCacheName(pattern))
        val nativeCache = (cache as CaffeineCache).nativeCache.asMap()
        val regex = "^" + realPattern.replace("*", ".*") + "$"
        val p = Pattern.compile(regex)
        for (key in nativeCache.keys) {
            if (p.matcher(key.toString()).matches()) {
                cache.evict(key)
            }
        }
    }

    /**
     * 是否存在指定 key——通过 `nativeCache.asMap().containsKey` 直接判断，
     * 不触发 Caffeine 的 LoadingCache 加载逻辑（与 [CaffeineCache.get] 的语义区分开）。
     */
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
