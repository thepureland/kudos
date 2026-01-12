package io.kudos.ams.sys.provider.cache

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


/**
 * junit test for DictByIdCacheHandler
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class DictByIdCacheHandlerTest : CacheHandlerTestBase() {

    @Autowired
    private lateinit var cacheHandler: DictByIdCacheHandler

    @Test
    fun getDictById() {
        // 存在的
        var id = "68139ed2-dbce-47fa-ac0d-111111111111"
        val cacheItem2 = cacheHandler.getDictById(id)
        val cacheItem3 = cacheHandler.getDictById(id)
        assertNotNull(cacheItem2)
        assert(cacheItem3 === cacheItem2)

        // 不存在的
        id = "no_exist_id"
        val cacheItem = cacheHandler.getDictById(id)
        assertNull(cacheItem)
    }

    @Test
    fun getDictsByIds() {
        // 都存在的
        var id1 = "68139ed2-dbce-47fa-ac0d-111111111111"
        var id2 = "68139ed2-dbce-47fa-ac0d-222222222222"
        val result2 = cacheHandler.getDictsByIds(listOf(id1, id2))
        val result3 = cacheHandler.getDictsByIds(listOf(id1, id2))
        assert(result2.isNotEmpty())
        assert(result3 == result2)

        // 部分存在的
        id1 = "no_exist_id-1"
        var cacheItems = cacheHandler.getDictsByIds(listOf(id1, id2))
        assertEquals(1, cacheItems.size)

        // 都不存在的
        id2 = "no_exist_id-2"
        cacheItems = cacheHandler.getDictsByIds(listOf(id1, id2))
        assert(cacheItems.isEmpty())
    }

}