package io.kudos.ability.cache.remote.redis.init

import io.kudos.ability.cache.common.init.BaseCacheConfiguration
import io.kudos.ability.cache.common.init.LinkableCacheAutoConfiguration
import io.kudos.ability.data.memdb.redis.init.RedisAutoConfiguration
import io.kudos.base.logger.LoggerFactory
import org.soul.ability.cache.redis.support.SoulRedisCacheManager
import org.soul.ability.data.memdb.redis.SoulRedisTemplate
import org.soul.ability.data.memdb.redis.starter.properties.SoulRedisProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.cache.CacheProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.serializer.RedisSerializationContext
import java.time.Duration
import java.util.*


/**
 * redis缓存自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@AutoConfigureBefore(LinkableCacheAutoConfiguration::class)
@AutoConfigureAfter(RedisAutoConfiguration::class)
@EnableConfigurationProperties(CacheProperties::class)
@Configuration
open class RedisCacheConfiguration : BaseCacheConfiguration() {

    private val log = LoggerFactory.getLogger(this)

    @get:Value("\${kudos.ability.cache.remoteStore}")
    private val remoteStore: String? = null

    @Bean(name = ["soulRemoteCacheManager"])
    @DependsOn("soulRedisTemplate")
    @ConditionalOnProperty(
        prefix = "kudos.ability.cache",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = false
    )
    open fun remoteCacheManager(soulRedisTemplate: SoulRedisTemplate, soulRedisProperties: SoulRedisProperties): CacheManager {
        var redisTemplate = soulRedisTemplate.getRedisTemplate(remoteStore)
        if (redisTemplate == null) {
            log.warn("找不到${remoteStore}对应的redis配置，使用默认的redis配置")
            redisTemplate = soulRedisTemplate.defaultRedisTemplate
        }
        val redisCacheConfiguration = RedisCacheConfiguration
            .defaultCacheConfig()
            .disableCachingNullValues()
            .entryTtl(Duration.ofSeconds(900)) //默认15分钟
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(SoulRedisTemplate.REDIS_KEY_SERIALIZER))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(soulRedisProperties.redisMap[remoteStore]!!.valueSerializer())
            )
        val connectionFactory = redisTemplate!!.connectionFactory
        val redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory)
        return SoulRedisCacheManager(redisCacheWriter, redisCacheConfiguration)
    }

}