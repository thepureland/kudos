package io.kudos.ams.auth.provider.cache

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


/**
 * junit test for UserByIdCacheHandler
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserByIdCacheHandlerTest : CacheHandlerTestBase() {

    @Resource
    private lateinit var cacheHandler: UserByIdCacheHandler

    @Test
    fun getUserById() {
        // 存在的
        var id = "11111111-1111-1111-1111-111111111111"
        val cacheItem2 = cacheHandler.getUserById(id)
        val cacheItem3 = cacheHandler.getUserById(id)
        assertNotNull(cacheItem2)
        assert(cacheItem3 === cacheItem2)

        // 不存在的
        id = "no_exist_id"
        assertNull(cacheHandler.getUserById(id))
    }

    @Test
    fun getUsersByIds() {
        // 都存在的
        var id1 = "11111111-1111-1111-1111-111111111111"
        var id2 = "22222222-2222-2222-2222-222222222222"
        val result2 = cacheHandler.getUsersByIds(listOf(id1, id2))
        val result3 = cacheHandler.getUsersByIds(listOf(id1, id2))
        assert(result2.isNotEmpty())
        assert(result3 == result2)

        // 部分存在的
        id1 = "no_exist_id-1"
        var cacheItems = cacheHandler.getUsersByIds(listOf(id1, id2))
        assertEquals(1, cacheItems.size)

        // 都不存在的
        id2 = "no_exist_id-2"
        cacheItems = cacheHandler.getUsersByIds(listOf(id1, id2))
        assert(cacheItems.isEmpty())
    }

}
