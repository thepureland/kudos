package io.kudos.ability.cache.remote.redis

import io.kudos.ability.cache.common.core.keyvalue.IKeyValueCacheManager
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.base.logger.LogFactory
import jakarta.annotation.Resource
import org.springframework.data.redis.cache.RedisCache
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.cache.RedisCacheWriter
import java.time.Duration

/**
 * Redis缓存管理器
 * 
 * 扩展Spring的RedisCacheManager，支持缓存版本管理和多租户缓存隔离。
 * 
 * 核心功能：
 * 1. 缓存版本管理：通过CacheVersionConfig为缓存名称添加版本前缀，支持缓存版本隔离和升级
 * 2. 动态缓存创建：根据CacheConfig配置动态创建RedisCache实例，支持自定义TTL
 * 3. 模式删除：支持按模式删除缓存key，使用SCAN替代KEYS命令，避免阻塞Redis
 * 4. 缓存初始化：在系统初始化后批量创建配置的缓存实例
 * 
 * 缓存命名规则：
 * - 实际缓存名称 = 版本前缀 + 原始缓存名称
 * - 例如：版本为"v1"，缓存名为"user"，实际名称为"v1::user"
 * 
 * 模式删除：
 * - 支持按模式删除缓存key，例如"user:*"会删除所有以"user:"开头的key
 * - 使用SCAN命令替代KEYS，避免在生产环境阻塞Redis
 * - 会自动添加缓存名称前缀和版本前缀
 * 
 * 注意事项：
 * - 缓存创建时会应用版本前缀，确保不同版本的缓存相互隔离
 * - 支持自定义TTL，如果配置中未指定TTL，使用默认配置
 * - 缓存实例创建后会被添加到caches列表中，供后续使用
 */
