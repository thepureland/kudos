package io.kudos.ability.cache.common.init

import io.kudos.ability.cache.common.batch.keyvalue.DefaultKeysGenerator
import io.kudos.ability.cache.common.init.properties.CacheItemsProperties
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.support.ContextKeyGenerator
import io.kudos.ability.cache.common.support.DefaultCacheConfigProvider
import org.springframework.cache.interceptor.SimpleKeyGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for the bean factory methods of [BaseCacheConfiguration].
 *
 * Each @Bean method is a thin factory; calling them directly verifies they return a non-null instance of the
 * expected concrete type (the auto-configuration wiring itself is covered by integration tests elsewhere).
 *
 * @author K
 * @since 1.0.0
 */
internal class BaseCacheConfigurationTest {

    private val config = BaseCacheConfiguration()

    @Test
    fun cacheVersionConfig_returnsInstance() {
        assertNotNull(config.cacheVersionConfig())
        assertTrue(config.cacheVersionConfig() is CacheVersionConfig)
    }

    @Test
    fun cacheItemsProperties_returnsEmptyContainer() {
        val props = config.cacheItemsProperties()
        assertTrue(props.cacheItems.isEmpty())
        assertTrue(props.cacheItemConfigs.isEmpty())
    }

    @Test
    fun cacheConfigProvider_returnsDefaultProvider() {
        val provider = config.cacheConfigProvider(CacheItemsProperties())
        assertTrue(provider is DefaultCacheConfigProvider)
    }

    @Test
    fun myKeyGenerator_returnsContextKeyGenerator() {
        assertTrue(config.myKeyGenerator() is ContextKeyGenerator)
    }

    @Test
    fun simpleKeyGenerator_returnsSpringSimpleGenerator() {
        assertTrue(config.simpleKeyGenerator() is SimpleKeyGenerator)
    }

    @Test
    fun keysGenerator_returnsDefaultKeysGenerator() {
        assertTrue(config.keysGenerator() is DefaultKeysGenerator)
    }

    @Test
    fun cacheConfigProvider_wiresGivenProperties() {
        val props = CacheItemsProperties().apply {
            cacheItems = mutableListOf("name=X&strategy=REMOTE")
        }
        val provider = config.cacheConfigProvider(props)
        assertEquals(setOf("X"), provider.getAllCacheConfigs().keys)
    }
}
