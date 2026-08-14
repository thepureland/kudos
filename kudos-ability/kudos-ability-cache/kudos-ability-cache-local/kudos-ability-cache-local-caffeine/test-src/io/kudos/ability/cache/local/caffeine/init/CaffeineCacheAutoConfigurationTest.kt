package io.kudos.ability.cache.local.caffeine.init

import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.ability.cache.local.caffeine.CaffeineHashCache
import io.kudos.ability.cache.local.caffeine.CaffeineKeyValueCacheManager
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.ObjectProvider
import kotlin.test.*

/**
 * [CaffeineCacheAutoConfiguration] 与 [CaffeineHashCacheProperties] 纯单元测试。
 *
 * 直接实例化配置类，覆盖各 @Bean 工厂方法的返回类型与属性传递，以及组件名。
 * （Spring 容器内的装配行为已由本模块各 @EnableKudosTest 集成测试覆盖。）
 *
 * @author K
 * @since 1.0.0
 */
internal class CaffeineCacheAutoConfigurationTest {

    private val configuration = CaffeineCacheAutoConfiguration()

    @Test
    fun caffeineCacheManager_returnsCaffeineKeyValueCacheManager() {
        assertIs<CaffeineKeyValueCacheManager>(configuration.caffeineCacheManager())
    }

    @Test
    fun caffeineHashCacheProperties_defaultsToHashCacheDefaultMaximumSize() {
        val properties = configuration.caffeineHashCacheProperties()
        assertEquals(CaffeineHashCache.DEFAULT_MAXIMUM_SIZE, properties.maximumSize)
    }

    @Test
    fun caffeineIdEntitiesHashCache_usesConfiguredMaximumSize() {
        val properties = CaffeineHashCacheProperties().apply { maximumSize = 5 }
        val hashCache = configuration.caffeineIdEntitiesHashCache(properties, emptyConfigProvider())
        assertNotNull(hashCache)
        // maximumSize=5 生效：能正常保存读取
        hashCache.clear("autoConfig")
        assertFalse(hashCache.existsById("autoConfig", "x"))
    }

    @Test
    fun caffeineIdEntitiesHashCache_rejectsNonPositiveMaximumSize() {
        val properties = CaffeineHashCacheProperties().apply { maximumSize = 0 }
        assertFailsWith<IllegalArgumentException> { configuration.caffeineIdEntitiesHashCache(properties, emptyConfigProvider()) }
    }

    @Test
    fun getComponentName() {
        assertEquals("kudos-ability-cache-local-caffeine", configuration.getComponentName())
    }

    /**
     * An [ObjectProvider] that resolves to nothing — mirrors the real startup order, where this configuration
     * is wired before `LinkableCacheAutoConfiguration` has contributed the config provider.
     */
    private fun emptyConfigProvider(): ObjectProvider<ICacheConfigProvider> =
        object : ObjectProvider<ICacheConfigProvider> {
            override fun getObject(): ICacheConfigProvider = throw NoSuchBeanDefinitionException(ICacheConfigProvider::class.java)
            override fun getObject(vararg args: Any?): ICacheConfigProvider = getObject()
            override fun getIfAvailable(): ICacheConfigProvider? = null
            override fun getIfUnique(): ICacheConfigProvider? = null
        }

    @Test
    fun caffeineHashCacheProperties_isMutable() {
        val properties = CaffeineHashCacheProperties()
        properties.maximumSize = 123
        assertEquals(123L, properties.maximumSize)
    }
}
