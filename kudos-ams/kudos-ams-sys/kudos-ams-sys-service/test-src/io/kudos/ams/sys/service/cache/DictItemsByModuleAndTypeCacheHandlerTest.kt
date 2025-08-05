package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ams.sys.common.vo.dictitem.SysDictItemCacheItem
import io.kudos.ams.sys.service.dao.SysDictItemDao
import io.kudos.ams.sys.service.model.po.SysDictItem
import org.springframework.beans.factory.annotation.Autowired
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * junit test for DictItemsByModuleAndTypeCacheHandler
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerAvailable
class DictItemsByModuleAndTypeCacheHandlerTest : CacheHandlerTestBase() {
    
    @Autowired
    private lateinit var cacheHandler: DictItemsByModuleAndTypeCacheHandler
    
    @Autowired
    private lateinit var dictByIdCacheHandler: DictByIdCacheHandler
    
    @Autowired
    private lateinit var sysDictItemDao: SysDictItemDao
    
    private val newName = "newName"

    @Test
    fun reloadAll() {
        // 清除并重载缓存，保证与数据库中的数据一致
        cacheHandler.reloadAll(true)

        // 获取当前缓存中的记录
        val moduleCode = "kudos-sys"
        var dictType = "dict_type-11"
        val cacheItems = cacheHandler.getDictItems(moduleCode, dictType)

        // 插入新的记录到数据库
        val sysDictItemNew = insertNewRecordToDb("78139ed2-dbce-47fa-ac0d-111111111111")

        // 更新数据库的记录
        val idUpdate = "8aabaa7f-6d19-4d8a-8aed-a9f8ca553eee"
        sysDictItemDao.updateProperties(idUpdate, mapOf(SysDictItem::itemName.name to newName))

        // 从数据库中删除记录
        val idDelete = "d2e7c962-d0ca-43a5-b722-e1878dfa1555"
        sysDictItemDao.deleteById(idDelete)

        // 重载缓存，但不清除旧缓存
        cacheHandler.reloadAll(false)

        // 原来缓存中的记录内存地址会变
        val cacheItemsNew = cacheHandler.getDictItems(moduleCode, dictType)
        assert(cacheItems.first() !== cacheItemsNew.first())

        // 数据库中新增的记录在缓存应该要存在
        assertEquals(cacheItems.size + 1, cacheItemsNew.size)
        assert(cacheItemsNew.any { it.id == sysDictItemNew.id })

        // 数据库中更新的记录在缓存中应该也更新了
        dictType = "dict_type-22"
        val cacheItemsUpdate = cacheHandler.getDictItems(moduleCode, dictType)
        val name = cacheItemsUpdate.first { it.id == idUpdate }.itemName
        assertEquals(newName, name)

        // 数据库中删除的记录在缓存中应该不存在
        dictType = "dict_type-33"
        val cacheItemsDelete = cacheHandler.getDictItems(moduleCode, dictType)
        assertFalse(cacheItemsDelete.any { it.id == idDelete })
    }

    @Test
    fun getDictItems() {
        var moduleCode = "kudos-sys"
        var dictType = "dict_type-11"
        val cacheItems = cacheHandler.getDictItems(moduleCode, dictType)
        assert(cacheItems.size >= 3)

        // active为false的dictItem应该没有在缓存中
        assertFalse(cacheItems.any { it.id == "c46091d2-945c-4440-b103-ac58a7aec999" })
        
        // 只有dict，没有dictItem的，应该不会在缓存中
        moduleCode = "kudos-user"
        dictType = "dict_type-44"
        assert(cacheHandler.getDictItems(moduleCode, dictType).isEmpty())

        // active为false的dict，应该不会在缓存中
        dictType = "dict_type-55"
        assert(cacheHandler.getDictItems(moduleCode, dictType).isEmpty())
    }

