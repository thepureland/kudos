package io.kudos.ams.sys.provider.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ams.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.ams.sys.provider.dao.SysResourceDao
import io.kudos.ams.sys.provider.model.po.SysResource
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for ResourceByIdCacheHandler
 *
 * 测试数据来源：`ResourceByIdCacheHandlerTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ResourceByIdCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: ResourceByIdCacheHandler

    @Resource
    private lateinit var dao: SysResourceDao

    private val newResourceName = "新资源名称"

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

    @Test
    fun syncOnInsert() {
        // 插入新的记录到数据库
        val id = insertNewRecordToDb()

        // 同步缓存
        cacheHandler.syncOnInsert(id)

        // 验证新记录是否在缓存中
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), id)
        assertNotNull(cacheItem1)
        val cacheItem2 = cacheHandler.getResourceById(id)
        assertNotNull(cacheItem2)
        val cacheItem3 = cacheHandler.getResourceById(id)
        assert(cacheItem2 === cacheItem3)
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val id = "9b76084a-ceaa-44f1-9c9d-222222222222"
        val success = dao.updateProperties(id, mapOf(SysResource::name.name to newResourceName))
        assert(success)

        // 同步缓存
        cacheHandler.syncOnUpdate(id)

        // 验证缓存中的记录
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), id) as SysResourceCacheItem?
        assertNotNull(cacheItem1)
        assertEquals(newResourceName, cacheItem1.name)
        val cacheItem2 = cacheHandler.getResourceById(id)
        assertNotNull(cacheItem2)
        assertEquals(newResourceName, cacheItem2.name)
    }

    @Test
    fun syncOnDelete() {
        // 删除数据库中的记录
        val id = insertNewRecordToDb()
        val deleteSuccess = dao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(id)

        // 验证缓存中有没有
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), id)
        assertNull(cacheItem1)
        val cacheItem2 = cacheHandler.getResourceById(id)
        assertNull(cacheItem2)
    }

    @Test
    fun syncOnBatchDelete() {
        // 批量删除数据库中的记录
        val id1 = insertNewRecordToDb()
        val id2 = insertNewRecordToDb()
        val ids = listOf(id1, id2)
        val count = dao.batchDelete(ids)
        assert(count == 2)

        // 同步缓存
        cacheHandler.syncOnBatchDelete(ids)

        // 验证缓存中有没有
        var cacheItem = CacheKit.getValue(cacheHandler.cacheName(), id1)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getResourceById(id1)
        assertNull(cacheItem)
        cacheItem = CacheKit.getValue(cacheHandler.cacheName(), id2)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getResourceById(id2)
        assertNull(cacheItem)
    }

    private fun insertNewRecordToDb(): String {
        val sysResource = SysResource().apply {
            name = "测试资源_${System.currentTimeMillis()}"
            url = "/test/resource"
            resourceTypeDictCode = "1"
            subSystemCode = "subSys-a"
        }
        return dao.insert(sysResource)
    }

}