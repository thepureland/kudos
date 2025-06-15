package io.kudos.ability.cache.remote.redis.init

import io.kudos.ability.cache.common.init.BaseCacheConfiguration
import io.kudos.ability.cache.common.init.LinkableCacheAutoConfiguration
import io.kudos.ability.data.memdb.redis.init.RedisAutoConfiguration
import io.kudos.base.logger.LogFactory
import io.kudos.context.init.IComponentInitializer
import org.soul.ability.cache.remote.redis.support.SoulRedisCacheManager
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
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.serializer.RedisSerializationContext
import java.time.Duration


/**
 * redis缓存自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureBefore(LinkableCacheAutoConfiguration::class)
@AutoConfigureAfter(RedisAutoConfiguration::class)
@EnableConfigurationProperties(CacheProperties::class)
@ConditionalOnProperty(prefix = "kudos.ability.cache", name = ["enabled"], havingValue = "true", matchIfMissing = true)
open class RedisCacheAutoConfiguration : BaseCacheConfiguration(), IComponentInitializer {

    private val log = LogFactory.getLog(this)

    @Value("\${kudos.ability.cache.remoteStore}")
    private val remoteStore: String? = null

    @Bean(name = ["soulRemoteCacheManager"])
    open fun remoteCacheManager(
        soulRedisTemplate: SoulRedisTemplate,
        soulRedisProperties: SoulRedisProperties
    ): CacheManager {
        var redisTemplate = soulRedisTemplate.getRedisTemplate(remoteStore)
        if (redisTemplate == null) {
            log.warn("找不到${remoteStore}对应的redis配置，使用默认的redis配置")
            redisTemplate = soulRedisTemplate.defaultRedisTemplate
        }
        val keySerializationPair =
            RedisSerializationContext.SerializationPair.fromSerializer(SoulRedisTemplate.REDIS_KEY_SERIALIZER)
        val valueSerializationPair =
            RedisSerializationContext.SerializationPair.fromSerializer(soulRedisProperties.redisMap[remoteStore]!!.valueSerializer())
        val defaultRedisCacheConfiguration = RedisCacheConfiguration
            .defaultCacheConfig()
            .disableCachingNullValues()
            .entryTtl(Duration.ofSeconds(900)) //默认15分钟
            .serializeKeysWith(keySerializationPair)
            .serializeValuesWith(valueSerializationPair)
        val connectionFactory = redisTemplate!!.connectionFactory
        val redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory)
        return SoulRedisCacheManager(redisCacheWriter, defaultRedisCacheConfiguration, redisTemplate.connectionFactory)
    }

    override fun getComponentName() = "kudos-ability-cache-remote-redis"

}