    @Test
    fun syncOnInsert() {
        // 插入新的记录到数据库
        val dictItem = insertNewRecordToDb("78139ed2-dbce-47fa-ac0d-666666666666")

        val dict = dictByIdCacheHandler.getDictById(dictItem.dictId)!!

        // 同步缓存
        cacheHandler.syncOnInsert(dictItem, dictItem.id!!)

        // 验证新记录是否在缓存中
        val key = cacheHandler.getKey(dict.moduleCode, dict.dictType)
        @Suppress("UNCHECKED_CAST") 
        val cacheItems = CacheKit.getValue(cacheHandler.cacheName(), key) as List<SysDictItemCacheItem>
        assert(cacheItems.any { it.id == dictItem.id })
        val cacheItems2 = cacheHandler.getDictItems(dict.moduleCode!!, dict.dictType!!)
        assert(cacheItems.size == cacheItems2.size)
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val id = "8aabaa7f-6d19-4d8a-8aed-a9f8ca553eee"
        val success = sysDictItemDao.updateProperties(id, mapOf(SysDictItem::itemName.name to newName))
        assert(success)

        val dictItem = sysDictItemDao.get(id)!!
        val dict = dictByIdCacheHandler.getDictById(dictItem.dictId)!!

        // 同步缓存
        cacheHandler.syncOnUpdate(dictItem, id)

        // 验证缓存中的记录
        val key = cacheHandler.getKey(dict.moduleCode, dict.dictType)
        @Suppress("UNCHECKED_CAST")
        val cacheItems = CacheKit.getValue(cacheHandler.cacheName(), key) as List<SysDictItemCacheItem>
        assertEquals(newName, cacheItems.first { it.id == id }.itemName)
        val cacheItems2 = cacheHandler.getDictItems(dict.moduleCode!!, dict.dictType!!)
        assertEquals(newName, cacheItems2.first { it.id == id }.itemName)
    }

    @Test
    fun syncOnUpdateActive() {
        // 由true更新为false
        var id = "e8ff3f9a-a57a-4183-953d-fe80c12fc777"
        var success = sysDictItemDao.updateProperties(id, mapOf(SysDictItem::active.name to false))
        assert(success)
        var dictItem = sysDictItemDao.get(id)!!
        var dict = dictByIdCacheHandler.getDictById(dictItem.dictId)!!
        cacheHandler.syncOnUpdateActive(id)
        var key = cacheHandler.getKey(dict.moduleCode, dict.dictType)
        @Suppress("UNCHECKED_CAST")
        var cacheItems1 = CacheKit.getValue(cacheHandler.cacheName(), key) as List<SysDictItemCacheItem>
        assertFalse(cacheItems1.any { it.id == id })
        var cacheItems2 = cacheHandler.getDictItems(dict.moduleCode!!, dict.dictType!!)
        assertFalse(cacheItems2.any { it.id == id })

        // 由false更新为true
        id = "c46091d2-945c-4440-b103-ac58a7aec999"
        success = sysDictItemDao.updateProperties(id, mapOf(SysDictItem::active.name to true))
        assert(success)
        dictItem = sysDictItemDao.get(id)!!
        dict = dictByIdCacheHandler.getDictById(dictItem.dictId)!!
        cacheHandler.syncOnUpdateActive(id)
        key = cacheHandler.getKey(dict.moduleCode, dict.dictType)
        @Suppress("UNCHECKED_CAST")
        cacheItems1 = CacheKit.getValue(cacheHandler.cacheName(), key) as List<SysDictItemCacheItem>
        assert(cacheItems1.any { it.id == id })
        cacheItems2 = cacheHandler.getDictItems(dict.moduleCode!!, dict.dictType!!)
        assert(cacheItems2.any { it.id == id })
    }

    @Test
    fun syncOnDelete() {
        val id = "04626227-0ac0-49a2-8036-241cd017a666"
        val dictItem = sysDictItemDao.get(id)!!
        val dict = dictByIdCacheHandler.getDictById(dictItem.dictId)!!

        // 删除数据库中的记录
        val deleteSuccess = sysDictItemDao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(id, dictItem.dictId)

        // 验证缓存中有没有
        val key = cacheHandler.getKey(dict.moduleCode, dict.dictType)
        @Suppress("UNCHECKED_CAST")
        val cacheItems1 = CacheKit.getValue(cacheHandler.cacheName(), key) as List<SysDictItemCacheItem>?
        assert(cacheItems1 == null || !cacheItems1.any { it.id == id })
        val cacheItems2 = cacheHandler.getDictItems(dict.moduleCode!!, dict.dictType!!)
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