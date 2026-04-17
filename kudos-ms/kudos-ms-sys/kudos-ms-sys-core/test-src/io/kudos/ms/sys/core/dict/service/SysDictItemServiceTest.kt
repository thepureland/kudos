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
 * 测试数据来源：`SysDictItemServiceTest.sql`
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

    /** 按主键 `get(id)` 取实体。 */
    @Test
    fun get_byId_entity() {
        val row = sysDictItemService.get(seededParentItemId)
        assertNotNull(row)
        assertEquals(seededParentItemId, row.id)
    }

    /** `get(id, SysDictItemCacheEntry::class)` 与 `getDictItemFromCache` 在刷新 Hash 后一致。 */
    @Test
    fun get_withCacheEntryReturnType_delegatesToHashCache() {
        sysDictItemHashCache.reloadAll(clear = true)
        val fromGet = sysDictItemService.get(seededParentItemId, SysDictItemCacheEntry::class)
        val fromCache = sysDictItemService.getDictItemFromCache(seededParentItemId)
        assertNotNull(fromGet)
        assertNotNull(fromCache)
        assertEquals(fromCache.id, fromGet.id)
    }

    /** 按类型 + 模块编码加载缓存列表。 */
    @Test
    fun getDictItemsFromCache() {
        sysDictItemHashCache.reloadAll(clear = true)
        val items = sysDictItemService.getDictItemsFromCache(dictType, atomicServiceCode)
        assertEquals(2, items.size)
        assertTrue(items.any { it.id == seededParentItemId })
    }

    /** 编码 → 名称映射。 */
    @Test
    fun getDictItemMapFromCache() {
        sysDictItemHashCache.reloadAll(clear = true)
        val map = sysDictItemService.getDictItemMapFromCache(dictType, atomicServiceCode)
        assertEquals("svc-item-name-1", map[parentItemCode])
    }

    /** 译码。 */
    @Test
    fun transDictItemNameFromCache() {
        sysDictItemHashCache.reloadAll(clear = true)
        assertEquals(
            "svc-item-name-2",
            sysDictItemService.transDictItemNameFromCache(dictType, childItemCode, atomicServiceCode)
        )
    }

    /** 字典类型下第一层结点。 */
    @Test
    fun getDirectChildrenOfDictFromCache() {
        sysDictItemHashCache.reloadAll(clear = true)
        val roots = sysDictItemService.getDirectChildrenOfDictFromCache(atomicServiceCode, dictType)
        assertTrue(roots.any { it.id == seededParentItemId })
    }

    /** 某字典项下的直接子项。 */
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

    /** 祖先链。 */
    @Test
    fun fetchAllParentIds() {
        assertTrue(sysDictItemService.fetchAllParentIds(seededChildItemId).contains(seededParentItemId))
    }

    /** 按主键取父 id 下子项列表。 */
    @Test
    fun getDirectChildrenOfItemFromCache_byParentId() {
        sysDictItemHashCache.reloadAll(clear = true)
        val children = sysDictItemService.getDirectChildrenOfItemFromCache(seededParentItemId)
        assertTrue(children.any { it.id == seededChildItemId })
    }

    /** 批量按模块取缓存列表。 */
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

    /** 启用状态更新后缓存一致。 */
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

    /** 主键不存在时 `updateActive` 返回 false。 */
    @Test
    fun updateActive_whenIdNotExists_returnsFalse() {
        assertFalse(sysDictItemService.updateActive("00000000-0000-0000-0000-000000000001", true))
    }

    /** 主键不存在时 `deleteById` 返回 false。 */
    @Test
    fun deleteById_returnsFalseWhenRowMissing() {
        assertFalse(sysDictItemService.deleteById("00000000-0000-0000-0000-000000000001"))
    }
}
