package io.kudos.ability.cache.remote.redis.keyvalue

import io.kudos.ability.cache.common.core.MixCacheManager
import io.kudos.test.common.init.EnableKudosTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.Test
import kotlin.test.assertFailsWith


/**
 * 不启用缓存测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@Import(CacheTestService::class)
internal class NoCacheTest {

    @Autowired
    private lateinit var cacheTestService: CacheTestService

    @Autowired(required = false)
    @Qualifier("localCacheManager")
    private lateinit var localCacheManager: CacheManager

    @Autowired(required = false)
    @Qualifier("remoteCacheManager")
    private lateinit var remoteCacheManager: CacheManager

    @Autowired(required = false)
    private lateinit var mixCacheManager: MixCacheManager


    companion object {

        @DynamicPropertySource
        @JvmStatic
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.cache.enabled") { "false" }
        }

    }

    @Test
    fun testNoCache() {
        assertFailsWith<UninitializedPropertyAccessException> { localCacheManager }
        assertFailsWith<UninitializedPropertyAccessException> { remoteCacheManager }
        assertFailsWith<UninitializedPropertyAccessException> { mixCacheManager }

        val key = "key"
        val value1 = cacheTestService.getFromDB(key)
        val value2 = cacheTestService.getFromDB(key)
        assert(value1 != value2)
    }

}

