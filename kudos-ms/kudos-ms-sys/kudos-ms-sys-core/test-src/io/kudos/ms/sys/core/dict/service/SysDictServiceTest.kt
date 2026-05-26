package io.kudos.ms.sys.core.dict.service

import io.kudos.ms.sys.common.dict.vo.SysDictCacheEntry
import io.kudos.ms.sys.core.dict.cache.SysDictHashCache
import io.kudos.ms.sys.core.dict.service.iservice.ISysDictService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysDictService
 *
 * Test data source: `SysDictServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDictServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysDictService: ISysDictService

    @Resource
    private lateinit var sysDictHashCache: SysDictHashCache

    private val seededId = "20000000-0000-0000-0000-000000007514"
    private val atomicServiceCode = "svc-module-dict-test-1"
    private val dictType = "svc-dict-type-1"

    /** `get(id, SysDictCacheEntry::class)` and `getDictFromCache` are consistent after refreshing the Hash cache. */
    @Test
    fun get_withCacheEntryReturnType_delegatesToHashCache() {
        sysDictHashCache.reloadAll(clear = true)
        val fromGet = sysDictService.get(seededId, SysDictCacheEntry::class)
        val fromCache = sysDictService.getDictFromCache(seededId)
        assertNotNull(fromGet)
        assertNotNull(fromCache)
        assertEquals(fromCache.id, fromGet.id)
        assertEquals(seededId, fromGet.id)
    }

    /** Read cache entry from Hash by primary key. */
    @Test
    fun getDictFromCache_byId() {
        sysDictHashCache.reloadAll(clear = true)
        val item = sysDictService.getDictFromCache(seededId)
        assertNotNull(item)
        assertEquals(dictType, item.dictType)
    }

    /** Get the dictionary list from cache by atomic service code. */
    @Test
    fun getDictsFromCacheByAtomicServiceCode() {
        sysDictHashCache.reloadAll(clear = true)
        val dicts = sysDictService.getDictsFromCacheByAtomicServiceCode(atomicServiceCode)
        assertTrue(dicts.any { it.id == seededId })
    }

    /** Direct DB queries by list and by primary key return the same row. */
    @Test
    fun getDictByAtomicServiceAndType_and_getRecord() {
        val row = sysDictService.getDictByAtomicServiceAndType(atomicServiceCode, dictType)
        assertNotNull(row)
        assertEquals(seededId, row.id)

        val byId = sysDictService.getRecord(seededId)
        assertNotNull(byId)
        assertEquals(seededId, byId.id)
        assertEquals(dictType, byId.dictType)
    }

    /** After updating the active flag, the cache-side `active` matches the database. */
    @Test
    fun updateActive() {
        sysDictHashCache.reloadAll(clear = true)
        assertTrue(sysDictService.updateActive(seededId, false))
        sysDictHashCache.reloadAll(clear = true)
        assertFalse(requireNotNull(sysDictService.getDictFromCache(seededId)).active)

        assertTrue(sysDictService.updateActive(seededId, true))
        sysDictHashCache.reloadAll(clear = true)
        assertTrue(requireNotNull(sysDictService.getDictFromCache(seededId)).active)
    }

    /** `updateActive` returns false when the primary key does not exist. */
    @Test
    fun updateActive_whenIdNotExists_returnsFalse() {
        assertFalse(sysDictService.updateActive("00000000-0000-0000-0000-000000000001", true))
    }

    /** `deleteById` returns false when the primary key does not exist. */
    @Test
    fun deleteById_returnsFalseWhenRowMissing() {
        assertFalse(sysDictService.deleteById("00000000-0000-0000-0000-000000000001"))
    }

    /** With seed data containing no dictionary items, active item queries return empty lists (call chain still works). */
    @Test
    fun getActiveDictItemsFromCache_emptyWhenNoItems() {
        val items = sysDictService.getActiveDictItemsFromCache(dictType, atomicServiceCode)
        assertTrue(items.isEmpty())
        assertTrue(sysDictService.getActiveDictItemMapFromCache(dictType, atomicServiceCode).isEmpty())
    }
}
