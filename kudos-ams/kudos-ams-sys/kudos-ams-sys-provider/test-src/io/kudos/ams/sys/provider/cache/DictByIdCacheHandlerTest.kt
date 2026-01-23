package io.kudos.ams.sys.provider.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ams.sys.common.vo.dict.SysDictCacheItem
import io.kudos.ams.sys.provider.dao.SysDictDao
import io.kudos.ams.sys.provider.model.po.SysDict
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.cache.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


/**
 * junit test for DictByIdCacheHandler
 *
 * 测试数据来源：`V1.0.0.4__DictByIdCacheHandlerTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class DictByIdCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: DictByIdCacheHandler

    @Resource
    private lateinit var sysDictDao: SysDictDao

    private val newDictName = "新字典名称"

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

    @Test
    fun syncOnInsert() {
        // 插入新的记录到数据库
        val id = insertNewRecordToDb()

        // 同步缓存
        cacheHandler.syncOnInsert(id)

        // 验证新记录是否在缓存中
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), id)
        assertNotNull(cacheItem1)
        val cacheItem2 = cacheHandler.getDictById(id)
        assertNotNull(cacheItem2)
        val cacheItem3 = cacheHandler.getDictById(id)
        assert(cacheItem2 === cacheItem3)
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val id = "68139ed2-dbce-47fa-ac0d-222222222222"
        val success = sysDictDao.updateProperties(id, mapOf(SysDict::dictName.name to newDictName))
        assert(success)

        // 同步缓存
        cacheHandler.syncOnUpdate(id)

        // 验证缓存中的记录
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), id) as SysDictCacheItem?
        assertNotNull(cacheItem1)
        assertEquals(newDictName, cacheItem1.dictName)
        val cacheItem2 = cacheHandler.getDictById(id)
        assertNotNull(cacheItem2)
        assertEquals(newDictName, cacheItem2.dictName)
    }

    @Test
    fun syncOnDelete() {
        // 删除数据库中的记录
        val id = insertNewRecordToDb()
        val deleteSuccess = sysDictDao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(id)

        // 验证缓存中有没有
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), id)
        assertNull(cacheItem1)
        val cacheItem2 = cacheHandler.getDictById(id)
        assertNull(cacheItem2)
    }

    @Test
    fun syncOnBatchDelete() {
        // 批量删除数据库中的记录
        val id1 = insertNewRecordToDb()
        val id2 = insertNewRecordToDb()
        val ids = listOf(id1, id2)
        val count = sysDictDao.batchDelete(ids)
        assert(count == 2)

        // 同步缓存
        cacheHandler.syncOnBatchDelete(ids)

        // 验证缓存中有没有
        var cacheItem = CacheKit.getValue(cacheHandler.cacheName(), id1)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getDictById(id1)
        assertNull(cacheItem)
        cacheItem = CacheKit.getValue(cacheHandler.cacheName(), id2)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getDictById(id2)
        assertNull(cacheItem)
    }

    private fun insertNewRecordToDb(): String {
        val sysDict = SysDict().apply {
            dictType = "TEST_DICT_TYPE_${System.currentTimeMillis()}"
            dictName = "测试字典"
            moduleCode = "default"
        }
        return sysDictDao.insert(sysDict)
    }

}