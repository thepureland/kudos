package io.kudos.ms.sys.core.dict.cache

import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ms.sys.core.dict.dao.SysDictItemDao
import io.kudos.ms.sys.core.dict.dao.VSysDictItemDao
import io.kudos.ms.sys.core.dict.event.SysDictItemBatchDeleted
import io.kudos.ms.sys.core.dict.event.SysDictItemDeleted
import io.kudos.ms.sys.core.dict.event.SysDictItemInserted
import io.kudos.ms.sys.core.dict.event.SysDictItemUpdated
import io.kudos.ms.sys.core.dict.model.po.SysDictItem
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.*

/**
 * Tests for SysDictItemHashCache.
 *
 * Test data source: `sql/h2/dict/cache/SysDictItemHashCacheTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDictItemHashCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cache: SysDictItemHashCache

    @Resource
    private lateinit var vSysDictItemDao: VSysDictItemDao

    @Resource
    private lateinit var sysDictItemDao: SysDictItemDao

    override fun getTestDataSqlPath(): String = "sql/h2/dict/cache/SysDictItemHashCacheTest.sql"

    private fun isLocalCacheEnabled(): Boolean = HashCacheKit.isLocalCacheEnabled(SysDictItemHashCache.CACHE_NAME)

    // ---------- By primary key id ----------

    @Test
    fun getDictItemById() {
        cache.reloadAll(true)
        val id = "sdih-i001-1a2b-4c5d-8e9f-000001"
        val item = cache.getDictItemById(id)
        assertNotNull(item)
        assertEquals(id, item.id.trim())
        assertEquals("sdih-ms-7f3e2d1c", item.atomicServiceCode.trim())
        assertEquals("sdih-type-001", item.dictType)
        assertEquals("sdih-code-001", item.itemCode)
        val itemAgain = cache.getDictItemById(id)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain, "Fetching the same id from cache again should return the same object reference")
        assertNull(cache.getDictItemById("sdih-nonexistent-00000000000000000000"))
    }

    @Test
    fun getDictItemsByIds() {
        cache.reloadAll(true)
        val ids = setOf(
            "sdih-i001-1a2b-4c5d-8e9f-000001",
            "sdih-i002-2b3c-5d6e-9f0a-000002",
            "sdih-i003-3c4d-6e7f-0a1b-000003"
        )
        val map = cache.getDictItemsByIds(ids)
        assertEquals(3, map.size)
        map.forEach { (k, v) -> assertEquals(k, v.id.trim()) }
        val mapAgain = cache.getDictItemsByIds(ids)
        if (isLocalCacheEnabled()) ids.forEach { id ->
            assertSame(map[id], mapAgain[id], "Fetching the same id from cache again should return the same object reference")
        }
        assertTrue(cache.getDictItemsByIds(emptySet()).isEmpty())
    }

    // ---------- By atomicServiceCode + dictType + itemCode ----------

    @Test
    fun getDictItem() {
        val atomicServiceCode = "sdih-ms-a1b2c3d4"
        val dictType = "sdih-type-a1"
        val itemCode = "sdih-code-p01"
        val idFromDao = vSysDictItemDao.fetchByAtomicServiceCodeAndDictTypeAndItemCode(
            atomicServiceCode, dictType, itemCode
        )?.id
        assertNotNull(idFromDao, "Test data not loaded: expected a row with atomic_service_code=$atomicServiceCode, dict_type=$dictType, item_code=$itemCode, active=true")
        assertEquals("sdih-ia1-4d5e-7f8a-1b2c-000011", idFromDao.trim())
        val res1 = cache.getDictItem(atomicServiceCode, dictType, itemCode)
        assertEquals("sdih-ia1-4d5e-7f8a-1b2c-000011", res1?.id?.trim())
        val res2 = cache.getDictItem(atomicServiceCode, dictType, itemCode)
        if (isLocalCacheEnabled()) assertSame(res1, res2, "When local cache is enabled, fetching the same dimension again should return the same object reference")
    }

    // ---------- By atomicServiceCode + dictType ----------

    @Test
    fun getDictItems() {
        cache.reloadAll(true)
        val atomicServiceCode = "sdih-ms-a1b2c3d4"
        val dictType = "sdih-type-a1"
        val list = cache.getDictItems(atomicServiceCode, dictType)
        assertEquals(2, list.size)
        assertTrue(list.all { it.atomicServiceCode.trim() == atomicServiceCode && it.dictType.trim() == dictType })
        val listAgain = cache.getDictItems(atomicServiceCode, dictType)
        if (isLocalCacheEnabled()) assertSame(list.first(), listAgain.first(), "When local cache is enabled, fetching the same dimension again should return the same object reference")
        assertTrue(cache.getDictItems("sdih-ms-nonexistent", "sdih-type-x").isEmpty())
    }

    // ---------- By parentId ----------

    @Test
    fun getDictItems_byParentId_emptyWhenNoChildren() {
        cache.reloadAll(true)
        // seeded items have no parentId, so querying any id as parent yields an empty child list
        val children = cache.getDictItems("sdih-i001-1a2b-4c5d-8e9f-000001")
        assertTrue(children.isEmpty())
    }

    @Test
    fun getDictItems_byParentId_blankRejected() {
        assertFailsWith<IllegalArgumentException> { cache.getDictItems("   ") }
    }

    @Test
    fun getDictItemById_blankRejected() {
        assertFailsWith<IllegalArgumentException> { cache.getDictItemById("") }
    }

    // ---------- Full reload ----------

    @Test
    fun reloadAll() {
        cache.reloadAll(true)
        val id = cache.getDictItem("sdih-ms-a1b2c3d4", "sdih-type-a1", "sdih-code-p01")?.id
        assertNotNull(id)
        val newItem = insertNewDictItem()
        cache.reloadAll(false)
        assertNotNull(cache.getDictItems("sdih-ms-a1b2c3d4", "sdih-type-a1"))
        assertNotNull(cache.getDictItemById(newItem.id.trim()))
        cache.reloadAll(true)
        val item = cache.getDictItemById("sdih-i001-1a2b-4c5d-8e9f-000001")
        assertNotNull(item)
        val itemAgain = cache.getDictItemById("sdih-i001-1a2b-4c5d-8e9f-000001")
        if (isLocalCacheEnabled()) assertSame(item, itemAgain, "Fetching the same id from cache again should return the same object reference")
    }

    // ---------- Sync ----------

    @Test
    fun syncOnInsert() {
        cache.reloadAll(true)
        val newItem = insertNewDictItem()
        cache.syncOnInsert(newItem.id.trim())
        val item = cache.getDictItemById(newItem.id.trim())
        assertNotNull(item)
        val itemAgain = cache.getDictItemById(newItem.id.trim())
        if (isLocalCacheEnabled()) assertSame(item, itemAgain, "Fetching the same id from cache again should return the same object reference")
        assertNotNull(cache.getDictItem(
            "sdih-ms-a1b2c3d4", "sdih-type-a1", newItem.itemCode.trim()
        ))
    }

    @Test
    fun syncOnInsertWithAny() {
        cache.reloadAll(true)
        val newItem = insertNewDictItem()
        cache.syncOnInsert(Any(), newItem.id.trim())
        val item = cache.getDictItemById(newItem.id.trim())
        assertNotNull(item)
        val itemAgain = cache.getDictItemById(newItem.id.trim())
        if (isLocalCacheEnabled()) assertTrue(item === itemAgain, "Fetching the same id from cache again should return the same object reference")
    }

    @Test
    fun syncOnUpdate() {
        cache.reloadAll(true)
        val id = "sdih-iupd-6c7d-0e1f-000000000031"
        val newItemName = "sdih-item-name-upd-new"
        val entity = assertNotNull(sysDictItemDao.get(id))
        entity.itemName = newItemName
        sysDictItemDao.update(entity)
        cache.syncOnUpdate(id)
        val updated = cache.getDictItemById(id)
        assertNotNull(updated)
        assertEquals(newItemName, updated.itemName.trim())
        val updatedAgain = cache.getDictItemById(id)
        if (isLocalCacheEnabled()) assertSame(updated, updatedAgain, "Fetching the same id from cache again should return the same object reference")
    }

    @Test
    fun syncOnUpdateWithOldParams() {
        cache.reloadAll(true)
        val id = "sdih-iupd-6c7d-0e1f-000000000031"
        sysDictItemDao.updateProperties(id, mapOf(SysDictItem::itemName.name to "sdih-item-name-upd-v2"))
        cache.syncOnUpdate(Any(), id, "sdih-ms-sync-upd", "sdih-type-sync-1", "sdih-code-upd")
        val item = cache.getDictItemById(id)
        assertNotNull(item)
        val itemAgain = cache.getDictItemById(id)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain, "Fetching the same id from cache again should return the same object reference")
    }

    @Test
    fun syncOnDelete() {
        cache.reloadAll(true)
        val id = "sdih-idel-8e9f-2a3b-000000000033"
        val item = assertNotNull(cache.getDictItemById(id))
        sysDictItemDao.deleteById(id)
        cache.syncOnDelete(id, item.atomicServiceCode.trim() ?: "", item.dictType.trim(), item.itemCode.trim())
        assertNull(vSysDictItemDao.get(id), "After delete + sync, the id should no longer exist in the view")
    }

    @Test
    fun syncOnBatchDelete() {
        cache.reloadAll(true)
        val ids = listOf(
            "sdih-ib1-2b3c-5d6e-9f0a-000061",
            "sdih-ib2-3c4d-6e7f-0a1b-000062"
        )
        ids.forEach { sysDictItemDao.deleteById(it) }
        cache.syncOnBatchDelete(ids)
        ids.forEach { assertNull(vSysDictItemDao.get(it), "After batch delete + sync, the id should no longer exist in the view") }
    }

    // ---------- key helpers ----------

    @Test
    fun getKeyAtomicServiceCodeAndDictTypeAndItemCode() {
        val key = cache.getKeyAtomicServiceCodeAndDictTypeAndItemCode("sdih-ms-a", "type-1", "code-x")
        assertTrue(key.contains("sdih-ms-a"))
        assertTrue(key.contains("type-1"))
        assertTrue(key.contains("code-x"))
    }

    @Test
    fun getKeyAtomicServiceCodeAndDictType() {
        val key = cache.getKeyAtomicServiceCodeAndDictType("sdih-ms-a", "type-1")
        assertTrue(key.contains("sdih-ms-a"))
        assertTrue(key.contains("type-1"))
    }

    // ---------- transactional event listeners (delegate to syncOn*) ----------

    @Test
    fun onEvents_delegateToSync() {
        cache.reloadAll(true)
        val newItem = insertNewDictItem()
        // inserted -> syncOnInsert
        cache.on(SysDictItemInserted(id = newItem.id.trim()))
        assertNotNull(cache.getDictItemById(newItem.id.trim()))

        // updated -> syncOnUpdate
        sysDictItemDao.updateProperties(newItem.id.trim(), mapOf(SysDictItem::itemName.name to "sdih-on-updated"))
        cache.on(SysDictItemUpdated(id = newItem.id.trim()))
        assertEquals("sdih-on-updated", cache.getDictItemById(newItem.id.trim())?.itemName?.trim())

        // deleted -> syncOnDelete (carries dimensions)
        val entry = assertNotNull(cache.getDictItemById(newItem.id.trim()))
        sysDictItemDao.deleteById(newItem.id.trim())
        cache.on(
            SysDictItemDeleted(
                id = newItem.id.trim(),
                atomicServiceCode = entry.atomicServiceCode.trim(),
                dictType = entry.dictType.trim(),
                itemCode = entry.itemCode.trim(),
            )
        )
        assertNull(vSysDictItemDao.get(newItem.id.trim()))

        // batch deleted -> syncOnBatchDelete
        val item2 = insertNewDictItem()
        sysDictItemDao.deleteById(item2.id.trim())
        cache.on(SysDictItemBatchDeleted(ids = listOf(item2.id.trim())))
        assertNull(vSysDictItemDao.get(item2.id.trim()))
    }

    private fun insertNewDictItem(): SysDictItem {
        val dictId = "sdih-da1-a1b2c3d4-000000000011"
        val itemCode = "sdih-code-new-${System.currentTimeMillis()}"
        val item = SysDictItem().apply {
            this.dictId = dictId
            this.itemCode = itemCode
            itemName = "sdih-item-name-new"
            orderNum = 99
        }
        sysDictItemDao.insert(item)
        return item
    }
}
