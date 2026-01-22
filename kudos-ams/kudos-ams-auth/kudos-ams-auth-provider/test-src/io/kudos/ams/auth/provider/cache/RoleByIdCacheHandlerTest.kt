package io.kudos.ams.auth.provider.cache

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


/**
 * junit test for RoleByIdCacheHandler
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class RoleByIdCacheHandlerTest : CacheHandlerTestBase() {

    @Resource
    private lateinit var cacheHandler: RoleByIdCacheHandler

    @Test
    fun getRoleById() {
        // 存在的
        var id = "11111111-1111-1111-1111-111111111111"
        val cacheItem2 = cacheHandler.getRoleById(id)
        val cacheItem3 = cacheHandler.getRoleById(id)
        assertNotNull(cacheItem2)
        assert(cacheItem3 === cacheItem2)

        // 不存在的
        id = "no_exist_id"
        assertNull(cacheHandler.getRoleById(id))
    }

    @Test
    fun getRolesByIds() {
        // 都存在的
        var id1 = "11111111-1111-1111-1111-111111111111"
        var id2 = "22222222-2222-2222-2222-222222222222"
        val result2 = cacheHandler.getRolesByIds(listOf(id1, id2))
        val result3 = cacheHandler.getRolesByIds(listOf(id1, id2))
        assert(result2.isNotEmpty())
        assert(result3 == result2)

        // 部分存在的
        id1 = "no_exist_id-1"
        var cacheItems = cacheHandler.getRolesByIds(listOf(id1, id2))
        assertEquals(1, cacheItems.size)

        // 都不存在的
        id2 = "no_exist_id-2"
        cacheItems = cacheHandler.getRolesByIds(listOf(id1, id2))
        assert(cacheItems.isEmpty())
    }

}
