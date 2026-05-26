package io.kudos.ms.sys.core.dict.service

import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.core.dict.cache.SysDictItemHashCache
import io.kudos.ms.sys.core.dict.service.iservice.ISysDictItemService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysDictItemService
 *
 * Test data source: `SysDictItemServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDictItemServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysDictItemService: ISysDictItemService

    @Resource
    private lateinit var sysDictItemHashCache: SysDictItemHashCache

    private val seededParentItemId = "20000000-0000-0000-0000-000000004968"
    private val seededChildItemId = "20000000-0000-0000-0000-000000004969"
    private val atomicServiceCode = "svc-module-dictitem-test-1"
    private val dictType = "svc-dict-type-item-1"
    private val parentItemCode = "svc-item-code-1_8400"
    private val childItemCode = "svc-item-code-2_8400"

    /** Fetch the entity by primary key via `get(id)`. */
    @Test
    fun get_byId_entity() {
        val row = sysDictItemService.get(seededParentItemId)
        assertNotNull(row)
        assertEquals(seededParentItemId, row.id)
    }

    /** `get(id, SysDictItemCacheEntry::class)` and `getDictItemFromCache` are consistent after refreshing the Hash cache. */
    @Test
    fun get_withCacheEntryReturnType_delegatesToHashCache() {
        sysDictItemHashCache.reloadAll(clear = true)
        val fromGet = sysDictItemService.get(seededParentItemId, SysDictItemCacheEntry::class)
        val fromCache = sysDictItemService.getDictItemFromCache(seededParentItemId)
        assertNotNull(fromGet)
        assertNotNull(fromCache)
        assertEquals(fromCache.id, fromGet.id)
    }

    /** Load the cached list by dictionary type and module (atomic service) code. */
    @Test
    fun getDictItemsFromCache() {
        sysDictItemHashCache.reloadAll(clear = true)
        val items = sysDictItemService.getDictItemsFromCache(dictType, atomicServiceCode)
        assertEquals(2, items.size)
        assertTrue(items.any { it.id == seededParentItemId })
    }

    /** Code -> name mapping. */
    @Test
    fun getDictItemMapFromCache() {
        sysDictItemHashCache.reloadAll(clear = true)
        val map = sysDictItemService.getDictItemMapFromCache(dictType, atomicServiceCode)
        assertEquals("svc-item-name-1", map[parentItemCode])
    }

    /** Decode item name from item code. */
    @Test
    fun transDictItemNameFromCache() {
        sysDictItemHashCache.reloadAll(clear = true)
        assertEquals(
            "svc-item-name-2",
            sysDictItemService.transDictItemNameFromCache(dictType, childItemCode, atomicServiceCode)
        )
    }

    /** First-level nodes under a dictionary type. */
    @Test
    fun getDirectChildrenOfDictFromCache() {
        sysDictItemHashCache.reloadAll(clear = true)
        val roots = sysDictItemService.getDirectChildrenOfDictFromCache(atomicServiceCode, dictType)
        assertTrue(roots.any { it.id == seededParentItemId })
    }

    /** Direct children under a given dictionary item. */
    @Test
    fun getDirectChildrenOfItemFromCache() {
        sysDictItemHashCache.reloadAll(clear = true)
        val children = sysDictItemService.getDirectChildrenOfItemFromCache(
            atomicServiceCode,
            dictType,
            parentItemCode
        )
        assertTrue(children.any { it.id == seededChildItemId })
    }

    /** Ancestor chain. */
    @Test
    fun fetchAllParentIds() {
        assertTrue(sysDictItemService.fetchAllParentIds(seededChildItemId).contains(seededParentItemId))
    }

    /** Fetch the list of children under a parent id by primary key. */
    @Test
    fun getDirectChildrenOfItemFromCache_byParentId() {
        sysDictItemHashCache.reloadAll(clear = true)
        val children = sysDictItemService.getDirectChildrenOfItemFromCache(seededParentItemId)
        assertTrue(children.any { it.id == seededChildItemId })
    }

    /** Batch fetch cache list by module. */
    @Test
    fun batchGetDictItemsFromCache() {
        sysDictItemHashCache.reloadAll(clear = true)
        val batch = sysDictItemService.batchGetDictItemsFromCache(
            mapOf(atomicServiceCode to listOf(dictType))
        )
        val byAtomic = requireNotNull(batch[atomicServiceCode]) { "batch missing atomicServiceCode" }
        val items = requireNotNull(byAtomic[dictType]) { "batch missing dictType" }
        assertEquals(2, items.size)
    }

    /** After updating the active state, the cache stays consistent. */
    @Test
    fun updateActive() {
        sysDictItemHashCache.reloadAll(clear = true)
        assertTrue(sysDictItemService.updateActive(seededParentItemId, false))
        sysDictItemHashCache.reloadAll(clear = true)
        assertFalse(requireNotNull(sysDictItemService.getDictItemFromCache(seededParentItemId)).active)

        assertTrue(sysDictItemService.updateActive(seededParentItemId, true))
        sysDictItemHashCache.reloadAll(clear = true)
        assertTrue(requireNotNull(sysDictItemService.getDictItemFromCache(seededParentItemId)).active)
    }

    /** `updateActive` returns false when the primary key does not exist. */
    @Test
    fun updateActive_whenIdNotExists_returnsFalse() {
        assertFalse(sysDictItemService.updateActive("00000000-0000-0000-0000-000000000001", true))
    }

    /** `deleteById` returns false when the primary key does not exist. */
    @Test
    fun deleteById_returnsFalseWhenRowMissing() {
        assertFalse(sysDictItemService.deleteById("00000000-0000-0000-0000-000000000001"))
    }
}
