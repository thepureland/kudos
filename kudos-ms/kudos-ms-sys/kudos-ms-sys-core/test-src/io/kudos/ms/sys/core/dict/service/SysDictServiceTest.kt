package io.kudos.ms.sys.core.dict.service

import io.kudos.ms.sys.common.dict.vo.SysDictCacheEntry
import io.kudos.ms.sys.core.dict.cache.SysDictHashCache
import io.kudos.ms.sys.core.dict.model.po.SysDict
import io.kudos.ms.sys.core.dict.service.iservice.ISysDictService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

    private fun newDict(): SysDict {
        val unique = UUID.randomUUID().toString().take(8)
        return SysDict().apply {
            atomicServiceCode = "svc-module-dict-test-1"
            dictType = "svc-dict-type-new-$unique"
            dictName = "svc-dict-name-new-$unique"
            remark = "from SysDictServiceTest insert"
            active = true
            builtIn = false
        }
    }

    /** insert publishes an event and yields a readable record. */
    @Test
    fun insert_persistsAndReturnsId() {
        val dict = newDict()
        val id = sysDictService.insert(dict)
        assertTrue(id.isNotBlank())
        val record = sysDictService.getRecord(id)
        assertNotNull(record)
        assertEquals(dict.dictType, record.dictType)
        assertEquals(dict.atomicServiceCode, record.atomicServiceCode)
    }

    /** update(any) succeeds and the change is reflected in the record. */
    @Test
    fun update_modifiesExistingRow() {
        val id = sysDictService.insert(newDict())
        val po = SysDict().apply {
            this.id = id
            this.dictName = "svc-dict-name-updated"
        }
        assertTrue(sysDictService.update(po))
        assertEquals("svc-dict-name-updated", sysDictService.getRecord(id)?.dictName)
    }

    /** deleteById succeeds for an existing row and the record disappears. */
    @Test
    fun deleteById_removesExistingRow() {
        val id = sysDictService.insert(newDict())
        assertNotNull(sysDictService.getRecord(id))
        assertTrue(sysDictService.deleteById(id))
        assertNull(sysDictService.getRecord(id))
    }

    /** batchDelete removes multiple rows and reports the actual count. */
    @Test
    fun batchDelete_removesMultipleRows() {
        val id1 = sysDictService.insert(newDict())
        val id2 = sysDictService.insert(newDict())
        assertEquals(2, sysDictService.batchDelete(listOf(id1, id2)))
        assertNull(sysDictService.getRecord(id1))
        assertNull(sysDictService.getRecord(id2))
    }

    /** batchDelete with no matching rows returns 0 (and publishes no event). */
    @Test
    fun batchDelete_returnsZeroWhenNothingMatches() {
        assertEquals(0, sysDictService.batchDelete(listOf("00000000-0000-0000-0000-0000000000aa")))
    }

    // NOTE: delete(id, isDict=true) -> deleteDictWithItems -> SysDictDao.deleteDictItemsByDictId is currently
    // unreachable for success because that DAO method calls batchDeleteWhen without a searchPayload, which always
    // throws "Unconditional database table delete is not allowed!". See suspectedBugs. A test asserting the correct
    // (successful cascade) behavior would fail against the current main code, so it is intentionally omitted here.

    /** delete(id, isDict=false) routes to dictionary item cascade deletion; missing item returns false. */
    @Test
    fun delete_withIsDictFalse_cascadeDeletesItem() {
        assertFalse(sysDictService.delete("00000000-0000-0000-0000-0000000000bb", isDict = false))
    }

    /** getDictByAtomicServiceAndType returns null when no row matches. */
    @Test
    fun getDictByAtomicServiceAndType_nullWhenMissing() {
        assertNull(sysDictService.getDictByAtomicServiceAndType("no-such-svc", "no-such-type"))
    }

    /** getRecord returns null for a non-existent id. */
    @Test
    fun getRecord_nullWhenMissing() {
        assertNull(sysDictService.getRecord("00000000-0000-0000-0000-0000000000cc"))
    }

    /** id -> dictType mapping for a module; activeOnly filtering keeps the seeded active dict. */
    @Test
    fun getDictTypesByAtomicServiceCode() {
        sysDictHashCache.reloadAll(clear = true)
        val all = sysDictService.getDictTypesByAtomicServiceCode(atomicServiceCode, activeOnly = false)
        assertEquals(dictType, all[seededId])
        val activeOnly = sysDictService.getDictTypesByAtomicServiceCode(atomicServiceCode, activeOnly = true)
        assertEquals(dictType, activeOnly[seededId])
    }

    /** getDictsFromCacheByAtomicServiceCode with activeOnly=false still returns the seeded dict. */
    @Test
    fun getDictsFromCacheByAtomicServiceCode_includeInactive() {
        sysDictHashCache.reloadAll(clear = true)
        val dicts = sysDictService.getDictsFromCacheByAtomicServiceCode(atomicServiceCode, activeOnly = false)
        assertTrue(dicts.any { it.id == seededId })
    }

    /** Batch cache fetches flip the key order to (atomicServiceCode, dictType) and return empty item lists when none. */
    @Test
    fun batchGetActiveDictItems_keyOrderAndEmpty() {
        sysDictHashCache.reloadAll(clear = true)
        val pairs = listOf(dictType to atomicServiceCode)

        val items = sysDictService.batchGetActiveDictItemsFromCache(pairs)
        val key = atomicServiceCode to dictType
        assertTrue(items.containsKey(key), "result key should be flipped to (atomicServiceCode, dictType)")
        assertTrue(items.getValue(key).isEmpty())

        val maps = sysDictService.batchGetActiveDictItemMapFromCache(pairs)
        assertTrue(maps.containsKey(key))
        assertTrue(maps.getValue(key).isEmpty())
    }
}
