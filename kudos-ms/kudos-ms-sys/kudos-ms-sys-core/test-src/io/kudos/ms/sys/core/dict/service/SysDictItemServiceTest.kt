package io.kudos.ms.sys.core.dict.service

import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.core.dict.cache.SysDictItemHashCache
import io.kudos.ms.sys.core.dict.model.po.SysDictItem
import io.kudos.ms.sys.core.dict.service.iservice.ISysDictItemService
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

    private val seededDictId = "20000000-0000-0000-0000-000000004968"

    private fun newItem(parentId: String? = null): SysDictItem {
        val unique = UUID.randomUUID().toString().take(8)
        return SysDictItem().apply {
            this.dictId = seededDictId
            this.itemCode = "svc-item-code-new-$unique"
            this.itemName = "svc-item-name-new-$unique"
            this.orderNum = 10
            this.parentId = parentId
            this.remark = "from SysDictItemServiceTest insert"
            this.active = true
            this.builtIn = false
        }
    }

    /** insert persists the item and the entity is readable by id. */
    @Test
    fun insert_persistsAndReturnsId() {
        val item = newItem()
        val id = sysDictItemService.insert(item)
        assertTrue(id.isNotBlank())
        val po = sysDictItemService.get(id)
        assertNotNull(po)
        assertEquals(item.itemCode, po.itemCode)
    }

    /** update(any) modifies an existing item. */
    @Test
    fun update_modifiesExistingRow() {
        val id = sysDictItemService.insert(newItem())
        val po = SysDictItem().apply {
            this.id = id
            this.itemName = "svc-item-name-updated"
        }
        assertTrue(sysDictItemService.update(po))
        assertEquals("svc-item-name-updated", sysDictItemService.get(id)?.itemName)
    }

    /** deleteById succeeds for an existing item. */
    @Test
    fun deleteById_removesExistingRow() {
        val id = sysDictItemService.insert(newItem())
        assertTrue(sysDictItemService.deleteById(id))
        assertNull(sysDictItemService.get(id))
    }

    /** batchDelete removes multiple items and returns the actual count. */
    @Test
    fun batchDelete_removesMultipleRows() {
        val id1 = sysDictItemService.insert(newItem())
        val id2 = sysDictItemService.insert(newItem())
        assertEquals(2, sysDictItemService.batchDelete(listOf(id1, id2)))
        assertNull(sysDictItemService.get(id1))
        assertNull(sysDictItemService.get(id2))
    }

    /** batchDelete returns 0 (and publishes no event) when nothing matches. */
    @Test
    fun batchDelete_returnsZeroWhenNothingMatches() {
        assertEquals(0, sysDictItemService.batchDelete(listOf("00000000-0000-0000-0000-0000000000dd")))
    }

    /** moveItem updates parent and order number. */
    @Test
    fun moveItem_updatesParentAndOrder() {
        val parent = sysDictItemService.insert(newItem())
        val child = sysDictItemService.insert(newItem())
        assertTrue(sysDictItemService.moveItem(child, parent, 5))
        val po = assertNotNull(sysDictItemService.get(child))
        assertEquals(parent, po.parentId)
        assertEquals(5, po.orderNum)
    }

    /** moveItem returns false when the target item does not exist. */
    @Test
    fun moveItem_returnsFalseWhenRowMissing() {
        assertFalse(sysDictItemService.moveItem("00000000-0000-0000-0000-0000000000ee", null, 1))
    }

    /** cascadeDeleteChildren removes the node together with its descendant subtree. */
    @Test
    fun cascadeDeleteChildren_removesSubtree() {
        val root = sysDictItemService.insert(newItem())
        val child = sysDictItemService.insert(newItem(parentId = root))
        val grandChild = sysDictItemService.insert(newItem(parentId = child))

        assertTrue(sysDictItemService.cascadeDeleteChildren(root))
        assertNull(sysDictItemService.get(root))
        assertNull(sysDictItemService.get(child))
        assertNull(sysDictItemService.get(grandChild))
    }

    /** cascadeDeleteChildren on a leaf (no children) still deletes the node itself. */
    @Test
    fun cascadeDeleteChildren_leafNode() {
        val leaf = sysDictItemService.insert(newItem())
        assertTrue(sysDictItemService.cascadeDeleteChildren(leaf))
        assertNull(sysDictItemService.get(leaf))
    }

    /** fetchAllParentIds returns empty for a root item (no parent). */
    @Test
    fun fetchAllParentIds_emptyForRoot() {
        assertTrue(sysDictItemService.fetchAllParentIds(seededParentItemId).isEmpty())
    }

    /** batchGetDictItemMapFromCache returns nested code->name maps per module/type. */
    @Test
    fun batchGetDictItemMapFromCache() {
        sysDictItemHashCache.reloadAll(clear = true)
        val batch = sysDictItemService.batchGetDictItemMapFromCache(
            mapOf(atomicServiceCode to listOf(dictType))
        )
        val byAtomic = requireNotNull(batch[atomicServiceCode]) { "batch missing atomicServiceCode" }
        val map = requireNotNull(byAtomic[dictType]) { "batch missing dictType" }
        assertEquals("svc-item-name-1", map[parentItemCode])
    }

    /** getDirectChildrenOfDictAsNodes maps cache entries to lightweight nodes. */
    @Test
    fun getDirectChildrenOfDictAsNodes() {
        sysDictItemHashCache.reloadAll(clear = true)
        val nodes = sysDictItemService.getDirectChildrenOfDictAsNodes(atomicServiceCode, dictType)
        assertTrue(nodes.any { it.id == seededParentItemId && it.itemCode == parentItemCode })
    }

    /** getDirectChildrenOfItemAsNodes maps the children of a parent item code to nodes. */
    @Test
    fun getDirectChildrenOfItemAsNodes() {
        sysDictItemHashCache.reloadAll(clear = true)
        val nodes = sysDictItemService.getDirectChildrenOfItemAsNodes(atomicServiceCode, dictType, parentItemCode)
        assertTrue(nodes.any { it.id == seededChildItemId && it.itemCode == childItemCode })
    }

    /** getDirectChildrenOfItemAsNodes returns empty for an unknown item code. */
    @Test
    fun getDirectChildrenOfItemAsNodes_emptyForUnknownCode() {
        sysDictItemHashCache.reloadAll(clear = true)
        val nodes = sysDictItemService.getDirectChildrenOfItemAsNodes(atomicServiceCode, dictType, "no-such-code")
        assertTrue(nodes.isEmpty())
    }

    /** getDirectChildrenOfItemFromCache(itemCode) returns empty when the item code is unknown. */
    @Test
    fun getDirectChildrenOfItemFromCache_emptyForUnknownCode() {
        sysDictItemHashCache.reloadAll(clear = true)
        val children = sysDictItemService.getDirectChildrenOfItemFromCache(atomicServiceCode, dictType, "no-such-code")
        assertTrue(children.isEmpty())
    }

    /** getDirectChildrenOfDictFromCache with activeOnly=false still returns root-level items. */
    @Test
    fun getDirectChildrenOfDictFromCache_includeInactive() {
        sysDictItemHashCache.reloadAll(clear = true)
        val roots = sysDictItemService.getDirectChildrenOfDictFromCache(atomicServiceCode, dictType, activeOnly = false)
        assertTrue(roots.any { it.id == seededParentItemId })
    }

    /** getDirectChildrenOfItemFromCache(parentId, activeOnly=false) still returns the child. */
    @Test
    fun getDirectChildrenOfItemFromCache_byParentId_includeInactive() {
        sysDictItemHashCache.reloadAll(clear = true)
        val children = sysDictItemService.getDirectChildrenOfItemFromCache(seededParentItemId, activeOnly = false)
        assertTrue(children.any { it.id == seededChildItemId })
    }

    /** get(id) returns null for a non-existent primary key. */
    @Test
    fun get_byId_nullWhenMissing() {
        assertNull(sysDictItemService.get("00000000-0000-0000-0000-0000000000ff"))
    }
}
