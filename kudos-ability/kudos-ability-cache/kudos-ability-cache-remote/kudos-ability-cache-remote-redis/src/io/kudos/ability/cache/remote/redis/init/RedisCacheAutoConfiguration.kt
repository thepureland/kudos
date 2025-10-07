package io.kudos.ability.cache.remote.redis.init

import io.kudos.ability.cache.common.init.BaseCacheConfiguration
import io.kudos.ability.cache.common.init.LinkableCacheAutoConfiguration
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.notice.ICacheMessageHandler
import io.kudos.ability.cache.remote.redis.RedisCacheManager
import io.kudos.ability.cache.remote.redis.notice.RedisCacheMessageHandler
import io.kudos.ability.cache.remote.redis.support.RedisRemoteCacheProcessor
import io.kudos.ability.data.memdb.redis.KudosRedisTemplate
import io.kudos.ability.data.memdb.redis.init.RedisAutoConfiguration
import io.kudos.ability.data.memdb.redis.init.properties.RedisProperties
import io.kudos.base.logger.LogFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.cache.CacheProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.thread.Threading
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.core.env.Environment
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
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

    @Autowired
    private lateinit var environment: Environment


    @Bean(name = ["remoteCacheManager"])
    open fun remoteCacheManager(
        kudosRedisTemplate: KudosRedisTemplate,
        redisProperties: RedisProperties
    ): CacheManager {
        var redisTemplate = kudosRedisTemplate.getRedisTemplate(remoteStore!!)
        if (redisTemplate == null) {
            log.warn("找不到${remoteStore}对应的redis配置，使用默认的redis配置")
            redisTemplate = kudosRedisTemplate.defaultRedisTemplate
        }
        val keySerializationPair =
            RedisSerializationContext.SerializationPair.fromSerializer(KudosRedisTemplate.REDIS_KEY_SERIALIZER)
        val valueSerializationPair =
            RedisSerializationContext.SerializationPair.fromSerializer(redisProperties.redisMap[remoteStore]!!.valueSerializer())
        val defaultRedisCacheConfiguration = RedisCacheConfiguration
            .defaultCacheConfig()
            .disableCachingNullValues()
            .entryTtl(Duration.ofSeconds(900)) //默认15分钟
            .serializeKeysWith(keySerializationPair)
            .serializeValuesWith(valueSerializationPair)
        val connectionFactory = redisTemplate.connectionFactory!!
        val redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory)
        return RedisCacheManager(redisCacheWriter, defaultRedisCacheConfiguration, redisTemplate.connectionFactory)
    }

    @Bean
    @DependsOn("redisCacheMessageHandler")
    open fun redisMessageListenerContainer(
        versionConfig: CacheVersionConfig,
        redisCacheMessageHandler: RedisCacheMessageHandler
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.connectionFactory = redisCacheMessageHandler.redisTemplate.connectionFactory
        container.addMessageListener(redisCacheMessageHandler, ChannelTopic(versionConfig.realMsgChannel))

        if (Threading.VIRTUAL.isActive(environment)) {
            // support virtual
            val executor = SimpleAsyncTaskExecutor("redis-msg-")
            executor.setVirtualThreads(true)
            container.setTaskExecutor(executor)
        }
        return container
    }

    @Bean(name = ["remoteCacheProcessor"])
    @DependsOn("kudosRedisTemplate")
    @ConditionalOnMissingBean
    open fun remoteCacheProcessor(kudosRedisTemplate: KudosRedisTemplate): RedisRemoteCacheProcessor {
        return RedisRemoteCacheProcessor(kudosRedisTemplate)
    }

    @Bean
    @ConditionalOnMissingBean
    open fun redisCacheMessageHandler() : ICacheMessageHandler = RedisCacheMessageHandler()

    override fun getComponentName() = "kudos-ability-cache-remote-redis"

}