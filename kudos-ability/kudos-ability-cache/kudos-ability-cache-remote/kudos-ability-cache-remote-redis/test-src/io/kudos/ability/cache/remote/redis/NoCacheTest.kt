package io.kudos.ability.cache.remote.redis

import io.kudos.test.common.SpringTest
import io.kudos.test.common.TestSpringBootContextLoader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.soul.ability.cache.common.MixCacheManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration


/**
 * 不启用缓存测试用例
 *
 * @author K
 * @since 1.0.0
 */
@Import(CacheTestService::class)
@ContextConfiguration(loader = NoCacheTest.NoCacheTestContextLoader::class)
internal class NoCacheTest : SpringTest() {

    @Autowired
    private lateinit var cacheTestService: CacheTestService

    @Autowired(required = false)
    @Qualifier("soulLocalCacheManager")
    private lateinit var localCacheManager: CacheManager

    @Autowired(required = false)
    @Qualifier("soulRemoteCacheManager")
    private lateinit var remoteCacheManager: CacheManager

    @Autowired(required = false)
    private lateinit var mixCacheManager: MixCacheManager

    @Test
    fun testNoCache() {
        assertThrows<UninitializedPropertyAccessException> { localCacheManager }
        assertThrows<UninitializedPropertyAccessException> { remoteCacheManager }
        assertThrows<UninitializedPropertyAccessException> { mixCacheManager }

        val key = "key"
        val value1 = cacheTestService.getFromDB(key)
        val value2 = cacheTestService.getFromDB(key)
        assert(value1 != value2)
    }

    class NoCacheTestContextLoader : TestSpringBootContextLoader() {

        override fun getDynamicProperties(): Map<String, String> {
            return mapOf("kudos.ability.cache.enabled" to "false")
        }

    }

}