class RedisKeyValueCacheManager(
    private val cacheWriter: RedisCacheWriter,
    private val defaultCacheConfiguration: RedisCacheConfiguration
) : RedisCacheManager(
    cacheWriter,
    defaultCacheConfiguration
), IKeyValueCacheManager<RedisCache> {

    var caches: MutableList<RedisCache> = mutableListOf()

    @Resource
    private lateinit var versionConfig: CacheVersionConfig

    override fun initCacheAfterSystemInit(cacheConfigMap: Map<String, CacheConfig>) {
        cacheConfigMap.forEach { (key: String, cacheConfig: CacheConfig) ->
            val cache: RedisCache = createCache(cacheConfig)
            log.debug("初始化远程缓存【{0}】成功！", key)
            addCache(cache)
        }
        afterPropertiesSet()
    }

    /**
     * 把单个 cache 实例追加到内部缓存表。`@Synchronized` 是为了应对未来"运行期动态加 cache"
     * 的可能；当前调用方仅 `initCacheAfterSystemInit`（启动单线程），实际无并发竞争。
     */
    @Synchronized
    fun addCache(cache: RedisCache) {
        this.caches.add(cache)
    }

    /**
     * Spring `AbstractCacheManager` 模板方法，返回所有要被注册的缓存。
     * 注意返回的是内部 [caches] 的直接引用——`afterPropertiesSet()` 调用一次后会做内部拷贝，
     * 此后再 `addCache` 加进来的不会自动被识别。
     */
    override fun loadCaches(): MutableCollection<RedisCache> {
        return caches
    }

    /**
     * 创建Redis缓存实例
     * 
     * 根据CacheConfig配置创建RedisCache实例，支持自定义TTL和版本管理。
     * 
     * 工作流程：
     * 1. 创建默认的RedisCacheConfiguration
     * 2. 禁用null值缓存（disableCachingNullValues）
     * 3. 使用默认配置的序列化器（key和value）
     * 4. 如果配置了TTL，设置缓存过期时间
     * 5. 应用版本前缀到缓存名称
     * 6. 创建并返回RedisCache实例
     * 
     * 配置说明：
     * - TTL：如果cacheConfig中指定了ttl，会设置为缓存过期时间（秒）
     * - 序列化：使用默认配置的序列化器，确保key和value的序列化方式一致
     * - 版本管理：缓存名称会添加版本前缀，例如"v1::user"
     * 
     * 注意事项：
     * - 如果未指定TTL，使用默认的缓存配置（可能不过期）
     * - 缓存名称会自动应用版本前缀，确保不同版本的缓存相互隔离
     * 
     * @param cacheConfig 缓存配置对象，包含缓存名称、TTL等信息
     * @return 创建的RedisCache实例
     */
    override fun createCache(cacheConfig: CacheConfig): RedisCache {
        var redisCacheConfiguration = RedisCacheConfiguration
            .defaultCacheConfig()
            .disableCachingNullValues()
            .serializeKeysWith(defaultCacheConfiguration.keySerializationPair)
            .serializeValuesWith(defaultCacheConfiguration.valueSerializationPair)
        cacheConfig.ttl?.let { ttl ->
            redisCacheConfiguration = redisCacheConfiguration.entryTtl(Duration.ofSeconds(ttl.toLong()))
        }
        val realKey: String = versionConfig.getFinalCacheName(requireNotNull(cacheConfig.name) { "cache name required" })
        // 修复 Spring Boot 4.0.6 中 [RedisCache.clear] 不真正删 Redis key 的 bug：override 自己用
        // `RedisTemplate.keys(pattern) + delete(keys)` 直删，覆盖所有 RedisCache 实例
        return ScanClearRedisCache(realKey, cacheWriter, redisCacheConfiguration)
    }

    /**
     * 按模式删除某个缓存下的所有 key。
     *
     * 工作流程：
     * 1. `prefixProvider.compute(cacheName)` 得到缓存键前缀（默认 `"$cacheName::"`）
     * 2. 经 [CacheVersionConfig.getFinalCacheName] 加上版本前缀
     * 3. 拼接最终匹配模式：实际 key 前缀 + 业务模式（如 `*` / `user:*`）
     * 4. 用 [RedisTemplate.keys] + [RedisTemplate.delete] 直接定位并删除
     *
     * 例如：版本 `v1`、缓存 `user`、模式 `*`，最终匹配 `v1::user::*`。
     *
     * **重要**：旧实现走 `cacheWriter.clear(name, pattern)`，但 Spring Boot 4.0.6 下
     * 这条路径在本项目的 Redis 配置中根本不删 key（与 [ScanClearRedisCache] 修复的
     * `RedisCache.clear()` 是同一个 bug，详见 [ScanClearRedisCache] KDoc）。已改为
     * 直接走 `RedisTemplate` 的 keys/delete API。
     *
     * **限制**：[RedisTemplate.keys] 在大库上会阻塞 Redis；本项目缓存键数量级很小（百级），
     * 可接受。如果未来增长很多，应改成 SCAN-based 迭代删除。
     *
     * @param cacheName Spring Cache 名称
     * @param pattern 业务 key 模式，支持通配符
     */
    override fun evictByPattern(cacheName: String, pattern: String) {
        // 与 [ScanClearRedisCache.clear] 同样的原因：Spring Boot 4.0.6 的
        // `cacheWriter.clear(name, pattern)` 在我们的 Redis 配置下并不真的删 key
        // （见 ScanClearRedisCache KDoc 的根因分析）。这里改用 RedisTemplate.keys + delete
        // 走标准 API。
        val prefixProvider = defaultCacheConfiguration.keyPrefix
        val keyPrefix = prefixProvider.compute(cacheName)
        val realKey: String = versionConfig.getFinalCacheName(keyPrefix)
        val fullPattern = realKey + pattern
        val template = findRedisTemplate() ?: return
        val matched = template.keys(fullPattern)
        if (matched.isNotEmpty()) {
            template.delete(matched)
        }
    }

    /**
     * 是否存在指定 key——通过公开 `Cache.get(key)` 判断（非反射）。
     *
     * 历史：旧实现反射调 `RedisCache.createAndConvertCacheKey`，在 JPMS 模块系统下会失败。
     * 改走公开 API 后简单可靠，且当前 `disableCachingNullValues()` 配置保证不存 null value，
     * 所以"`get` 返回非 null 的 ValueWrapper" 等价于"key 存在"。
     */
    override fun existsKey(cacheName: String, key: Any): Boolean {
        val redisCache = getCache(cacheName) as? RedisCache ?: return false
        return redisCache.get(key) != null
    }

    private val log = LogFactory.getLog(this::class)

}
