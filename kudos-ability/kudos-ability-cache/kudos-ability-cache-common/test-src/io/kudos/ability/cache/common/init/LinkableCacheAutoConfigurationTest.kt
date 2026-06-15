package io.kudos.ability.cache.common.init

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimaryAspect
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondaryAspect
import io.kudos.ability.cache.common.aop.keyvalue.DistributedCacheGuardAspect
import io.kudos.ability.cache.common.batch.hash.DefaultHashBatchKeysGenerator
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimaryAspect
import io.kudos.ability.cache.common.batch.keyvalue.BatchCacheableAspect
import io.kudos.ability.cache.common.batch.keyvalue.DefaultKeysGenerator
import io.kudos.ability.cache.common.core.CacheDataInitializer
import io.kudos.ability.cache.common.core.MixCacheInitializing
import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.init.properties.CacheItemsProperties
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.notify.CacheNotifyListener
import io.kudos.ability.cache.common.support.DefaultCacheConfigProvider
import org.springframework.cache.interceptor.SimpleKeyGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the bean factory methods of [LinkableCacheAutoConfiguration].
 *
 * Each @Bean method is a thin factory returning a concrete instance; calling them directly covers the method
 * bodies without bringing up the whole auto-configuration (which requires Redis / MQ infrastructure and is
 * covered by integration tests in downstream modules).
 *
 * @author K
 * @since 1.0.0
 */
internal class LinkableCacheAutoConfigurationTest {

    private val config = LinkableCacheAutoConfiguration()

    @Test
    fun mixCacheManager_returnsInstance() {
        assertTrue(config.mixCacheManager() is MixCacheManager)
    }

    @Test
    fun cacheVersionConfig_returnsInstance() {
        assertTrue(config.cacheVersionConfig() is CacheVersionConfig)
    }

    @Test
    fun cacheConfigProvider_returnsDefaultProvider_wiringProperties() {
        val props = CacheItemsProperties().apply { cacheItems = mutableListOf("name=X&strategy=REMOTE") }
        val provider = config.cacheConfigProvider(props)
        assertTrue(provider is DefaultCacheConfigProvider)
        assertEquals(setOf("X"), provider.getAllCacheConfigs().keys)
    }

    @Test
    fun cacheItemsProperties_returnsEmptyContainer() {
        val props = config.cacheItemsProperties()
        assertTrue(props.cacheItems.isEmpty())
        assertTrue(props.cacheItemConfigs.isEmpty())
    }

    @Test
    fun mixHashCacheManager_returnsInstance() {
        assertTrue(config.mixHashCacheManager() is MixHashCacheManager)
    }

    @Test
    fun mixCacheInitializing_returnsInstance() {
        assertTrue(config.mixCacheInitializing() is MixCacheInitializing)
    }

    @Test
    fun batchCacheableAspect_returnsInstance() {
        assertTrue(config.batchCacheableAspect() is BatchCacheableAspect)
    }

    @Test
    fun distributedCacheGuardAspect_returnsInstance() {
        assertTrue(config.distributedCacheGuardAspect() is DistributedCacheGuardAspect)
    }

    @Test
    fun hashCacheableByPrimaryAspect_returnsInstance() {
        assertTrue(config.hashCacheableByPrimaryAspect() is HashCacheableByPrimaryAspect)
    }

    @Test
    fun hashCacheableBySecondaryAspect_returnsInstance() {
        assertTrue(config.hashCacheableBySecondaryAspect() is HashCacheableBySecondaryAspect)
    }

    @Test
    fun hashBatchCacheableByPrimaryAspect_returnsInstance() {
        assertTrue(config.hashBatchCacheableByPrimaryAspect() is HashBatchCacheableByPrimaryAspect)
    }

    @Test
    fun defaultHashBatchKeysGenerator_returnsInstance() {
        assertTrue(config.defaultHashBatchKeysGenerator() is DefaultHashBatchKeysGenerator)
    }

    @Test
    fun cacheNotifyListener_returnsInstance() {
        assertTrue(config.cacheNotifyListener() is CacheNotifyListener)
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
    fun getComponentName_isStable() {
        assertEquals("kudos-ability-cache-linkable", config.getComponentName())
    }

    @Test
    fun cacheDataInitialize_companionFactory_returnsInstance() {
        assertTrue(LinkableCacheAutoConfiguration.cacheDataInitialize() is CacheDataInitializer)
    }
}
