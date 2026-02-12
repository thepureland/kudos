package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheItem
import io.kudos.ms.sys.core.dao.SysDictItemDao
import io.kudos.ms.sys.core.model.po.SysDictItem
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * junit test for DictItemsByModuleAndTypeCacheHandler
 *
 * 测试数据来源：`DictItemsByModuleAndTypeCacheTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class DictItemsByModuleAndTypeCacheTest : RdbAndRedisCacheTestBase() {
    
    @Resource
    private lateinit var cacheHandler: DictItemsByModuleAndTypeCache
    
    @Resource
    private lateinit var dictByIdCache: DictByIdCache
    
    @Resource
    private lateinit var sysDictItemDao: SysDictItemDao
    
    private val newName = "newName"

    @Test
    fun reloadAll() {
        // 清除并重载缓存，保证与数据库中的数据一致
        cacheHandler.reloadAll(true)

        // 获取当前缓存中的记录
        val atomicServiceCode = "kudos-sys"
        var dictType = "dict_type-11"
        val cacheItems = cacheHandler.getDictItems(atomicServiceCode, dictType)

        // 插入新的记录到数据库
        val sysDictItemNew = insertNewRecordToDb("78139ed2-dbce-47fa-ac0d-111111118149")

        // 更新数据库的记录
        val idUpdate = "8aabaa7f-6d19-4d8a-8aed-a9f8ca558149"
        sysDictItemDao.updateProperties(idUpdate, mapOf(SysDictItem::itemName.name to newName))

        // 从数据库中删除记录
        val idDelete = "d2e7c962-d0ca-43a5-b722-e1878dfa8149"
        sysDictItemDao.deleteById(idDelete)

        // 重载缓存，但不清除旧缓存
        cacheHandler.reloadAll(false)

        // 原来缓存中的记录内存地址会变
        val cacheItemsNew = cacheHandler.getDictItems(atomicServiceCode, dictType)
        assert(cacheItems.first() !== cacheItemsNew.first())

        // 数据库中新增的记录在缓存应该要存在
        assertEquals(cacheItems.size + 1, cacheItemsNew.size)
        assert(cacheItemsNew.any { it.id == sysDictItemNew.id })

        // 数据库中更新的记录在缓存中应该也更新了
        dictType = "dict_type-22"
        val cacheItemsUpdate = cacheHandler.getDictItems(atomicServiceCode, dictType)
        val name = cacheItemsUpdate.first { it.id == idUpdate }.itemName
        assertEquals(newName, name)

        // 数据库中删除的记录在缓存中应该不存在
        dictType = "dict_type-33"
        val cacheItemsDelete = cacheHandler.getDictItems(atomicServiceCode, dictType)
        assertFalse(cacheItemsDelete.any { it.id == idDelete })
    }

    @Test
    fun getDictItems() {
        var atomicServiceCode = "kudos-sys"
        var dictType = "dict_type-11"
        val cacheItems = cacheHandler.getDictItems(atomicServiceCode, dictType)
        assert(cacheItems.size >= 3)

        // active为false的dictItem应该没有在缓存中
        assertFalse(cacheItems.any { it.id == "c46091d2-945c-4440-b103-ac58a7ae8149" })
        
        // 只有dict，没有dictItem的，应该不会在缓存中
        atomicServiceCode = "kudos-user"
        dictType = "dict_type-44"
        assert(cacheHandler.getDictItems(atomicServiceCode, dictType).isEmpty())

        // active为false的dict，应该不会在缓存中
        dictType = "dict_type-55"
        assert(cacheHandler.getDictItems(atomicServiceCode, dictType).isEmpty())
    }

    @Test
    fun syncOnInsert() {
        // 插入新的记录到数据库
        val dictItem = insertNewRecordToDb("78139ed2-dbce-47fa-ac0d-666666668149")

        val dict = assertNotNull(dictByIdCache.getDictById(dictItem.dictId))

        // 同步缓存
        cacheHandler.syncOnInsert(dictItem, dictItem.id)

        // 验证新记录是否在缓存中
        val key = cacheHandler.getKey(dict.atomicServiceCode, dict.dictType)
        @Suppress("UNCHECKED_CAST") 
        val cacheItems = CacheKit.getValue(cacheHandler.cacheName(), key) as List<SysDictItemCacheItem>
        assert(cacheItems.any { it.id == dictItem.id })
        val atomicServiceCode = assertNotNull(dict.atomicServiceCode)
        val dictType = assertNotNull(dict.dictType)
        val cacheItems2 = cacheHandler.getDictItems(atomicServiceCode, dictType)
        assert(cacheItems.size == cacheItems2.size)
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val id = "8aabaa7f-6d19-4d8a-8aed-a9f8ca558149"
        val success = sysDictItemDao.updateProperties(id, mapOf(SysDictItem::itemName.name to newName))
        assert(success)

        val dictItem = assertNotNull(sysDictItemDao.get(id))
        val dict = assertNotNull(dictByIdCache.getDictById(dictItem.dictId))

        // 同步缓存
        cacheHandler.syncOnUpdate(dictItem, id)

        // 验证缓存中的记录
        val key = cacheHandler.getKey(dict.atomicServiceCode, dict.dictType)
        @Suppress("UNCHECKED_CAST")
        val cacheItems = CacheKit.getValue(cacheHandler.cacheName(), key) as List<SysDictItemCacheItem>
        assertEquals(newName, cacheItems.first { it.id == id }.itemName)
        val atomicServiceCode = assertNotNull(dict.atomicServiceCode)
        val dictType = assertNotNull(dict.dictType)
        val cacheItems2 = cacheHandler.getDictItems(atomicServiceCode, dictType)
        assertEquals(newName, cacheItems2.first { it.id == id }.itemName)
    }

    @Test
    fun syncOnUpdateActive() {
        // 由true更新为false
        var id = "e8ff3f9a-a57a-4183-953d-fe80c12f8149"
        var success = sysDictItemDao.updateProperties(id, mapOf(SysDictItem::active.name to false))
        assert(success)
        var dictItem = assertNotNull(sysDictItemDao.get(id))
        var dict = assertNotNull(dictByIdCache.getDictById(dictItem.dictId))
        cacheHandler.syncOnUpdateActive(id)
        var key = cacheHandler.getKey(dict.atomicServiceCode, dict.dictType)
        @Suppress("UNCHECKED_CAST")
        var cacheItems1 = CacheKit.getValue(cacheHandler.cacheName(), key) as List<SysDictItemCacheItem>
        assertFalse(cacheItems1.any { it.id == id })
        var atomicServiceCode = assertNotNull(dict.atomicServiceCode)
        var dictType = assertNotNull(dict.dictType)
        var cacheItems2 = cacheHandler.getDictItems(atomicServiceCode, dictType)
        assertFalse(cacheItems2.any { it.id == id })

        // 由false更新为true
        id = "c46091d2-945c-4440-b103-ac58a7ae8149"
        success = sysDictItemDao.updateProperties(id, mapOf(SysDictItem::active.name to true))
        assert(success)
        dictItem = assertNotNull(sysDictItemDao.get(id))
        dict = assertNotNull(dictByIdCache.getDictById(dictItem.dictId))
        cacheHandler.syncOnUpdateActive(id)
        key = cacheHandler.getKey(dict.atomicServiceCode, dict.dictType)
        @Suppress("UNCHECKED_CAST")
        cacheItems1 = CacheKit.getValue(cacheHandler.cacheName(), key) as List<SysDictItemCacheItem>
        assert(cacheItems1.any { it.id == id })
        atomicServiceCode = assertNotNull(dict.atomicServiceCode)
        dictType = assertNotNull(dict.dictType)
        cacheItems2 = cacheHandler.getDictItems(atomicServiceCode, dictType)
        assert(cacheItems2.any { it.id == id })
    }

    @Test
    fun syncOnDelete() {
        val id = "04626227-0ac0-49a2-8036-241cd0178149"
        val dictItem = assertNotNull(sysDictItemDao.get(id))
        val dict = assertNotNull(dictByIdCache.getDictById(dictItem.dictId))

        // 删除数据库中的记录
        val deleteSuccess = sysDictItemDao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(id, dictItem.dictId)

        // 验证缓存中有没有
        val key = cacheHandler.getKey(dict.atomicServiceCode, dict.dictType)
        @Suppress("UNCHECKED_CAST")
        val cacheItems1 = CacheKit.getValue(cacheHandler.cacheName(), key) as List<SysDictItemCacheItem>?
        assert(cacheItems1 == null || !cacheItems1.any { it.id == id })
        val atomicServiceCode = assertNotNull(dict.atomicServiceCode)
        val dictType = assertNotNull(dict.dictType)
        val cacheItems2 = cacheHandler.getDictItems(atomicServiceCode, dictType)
        assertFalse(cacheItems2.any { it.id == id })
    }
    
    private fun insertNewRecordToDb(dictId: String): SysDictItem {
        val dictItem = SysDictItem().apply {
            itemCode = "a_new_item_code"
            itemName = "a_new_item_name"
            this.dictId = dictId
            orderNum = 5
        }
        sysDictItemDao.insert(dictItem)
        return dictItem
    }

}