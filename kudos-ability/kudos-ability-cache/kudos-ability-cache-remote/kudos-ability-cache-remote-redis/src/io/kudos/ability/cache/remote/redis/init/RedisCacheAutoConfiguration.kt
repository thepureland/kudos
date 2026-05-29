package io.kudos.ability.cache.remote.redis.init

import io.kudos.ability.cache.common.init.BaseCacheConfiguration
import io.kudos.ability.cache.common.init.LinkableCacheAutoConfiguration
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.notify.ICacheMessageHandler
import io.kudos.ability.cache.remote.redis.RedisHashCache
import io.kudos.ability.cache.remote.redis.RedisKeyValueCacheManager
import io.kudos.ability.cache.remote.redis.notice.RedisCacheMessageHandler
import io.kudos.ability.cache.remote.redis.support.RedisCacheKeyConversionService
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
 * Redis cache auto-configuration.
 *
 * RedisTemplates is provided by [RedisAutoConfiguration] (kudos-ability-data-memdb-redis); this class uses
 * [AutoConfigureAfter] to ensure it loads after that, so runtime injection works. IDEs may flag false positives
 * because cross-module references are unresolved.
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
// See ContextAutoConfiguration: IComponentInitializer configuration classes must be instantiated before
// business BPPs; ROLE_INFRASTRUCTURE avoids false positives from Spring's BeanPostProcessorChecker.
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
open class RedisCacheAutoConfiguration : BaseCacheConfiguration(), IComponentInitializer {

    private val log = LogFactory.getLog(this::class)

    @Value($$"${kudos.ability.cache.remoteStore}")
    private val remoteStore: String? = null

    @Autowired
    private lateinit var environment: Environment

    /**
     * Redis cache module properties.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.cache.redis")
    open fun redisCacheProperties(): RedisCacheProperties = RedisCacheProperties()

    /**
     * Wires the remote K-V cache manager: picks the redis instance pointed at by `remoteStore` from
     * [RedisTemplates], builds a default `RedisCacheConfiguration` using that redis's key/value serializers,
     * and wraps it in a [RedisKeyValueCacheManager] for `MixCacheManager` to consume as `remoteCacheManager`.
     *
     * Throws at startup when both `remoteStore` and `defaultRedis` are blank — misconfigured caches should fail fast,
     * not silently become no-ops at runtime.
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
                "Both kudos.ability.cache.remoteStore and kudos.ability.data.redis.defaultRedis are blank; cannot initialize remoteCacheManager"
            )
        }
        var redisTemplate = redisTemplates.getRedisTemplate(selectedStore)
        if (redisTemplate == null) {
            log.warn("No redis configuration found for [{0}], falling back to the default redis template", selectedStore)
            redisTemplate = redisTemplates.defaultRedisTemplate
        }
        val keySerializationPair =
            RedisSerializationContext.SerializationPair.fromSerializer(RedisTemplates.REDIS_KEY_SERIALIZER)
        val extProps = redisProperties.redisMap[selectedStore]
            ?: redisProperties.redisMap[defaultStore]
            ?: throw IllegalStateException(
                "Redis serialization configuration not found: selectedStore=$selectedStore, defaultStore=$defaultStore"
            )
        val valueSerializationPair =
            RedisSerializationContext.SerializationPair.fromSerializer(extProps.valueSerializer())
        val defaultRedisCacheConfiguration = RedisCacheConfiguration
            .defaultCacheConfig()
            .disableCachingNullValues()
            .entryTtl(Duration.ofSeconds(900)) // default 15 minutes
            .serializeKeysWith(keySerializationPair)
            .serializeValuesWith(valueSerializationPair)
            // Override Spring's default `SimpleKey::toString` conversion so raw-String access and
            // single-param SimpleKey access land on the same Redis key. See
            // [RedisCacheKeyConversionService] for the rationale.
            .withConversionService(RedisCacheKeyConversionService.create())
        val connectionFactory = redisTemplate.connectionFactory!!
        val redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory)
        return RedisKeyValueCacheManager(redisCacheWriter, defaultRedisCacheConfiguration)
    }

    /**
     * Redis pub/sub listener container; subscribes to [CacheVersionConfig.realMsgChannel] and forwards
     * messages to [RedisCacheMessageHandler].
     *
     * A custom ErrorHandler raises exceptions to ERROR level — Spring's default ErrorHandler uses
     * `java.util.logging` silently and swallows connection-jitter / resubscribe-failure signals.
     *
     * Virtual threads: when `spring.threads.virtual.enabled=true`, switches to a virtual-thread executor so
     * the pub/sub listener no longer occupies a platform thread.
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

        // The default ErrorHandler uses java.util.logging silently and disappears connection-jitter
        // / resubscribe exceptions from the log. Install a handler that raises them to ERROR level so
        // operators can detect pub/sub channel disconnections or resubscribe failures.
        container.setErrorHandler { t ->
            log.error(t, "Redis cache notification listener error (channel={0})", versionConfig.realMsgChannel)
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
     * Remote cache read/write processor (Hash structure). Used by `TenantAdvancedCacheableAspect` etc. as the
     * storage backend for "tenant-level hash cache"; distinct from [RedisHashCache]'s "id-keyed entity-set hash cache".
     */
    @Bean(name = ["remoteCacheProcessor"])
    @DependsOn("kudosRedisTemplate")
    @ConditionalOnMissingBean
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    open fun remoteCacheProcessor(redisTemplates: RedisTemplates): RedisRemoteCacheProcessor {
        return RedisRemoteCacheProcessor(redisTemplates)
    }

    /**
     * Node identity: prefers the stable id from business configuration; falls back to a startup UUID when unset.
     * Used by [RedisCacheMessageHandler] to detect whether a pub/sub message originated from this node (avoids self-loop clears).
     */
    @Bean("cacheNodeId")
    @ConditionalOnMissingBean(name = ["cacheNodeId"])
    open fun cacheNodeId(redisCacheProperties: RedisCacheProperties): String =
        redisCacheProperties.nodeId?.trim()?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

    /** Cache message SPI implementation (send + receive loopback + error log on deserialization failure). */
    @Bean
    @ConditionalOnMissingBean
    open fun redisCacheMessageHandler(
        @Qualifier("cacheNodeId") cacheNodeId: String
    ): ICacheMessageHandler = RedisCacheMessageHandler(cacheNodeId)

    /**
     * Redis storage backend for the Hash cache. **No broadcasting** — broadcasts are centralized in
     * [io.kudos.ability.cache.common.core.hash.MixHashCache] to avoid the "remote + upper layer each send one"
     * doubled pub/sub traffic.
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
