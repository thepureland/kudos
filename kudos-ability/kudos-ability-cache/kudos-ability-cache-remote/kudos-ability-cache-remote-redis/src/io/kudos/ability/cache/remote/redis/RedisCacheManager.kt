package io.kudos.ability.cache.remote.redis

import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheManager
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.cache.RedisCache
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.connection.RedisConnectionFactory
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.*

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
class RedisCacheManager(
    private val cacheWriter: RedisCacheWriter,
    private val defaultCacheConfiguration: RedisCacheConfiguration,
    private val connectionFactory: RedisConnectionFactory?
) : RedisCacheManager(
    cacheWriter,
    defaultCacheConfiguration
), ICacheManager<RedisCache> {

    var caches: MutableList<RedisCache> = LinkedList<RedisCache>()

    @Autowired
    private lateinit var versionConfig: CacheVersionConfig

    override fun initCacheAfterSystemInit(cacheConfigMap: Map<String, CacheConfig>) {
        cacheConfigMap.forEach { (key: String, cacheConfig: CacheConfig) ->
            val cache: RedisCache = createCache(cacheConfig)
            log.debug("初始化远程缓存【{0}】成功！", key)
            addCache(cache)
        }
        afterPropertiesSet()
    }

    @Synchronized
    fun addCache(cache: RedisCache) {
        this.caches.add(cache)
    }

    override fun loadCaches(): MutableCollection<RedisCache> {
        return caches
    }

    override fun createCache(cacheConfig: CacheConfig): RedisCache {
        var redisCacheConfiguration = RedisCacheConfiguration
            .defaultCacheConfig()
            .disableCachingNullValues()
            .serializeKeysWith(defaultCacheConfiguration.keySerializationPair)
            .serializeValuesWith(defaultCacheConfiguration.valueSerializationPair)
        if (cacheConfig.ttl != null) {
            redisCacheConfiguration = redisCacheConfiguration.entryTtl(Duration.ofSeconds(cacheConfig.ttl!!.toLong()))
        }
        val realKey: String = versionConfig.getFinalCacheName(cacheConfig.name!!)
        return createRedisCache(realKey, redisCacheConfiguration)
    }

    /**
     * 按模式删除某个 cacheName 下的所有 key（用 SCAN 替代 KEYS）
     *
     * @param cacheName Spring Cache 名称
     * @param pattern   业务 key 模式，比如 "user:*"
     */
    override fun evictByPattern(cacheName: String, pattern: String) {
        val prefixProvider = defaultCacheConfiguration.keyPrefix
        val keyPrefix = prefixProvider.compute(cacheName)
        val realKey: String = versionConfig.getFinalCacheName(keyPrefix)
        val fullPattern = realKey + pattern
        val patternBytes = fullPattern.toByteArray(StandardCharsets.UTF_8)
        cacheWriter.clear(realKey, patternBytes)
    }

    private val log = LogFactory.getLog(this)

}
