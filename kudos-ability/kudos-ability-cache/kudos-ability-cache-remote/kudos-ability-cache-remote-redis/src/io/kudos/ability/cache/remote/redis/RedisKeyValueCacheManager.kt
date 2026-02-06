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
class RedisKeyValueCacheManager(
    private val cacheWriter: RedisCacheWriter,
    private val defaultCacheConfiguration: RedisCacheConfiguration,
    private val connectionFactory: RedisConnectionFactory?
) : RedisCacheManager(
    cacheWriter,
    defaultCacheConfiguration
), IKeyValueCacheManager<RedisCache> {

    var caches: MutableList<RedisCache> = LinkedList<RedisCache>()

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

    @Synchronized
    fun addCache(cache: RedisCache) {
        this.caches.add(cache)
    }

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
        if (cacheConfig.ttl != null) {
            redisCacheConfiguration = redisCacheConfiguration.entryTtl(Duration.ofSeconds(cacheConfig.ttl!!.toLong()))
        }
        val realKey: String = versionConfig.getFinalCacheName(cacheConfig.name!!)
        return createRedisCache(realKey, redisCacheConfiguration)
    }

    /**
     * 按模式删除某个缓存下的所有key
     * 
     * 使用SCAN命令替代KEYS命令，避免在生产环境阻塞Redis。
     * 
     * 工作流程：
     * 1. 获取缓存名称的key前缀（通过keyPrefixProvider计算）
     * 2. 应用版本前缀，得到实际的缓存key前缀
     * 3. 拼接完整的匹配模式：实际key前缀 + 业务模式
     * 4. 将模式转换为字节数组
     * 5. 调用cacheWriter.clear执行模式删除
     * 
     * 模式匹配：
     * - 支持通配符模式，例如"user:*"会匹配所有以"user:"开头的key
     * - 最终匹配模式 = 版本前缀 + 缓存名称前缀 + 业务模式
     * - 例如：版本"v1"，缓存"user"，模式"*"，最终为"v1::user:*"
     * 
     * SCAN vs KEYS：
     * - 使用SCAN命令替代KEYS，避免阻塞Redis服务器
     * - SCAN是增量式遍历，不会一次性返回所有匹配的key
     * - 适合生产环境使用，不会影响Redis性能
     * 
     * 注意事项：
     * - 模式删除会删除所有匹配的key，需谨慎使用
     * - 删除操作是异步的，不会立即生效
     * - 会自动应用版本前缀和缓存名称前缀
     * 
     * @param cacheName Spring Cache名称
     * @param pattern 业务key模式，支持通配符，例如"user:*"
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
