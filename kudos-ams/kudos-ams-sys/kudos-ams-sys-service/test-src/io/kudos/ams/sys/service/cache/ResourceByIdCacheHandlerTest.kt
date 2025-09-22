package io.kudos.ams.sys.service.cache

import io.kudos.ams.sys.service.dao.SysResourceDao
import org.springframework.beans.factory.annotation.Autowired
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for ResourceByIdCacheHandler
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerAvailable
class ResourceByIdCacheHandlerTest : CacheHandlerTestBase() {

    @Autowired
    private lateinit var cacheHandler: ResourceByIdCacheHandler

    @Autowired
    private lateinit var dao: SysResourceDao

    @Test
    fun getResourceById() {
        // 存在的
        var id = "9b76084a-ceaa-44f1-9c9d-111111111111"
        val cacheItem2 = cacheHandler.getResourceById(id)
        val cacheItem3 = cacheHandler.getResourceById(id)
        assertNotNull(cacheItem2)
        assert(cacheItem3 === cacheItem2)

        // 不存在的
        id = "no_exist_id"
        assertNull(cacheHandler.getResourceById(id))
    }

    @Test
    fun getResourcesByIds() {
        // 都存在的
        var id1 = "9b76084a-ceaa-44f1-9c9d-111111111111"
        var id2 = "9b76084a-ceaa-44f1-9c9d-222222222222"
        val result2 = cacheHandler.getResourcesByIds(listOf(id1, id2))
        val result3 = cacheHandler.getResourcesByIds(listOf(id1, id2))
        assert(result2.isNotEmpty())
        assert(result3 == result2)

        // 部分存在的
        id1 = "no_exist_id-1"
        var cacheItems = cacheHandler.getResourcesByIds(listOf(id1, id2))
        assertEquals(1, cacheItems.size)

        // 都不存在的
        id2 = "no_exist_id-2"
        cacheItems = cacheHandler.getResourcesByIds(listOf(id1, id2))
        assert(cacheItems.isEmpty())
    }

}