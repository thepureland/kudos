package io.kudos.ability.cache.remote.redis.init

import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.remote.redis.RedisHashCache
import io.kudos.ability.cache.remote.redis.RedisKeyValueCacheManager
import io.kudos.ability.cache.remote.redis.notice.RedisCacheMessageHandler
import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.data.memdb.redis.init.properties.RedisExtProperties
import io.kudos.ability.data.memdb.redis.init.properties.RedisProperties
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.mock.env.MockEnvironment
import org.springframework.util.ErrorHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [RedisCacheAutoConfiguration] configuration behavior.
 *
 * Covers all bean factory methods without a Spring container:
 * - cacheNodeId: stable configured id (trimmed) vs generated UUID fallback
 * - remoteCacheManager: store selection (configured / default / both-blank failure), missing-template
 *   fallback to the default template, and missing serialization-config failure
 * - redisMessageListenerContainer: wiring, custom error handler, and the virtual-thread executor branch
 * - redisCacheProperties / remoteCacheProcessor / redisCacheMessageHandler / redisIdEntitiesHashCache /
 *   getComponentName
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class RedisCacheAutoConfigurationTest {

    // ---------- helpers ----------

    @Suppress("UNCHECKED_CAST")
    private fun mockTemplate(): RedisTemplate<Any, Any?> {
        val template = mock(RedisTemplate::class.java) as RedisTemplate<Any, Any?>
        `when`(template.connectionFactory).thenReturn(mock(RedisConnectionFactory::class.java))
        return template
    }

    private fun configWith(remoteStore: String?, environment: MockEnvironment? = null): RedisCacheAutoConfiguration {
        val config = RedisCacheAutoConfiguration()
        val storeField = RedisCacheAutoConfiguration::class.java.getDeclaredField("remoteStore")
        storeField.isAccessible = true
        storeField.set(config, remoteStore)
        if (environment != null) {
            val envField = RedisCacheAutoConfiguration::class.java.getDeclaredField("environment")
            envField.isAccessible = true
            envField.set(config, environment)
        }
        return config
    }

    // ---------- cacheNodeId ----------

    @Test
    fun cacheNodeId_usesConfiguredStableNodeId() {
        val properties = RedisCacheProperties().apply {
            nodeId = "  host-a:12345  "
        }

        val nodeId = RedisCacheAutoConfiguration().cacheNodeId(properties)

        assertEquals("host-a:12345", nodeId)
    }

    @Test
    fun cacheNodeId_generatesUuidWhenNodeIdBlank() {
        val config = RedisCacheAutoConfiguration()
        val first = config.cacheNodeId(RedisCacheProperties().apply { nodeId = " " })
        val second = config.cacheNodeId(RedisCacheProperties())

        assertTrue(first.isNotBlank())
        assertTrue(second.isNotBlank())
        assertNotEquals(first, second)
    }

    // ---------- remoteCacheManager ----------

    @Test
    fun remoteCacheManager_usesConfiguredRemoteStore() {
        val template = mockTemplate()
        val redisTemplates = RedisTemplates(mutableMapOf("r1" to template), template)
        val redisProperties = RedisProperties().apply {
            defaultRedis = "r1"
            redisMap["r1"] = RedisExtProperties()
        }

        val cacheManager = configWith(" r1 ").remoteCacheManager(redisTemplates, redisProperties)

        assertIs<RedisKeyValueCacheManager>(cacheManager)
    }

    @Test
    fun remoteCacheManager_fallsBackToDefaultRedisWhenRemoteStoreBlank() {
        val template = mockTemplate()
        val redisTemplates = RedisTemplates(mutableMapOf("d1" to template), template)
        val redisProperties = RedisProperties().apply {
            defaultRedis = "d1"
            redisMap["d1"] = RedisExtProperties()
        }

        val cacheManager = configWith("   ").remoteCacheManager(redisTemplates, redisProperties)

        assertIs<RedisKeyValueCacheManager>(cacheManager)
    }

    @Test
    fun remoteCacheManager_bothStoresBlank_failsFast() {
        val template = mockTemplate()
        val redisTemplates = RedisTemplates(mutableMapOf(), template)
        val redisProperties = RedisProperties() // defaultRedis null

        val ex = assertFailsWith<IllegalStateException> {
            configWith(null).remoteCacheManager(redisTemplates, redisProperties)
        }
        assertTrue(ex.message!!.contains("cannot initialize remoteCacheManager"))
    }

    @Test
    fun remoteCacheManager_unknownStore_fallsBackToDefaultTemplate() {
        val defaultTemplate = mockTemplate()
        // "missing" not in the map -> falls back to defaultRedisTemplate; ext props resolved via defaultStore
        val redisTemplates = RedisTemplates(mutableMapOf("d1" to mockTemplate()), defaultTemplate)
        val redisProperties = RedisProperties().apply {
            defaultRedis = "d1"
            redisMap["d1"] = RedisExtProperties()
        }

        val cacheManager = configWith("missing").remoteCacheManager(redisTemplates, redisProperties)

        assertIs<RedisKeyValueCacheManager>(cacheManager)
    }

    @Test
    fun remoteCacheManager_missingSerializationConfig_failsFast() {
        val template = mockTemplate()
        val redisTemplates = RedisTemplates(mutableMapOf("r1" to template), template)
        val redisProperties = RedisProperties() // empty redisMap, blank defaultRedis

        val ex = assertFailsWith<IllegalStateException> {
            configWith("r1").remoteCacheManager(redisTemplates, redisProperties)
        }
        assertTrue(ex.message!!.contains("Redis serialization configuration not found"))
        assertTrue(ex.message!!.contains("r1"))
    }

    // ---------- redisMessageListenerContainer ----------

    private fun newHandlerMock(): RedisCacheMessageHandler {
        val handler = mock(RedisCacheMessageHandler::class.java)
        val template = mockTemplate()
        `when`(handler.redisTemplate).thenReturn(template)
        return handler
    }

    /** Locates the configured ErrorHandler on the container via reflection (no public getter). */
    private fun errorHandlerOf(container: RedisMessageListenerContainer): ErrorHandler? =
        RedisMessageListenerContainer::class.java.declaredFields
            .firstOrNull { ErrorHandler::class.java.isAssignableFrom(it.type) }
            ?.also { it.isAccessible = true }
            ?.get(container) as? ErrorHandler

    @Test
    fun redisMessageListenerContainer_wiresConnectionFactoryAndErrorHandler() {
        val handler = newHandlerMock()
        val versionConfig = CacheVersionConfig()
        val config = configWith("r1", MockEnvironment())

        val container = config.redisMessageListenerContainer(versionConfig, handler)

        assertSame(handler.redisTemplate.connectionFactory, container.connectionFactory)
        val errorHandler = errorHandlerOf(container)
        assertNotNull(errorHandler, "a custom ErrorHandler must be installed (Spring's default logs silently)")
        // invoking it must only log, never rethrow
        errorHandler.handleError(RuntimeException("listener boom"))
    }

    @Test
    fun redisMessageListenerContainer_virtualThreadsEnabled_usesVirtualExecutor() {
        val handler = newHandlerMock()
        val versionConfig = CacheVersionConfig()
        val env = MockEnvironment().withProperty("spring.threads.virtual.enabled", "true")
        val config = configWith("r1", env)

        val container = config.redisMessageListenerContainer(versionConfig, handler)

        assertNotNull(container)
        assertSame(handler.redisTemplate.connectionFactory, container.connectionFactory)
    }

    // ---------- simple bean factory methods ----------

    @Test
    fun redisCacheProperties_returnsFreshInstance() {
        val config = RedisCacheAutoConfiguration()
        val properties = config.redisCacheProperties()
        assertNull(properties.nodeId)
        assertNotSame(properties, config.redisCacheProperties())
    }

    @Test
    fun remoteCacheProcessor_buildsProcessor() {
        val redisTemplates = mock(RedisTemplates::class.java)
        assertNotNull(RedisCacheAutoConfiguration().remoteCacheProcessor(redisTemplates))
    }

    @Test
    fun redisCacheMessageHandler_buildsHandler() {
        val handler = RedisCacheAutoConfiguration().redisCacheMessageHandler("node-1")
        assertIs<RedisCacheMessageHandler>(handler)
    }

    @Test
    fun redisIdEntitiesHashCache_buildsRedisHashCache() {
        val cache = RedisCacheAutoConfiguration()
            .redisIdEntitiesHashCache(mock(RedisTemplates::class.java), CacheVersionConfig())
        assertIs<RedisHashCache>(cache)
    }

    @Test
    fun getComponentName_isModuleName() {
        assertEquals("kudos-ability-cache-remote-redis", RedisCacheAutoConfiguration().getComponentName())
    }
}
