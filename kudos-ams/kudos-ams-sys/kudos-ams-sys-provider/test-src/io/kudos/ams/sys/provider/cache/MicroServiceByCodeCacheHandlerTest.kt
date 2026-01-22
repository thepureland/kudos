package io.kudos.ams.sys.provider.cache

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.cache.CacheHandlerTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for MicroServiceByCodeCacheHandler
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class MicroServiceByCodeCacheHandlerTest : CacheHandlerTestBase() {

    @Resource
    private lateinit var cacheHandler: MicroServiceByCodeCacheHandler

    @Test
    fun getMicroServiceByCode() {
        // 存在的
        var code = "code-1"
        val cacheItem = cacheHandler.getMicroServiceByCode(code)
        assertNotNull(cacheItem)
        assert(cacheItem === cacheHandler.getMicroServiceByCode(code))

        // 不存在的
        code = "no_exist_code"
        assertNull(cacheHandler.getMicroServiceByCode(code))
    }

    @Test
    fun getMicroServicesByCodes() {
        // 都存在的
        var code1 = "code-1"
        var code2 = "code-2"
        val result = cacheHandler.getMicroServicesByCodes(listOf(code1, code2))
        assert(result.isNotEmpty())
        assert(result == cacheHandler.getMicroServicesByCodes(listOf(code1, code2)))

        // 部分存在的
        code1 = "no_exist_code-1"
        var cacheItems = cacheHandler.getMicroServicesByCodes(listOf(code1, code2))
        assertEquals(1, cacheItems.size)

        // 都不存在的
        code2 = "no_exist_code-2"
        cacheItems = cacheHandler.getMicroServicesByCodes(listOf(code1, code2))
        assert(cacheItems.isEmpty())
    }

}