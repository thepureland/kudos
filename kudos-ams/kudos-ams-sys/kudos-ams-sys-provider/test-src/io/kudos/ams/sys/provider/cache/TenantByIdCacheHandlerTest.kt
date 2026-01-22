package io.kudos.ams.sys.provider.cache

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.cache.CacheHandlerTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for TenantByIdCacheHandler
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class TenantByIdCacheHandlerTest : CacheHandlerTestBase() {

    @Resource
    private lateinit var cacheHandler: TenantByIdCacheHandler

    @Test
    fun getTenantById() {
        // 存在的
        var id = "118772a0-c053-4634-a5e5-111111111111"
        val cacheItem2 = cacheHandler.getTenantById(id)
        val cacheItem3 = cacheHandler.getTenantById(id)
        assertNotNull(cacheItem2)
        assert(cacheItem3 === cacheItem2)

        // 不存在的
        id = "no_exist_id"
        assertNull(cacheHandler.getTenantById(id))
    }

    @Test
    fun getTenantsByIds() {
        // 都存在的
        var id1 = "118772a0-c053-4634-a5e5-111111111111"
        var id2 = "118772a0-c053-4634-a5e5-222222222222"
        val result2 = cacheHandler.getTenantsByIds(listOf(id1, id2))
        val result3 = cacheHandler.getTenantsByIds(listOf(id1, id2))
        assert(result2.isNotEmpty())
        assert(result3 == result2)

        // 部分存在的
        id1 = "no_exist_id-1"
        var cacheItems = cacheHandler.getTenantsByIds(listOf(id1, id2))
        assertEquals(1, cacheItems.size)

        // 都不存在的
        id2 = "no_exist_id-2"
        cacheItems = cacheHandler.getTenantsByIds(listOf(id1, id2))
        assert(cacheItems.isEmpty())
    }

}