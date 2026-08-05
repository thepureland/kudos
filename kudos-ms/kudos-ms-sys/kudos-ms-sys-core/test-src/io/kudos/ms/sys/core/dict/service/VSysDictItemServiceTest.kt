package io.kudos.ms.sys.core.dict.service

import io.kudos.ms.sys.common.dict.vo.request.SysDictItemQuery
import io.kudos.ms.sys.core.dict.cache.SysDictItemHashCache
import io.kudos.ms.sys.core.dict.service.iservice.IVSysDictItemService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * junit test for VSysDictItemService (read-only view service over v_sys_dict_item).
 *
 * Data source: `SysDictItemServiceTest.sql` (a parent item and a child item with parentId set), so the
 * parentId -> parentCode back-fill branch in [VSysDictItemService.pagingSearchWithParentCode] is exercised.
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class VSysDictItemServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var service: IVSysDictItemService

    @Resource
    private lateinit var sysDictItemHashCache: SysDictItemHashCache

    override fun getTestDataSqlPath(): String = "sql/h2/dict/service/SysDictItemServiceTest.sql"

    private val seededParentItemId = "20000000-0000-0000-0000-000000004968"
    private val seededChildItemId = "20000000-0000-0000-0000-000000004969"
    private val atomicServiceCode = "svc-module-dictitem-test-1"
    private val dictType = "svc-dict-type-item-1"
    private val parentItemCode = "svc-item-code-1_8400"
    private val childItemCode = "svc-item-code-2_8400"

    @Test
    fun getFromCache_byId() {
        sysDictItemHashCache.reloadAll(clear = true)
        val entry = service.getFromCache(seededParentItemId)
        assertNotNull(entry)
        assertEquals(seededParentItemId, entry.id)
    }

    @Test
    fun getFromCache_nullWhenMissing() {
        sysDictItemHashCache.reloadAll(clear = true)
        assertNull(service.getFromCache("00000000-0000-0000-0000-0000000000c1"))
    }

    @Test
    fun fetchByAtomicServiceCodeAndDictTypeAndItemCode() {
        sysDictItemHashCache.reloadAll(clear = true)
        val entry = service.fetchByAtomicServiceCodeAndDictTypeAndItemCode(atomicServiceCode, dictType, parentItemCode)
        assertNotNull(entry)
        assertEquals(seededParentItemId, entry.id)
    }

    @Test
    fun searchByAtomicServiceCodeAndDictType() {
        sysDictItemHashCache.reloadAll(clear = true)
        val list = service.searchByAtomicServiceCodeAndDictType(atomicServiceCode, dictType)
        assertEquals(2, list.size)
    }

    @Test
    fun searchByParentId() {
        sysDictItemHashCache.reloadAll(clear = true)
        val children = service.searchByParentId(seededParentItemId)
        assertTrue(children.any { it.id == seededChildItemId })
    }

    @Test
    fun searchByIds() {
        sysDictItemHashCache.reloadAll(clear = true)
        val map = service.searchByIds(setOf(seededParentItemId, seededChildItemId))
        assertEquals(2, map.size)
        assertEquals(parentItemCode, map[seededParentItemId]?.itemCode)
    }

    @Test
    fun searchByIds_emptyInput() {
        assertTrue(service.searchByIds(emptySet()).isEmpty())
    }

    /** Paging query back-fills parentCode for rows that have a parentId. */
    @Test
    fun pagingSearchWithParentCode_backfillsParentCode() {
        sysDictItemHashCache.reloadAll(clear = true)
        val result = service.pagingSearchWithParentCode(
            SysDictItemQuery(dictId = seededParentItemId)
        )
        val childRow = result.data.firstOrNull { it.id == seededChildItemId }
        assertNotNull(childRow, "child row should be returned")
        assertEquals(parentItemCode, childRow.parentCode, "child's parentCode should be back-filled from parentId")
        // root row has no parent -> parentCode stays null
        val parentRow = result.data.firstOrNull { it.id == seededParentItemId }
        assertNotNull(parentRow)
        assertNull(parentRow.parentCode)
    }

    /** Paging query with no parentId rows: parent back-fill block is skipped, query still returns rows. */
    @Test
    fun pagingSearchWithParentCode_noParentRows() {
        sysDictItemHashCache.reloadAll(clear = true)
        // query only the root item code -> the single returned row has no parentId
        val result = service.pagingSearchWithParentCode(
            SysDictItemQuery(itemCode = parentItemCode)
        )
        assertTrue(result.data.all { it.parentId == null })
    }
}
