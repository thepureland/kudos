package io.kudos.ability.cache.remote.redis.init

import io.kudos.ability.cache.common.init.BaseCacheConfiguration
import io.kudos.ability.cache.common.init.LinkableCacheAutoConfiguration
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.notify.ICacheMessageHandler
import io.kudos.ability.cache.remote.redis.RedisHashCache
import io.kudos.ability.cache.remote.redis.RedisKeyValueCacheManager
import io.kudos.ability.cache.remote.redis.notice.RedisCacheMessageHandler
import io.kudos.ability.cache.remote.redis.support.RedisRemoteCacheProcessor
import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.data.memdb.redis.init.RedisAutoConfiguration
import io.kudos.ability.data.memdb.redis.init.properties.RedisProperties
import io.kudos.base.logger.LogFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.cache.autoconfigure.CacheProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.thread.Threading
import org.springframework.cache.CacheManager
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Role
import org.springframework.core.env.Environment
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.serializer.RedisSerializationContext
import java.time.Duration
import java.util.UUID


/**
 * redis缓存自动配置类
 *
 * RedisTemplates 由 [RedisAutoConfiguration]（kudos-ability-data-memdb-redis）提供，
 * 本类通过 [AutoConfigureAfter] 保证在其之后加载，运行时注入正常；IDE 可能因跨模块未解析而误报。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@AutoConfigureBefore(LinkableCacheAutoConfiguration::class)
@AutoConfigureAfter(RedisAutoConfiguration::class)
@EnableConfigurationProperties(CacheProperties::class)
@ConditionalOnProperty(prefix = "kudos.ability.cache", name = ["enabled"], havingValue = "true", matchIfMissing = true)
// 见 ContextAutoConfiguration：IComponentInitializer 配置类必须早于业务 BPP 实例化，
// 加 ROLE_INFRASTRUCTURE 避免 Spring 的 BeanPostProcessorChecker 误报。
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
open class RedisCacheAutoConfiguration : BaseCacheConfiguration(), IComponentInitializer {

    private val log = LogFactory.getLog(this::class)

    @Value($$"${kudos.ability.cache.remoteStore}")
    private val remoteStore: String? = null

    @Autowired
    private lateinit var environment: Environment

    /**
     * Redis 缓存模块配置。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.cache.redis")
    open fun redisCacheProperties(): RedisCacheProperties = RedisCacheProperties()

    /**
     * 装配远程 K-V 缓存管理器：从 [RedisTemplates] 里挑出 `remoteStore` 配置指向的 redis 实例，
     * 用对应 redis 的 key / value 序列化器构造默认 `RedisCacheConfiguration`，最终包成
     * [RedisKeyValueCacheManager] 给 `MixCacheManager` 取作 `remoteCacheManager`。
     *
     * `remoteStore` 与 `defaultRedis` 都没配时启动期抛错——缓存配错应启动失败，而不是
     * 跑起来后才发现"缓存其实是 no-op"。
     */
    @Bean(name = ["remoteCacheManager"])
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    open fun remoteCacheManager(
        redisTemplates: RedisTemplates,
        redisProperties: RedisProperties
    ): CacheManager {
        val configuredStore = remoteStore?.trim().orEmpty()
        val defaultStore = redisProperties.defaultRedis?.trim().orEmpty()
        val selectedStore = when {
            configuredStore.isNotBlank() -> configuredStore
            defaultStore.isNotBlank() -> defaultStore
            else -> throw IllegalStateException(
                "kudos.ability.cache.remoteStore 与 kudos.ability.data.redis.defaultRedis 同时为空，无法初始化 remoteCacheManager"
            )
        }
        var redisTemplate = redisTemplates.getRedisTemplate(selectedStore)
        if (redisTemplate == null) {
            log.warn("找不到{0}对应的redis配置，使用默认的redis模板", selectedStore)
            redisTemplate = redisTemplates.defaultRedisTemplate
        }
        val keySerializationPair =
            RedisSerializationContext.SerializationPair.fromSerializer(RedisTemplates.REDIS_KEY_SERIALIZER)
        val extProps = redisProperties.redisMap[selectedStore]
            ?: redisProperties.redisMap[defaultStore]
            ?: throw IllegalStateException(
                "找不到redis序列化配置: selectedStore=$selectedStore, defaultStore=$defaultStore"
            )
        val valueSerializationPair =
            RedisSerializationContext.SerializationPair.fromSerializer(extProps.valueSerializer())
        val defaultRedisCacheConfiguration = RedisCacheConfiguration
            .defaultCacheConfig()
            .disableCachingNullValues()
            .entryTtl(Duration.ofSeconds(900)) //默认15分钟
            .serializeKeysWith(keySerializationPair)
            .serializeValuesWith(valueSerializationPair)
        val connectionFactory = redisTemplate.connectionFactory!!
        val redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory)
        return RedisKeyValueCacheManager(redisCacheWriter, defaultRedisCacheConfiguration)
    }

    /**
     * Redis pub/sub 监听容器，订阅 [CacheVersionConfig.realMsgChannel] 通道，
     * 把消息转发给 [RedisCacheMessageHandler]。
     *
     * 自定义 ErrorHandler 把异常抬到 error 级别——Spring 默认 ErrorHandler 走
     * `java.util.logging` 且 silent，连接抖动 / 重订阅失败的事故信号会被吞掉。
     *
     * 虚拟线程：当 `spring.threads.virtual.enabled=true` 时改用虚拟线程执行器，
     * pub/sub 监听不再独占平台线程。
     */
    @Bean
    @DependsOn("redisCacheMessageHandler")
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    open fun redisMessageListenerContainer(
        versionConfig: CacheVersionConfig,
        redisCacheMessageHandler: RedisCacheMessageHandler
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(redisCacheMessageHandler.redisTemplate.connectionFactory!!)
        container.addMessageListener(redisCacheMessageHandler, ChannelTopic(versionConfig.realMsgChannel))

        // 缺省 ErrorHandler 走 java.util.logging 且 silent，连接抖动/订阅恢复异常会消失在日志里。
        // 这里挂一个把异常抬到 error 级别的 handler，便于运维感知 pub/sub 通道断连或重订阅失败。
        container.setErrorHandler { t ->
            log.error(t, "Redis 缓存通知监听异常 (channel={0})", versionConfig.realMsgChannel)
        }

        if (Threading.VIRTUAL.isActive(environment)) {
            // support virtual
            val executor = SimpleAsyncTaskExecutor("redis-msg-")
            executor.setVirtualThreads(true)
            container.setTaskExecutor(executor)
        }
        return container
    }

    /**
     * 远程缓存读写处理器（Hash 结构）。被 `TenantAdvancedCacheableAspect` 等用作"租户级 hash 缓存"
     * 的存储后端，区别于 [RedisHashCache] 的"带 id 实体集合 hash 缓存"。
     */
    @Bean(name = ["remoteCacheProcessor"])
    @DependsOn("kudosRedisTemplate")
    @ConditionalOnMissingBean
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    open fun remoteCacheProcessor(redisTemplates: RedisTemplates): RedisRemoteCacheProcessor {
        return RedisRemoteCacheProcessor(redisTemplates)
    }

    /**
     * 节点身份标识：优先使用业务配置的稳定 id；未配置时回退启动期 UUID。
     * 用于 [RedisCacheMessageHandler] 判断 pub/sub 消息是不是本节点自发的（避免回环清理）。
     */
    @Bean("cacheNodeId")
    @ConditionalOnMissingBean(name = ["cacheNodeId"])
    open fun cacheNodeId(redisCacheProperties: RedisCacheProperties): String =
        redisCacheProperties.nodeId?.trim()?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

    /** 缓存消息 SPI 实现（发送 + 接收回环 + 反序列化失败的 error 日志）。 */
    @Bean
    @ConditionalOnMissingBean
    open fun redisCacheMessageHandler(
        @Qualifier("cacheNodeId") cacheNodeId: String
    ): ICacheMessageHandler = RedisCacheMessageHandler(cacheNodeId)

    /**
     * Hash 缓存的 Redis 存储后端。**不带广播**——广播由 [io.kudos.ability.cache.common.core.hash.MixHashCache]
     * 统一处理，避免出现"远程 + 上层各发一条"的双倍 pub/sub 流量。
     */
    @Bean("redisIdEntitiesHashCache")
    @ConditionalOnMissingBean(name = ["redisIdEntitiesHashCache"])
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    open fun redisIdEntitiesHashCache(
        redisTemplates: RedisTemplates,
        versionConfig: CacheVersionConfig
    ): RedisHashCache {
        return RedisHashCache(redisTemplates, versionConfig)
    }

    override fun getComponentName() = "kudos-ability-cache-remote-redis"

}
