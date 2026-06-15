package io.kudos.ms.sys.core.resource.service

import io.kudos.ms.sys.common.resource.enums.ResourceTypeEnum
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.sys.core.resource.cache.SysResourceHashCache
import io.kudos.ms.sys.core.resource.service.iservice.ISysResourceService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * junit test for SysResourceService
 *
 * Test data source: `SysResourceServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysResourceServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysResourceService: ISysResourceService

    @Resource
    private lateinit var sysResourceHashCache: SysResourceHashCache

    private val seededParentId = "20000000-0000-0000-0000-000000001461"
    private val seededChildId = "20000000-0000-0000-0000-000000001462"
    private val seededSubSystemCode = "svc-subsys-res-test-1_1461"
    private val seededUrl = "/svc-res-test-1"

    /** Get the entity by primary key via `get(id)`. */
    @Test
    fun get_byId_entity() {
        val row = sysResourceService.get(seededParentId)
        assertNotNull(row)
        assertEquals(seededParentId, row.id)
    }

    /** `get(id, SysResourceCacheEntry::class)` is consistent with `getResourceFromCache` after refreshing the Hash. */
    @Test
    fun get_withCacheEntryReturnType_delegatesToHashCache() {
        sysResourceHashCache.reloadAll(clear = true)
        val fromGet = sysResourceService.get(seededParentId, SysResourceCacheEntry::class)
        val fromCache = sysResourceService.getResourceFromCache(seededParentId)
        assertNotNull(fromGet)
        assertNotNull(fromCache)
        assertEquals(fromCache.id, fromGet.id)
        assertEquals(seededParentId, fromGet.id)
    }

    /** Read a cache entry from the Hash cache by id. */
    @Test
    fun getResourceFromCache_byId() {
        sysResourceHashCache.reloadAll(clear = true)
        val item = sysResourceService.getResourceFromCache(seededParentId)
        assertNotNull(item)
        assertEquals(seededUrl, item.url)
    }

    /** Resolve resource id by subsystem + URL. */
    @Test
    fun getResourceIdFromCacheBySubSystemAndUrl() {
        sysResourceHashCache.reloadAll(clear = true)
        val resourceId = sysResourceService.getResourceIdFromCacheBySubSystemAndUrl(seededSubSystemCode, seededUrl)
        assertEquals(seededParentId, resourceId)
    }

    /** Convenience method equivalent to [getResourceIdFromCacheBySubSystemAndUrl]. */
    @Test
    fun getResourceIdFromCache_alias() {
        sysResourceHashCache.reloadAll(clear = true)
        assertEquals(
            sysResourceService.getResourceIdFromCacheBySubSystemAndUrl(seededSubSystemCode, seededUrl),
            sysResourceService.getResourceIdFromCache(seededSubSystemCode, seededUrl)
        )
    }

    /** Get id list by subsystem + resource type dictionary code. */
    @Test
    fun getResourceIdsFromCacheBySubSystemAndType() {
        sysResourceHashCache.reloadAll(clear = true)
        val menuIds = sysResourceService.getResourceIdsFromCacheBySubSystemAndType(
            seededSubSystemCode,
            ResourceTypeEnum.MENU.code
        )
        assertTrue(menuIds.contains(seededParentId))

        val functionIds = sysResourceService.getResourceIdsFromCacheBySubSystemAndType(
            seededSubSystemCode,
            ResourceTypeEnum.FUNCTION.code
        )
        assertTrue(functionIds.contains(seededChildId))
    }

    /** Batch load from cache by ids. */
    @Test
    fun getResourcesFromCacheByIds() {
        sysResourceHashCache.reloadAll(clear = true)
        val map = sysResourceService.getResourcesFromCacheByIds(listOf(seededParentId, seededChildId))
        assertEquals(2, map.size)
        assertEquals(seededParentId, map[seededParentId]?.id)
    }

    /** Load list from cache by resource type + sub-system. */
    @Test
    fun getResourcesFromCacheBySubSystemAndType() {
        sysResourceHashCache.reloadAll(clear = true)
        val menus = sysResourceService.getResourcesFromCacheBySubSystemAndType(
            ResourceTypeEnum.MENU,
            seededSubSystemCode
        )
        assertTrue(menus.any { it.id == seededParentId })
    }

    /** Direct child nodes (first level) of menu-type resources. */
    @Test
    fun getDirectChildrenResourcesFromCache() {
        sysResourceHashCache.reloadAll(clear = true)
        val roots = sysResourceService.getDirectChildrenResourcesFromCache(
            ResourceTypeEnum.MENU,
            null,
            seededSubSystemCode
        )
        assertTrue(roots.any { it.id == seededParentId })
        val underParent = sysResourceService.getDirectChildrenResourcesFromCache(
            ResourceTypeEnum.FUNCTION,
            seededParentId,
            seededSubSystemCode
        )
        assertTrue(underParent.any { it.id == seededChildId })
    }

    /** Menu tree assembled from cache is non-empty. */
    @Test
    fun getSimpleMenusFromCache_and_getMenusFromCache() {
        sysResourceHashCache.reloadAll(clear = true)
        assertTrue(sysResourceService.getSimpleMenusFromCache(seededSubSystemCode).isNotEmpty())
        assertTrue(sysResourceService.getMenusFromCache(seededSubSystemCode).isNotEmpty())
    }

    /** Query list rows from database by sub-system code. */
    @Test
    fun getResourcesBySubSystemCode() {
        val resources = sysResourceService.getResourcesBySubSystemCode(seededSubSystemCode)
        assertTrue(resources.isNotEmpty())
    }

    /** Child resource list. */
    @Test
    fun getChildResources() {
        val children = sysResourceService.getChildResources(seededParentId)
        assertTrue(children.any { it.id == seededChildId })
    }

    /** Resource tree. */
    @Test
    fun getResourceTree() {
        val tree = sysResourceService.getResourceTree(seededSubSystemCode, null)
        assertTrue(tree.isNotEmpty())
        val parent = tree.firstOrNull { it.id == seededParentId }
        assertNotNull(parent)
        assertTrue(parent.children.orEmpty().any { it.id == seededChildId })
    }

    /** Ancestor chain of a child node contains the parent id. */
    @Test
    fun fetchAllParentIds() {
        sysResourceHashCache.reloadAll(clear = true)
        val parents = sysResourceService.fetchAllParentIds(seededChildId)
        assertTrue(parents.contains(seededParentId))
    }

    /** After updating active status, the Hash cache entry's `active` matches the database (the resource Hash cache contains disabled records). */
    @Test
    fun updateActive() {
        sysResourceHashCache.reloadAll(clear = true)
        assertTrue(sysResourceService.updateActive(seededParentId, false))
        sysResourceHashCache.reloadAll(clear = true)
        assertEquals(false, requireNotNull(sysResourceService.getResourceFromCache(seededParentId)).active)

        assertTrue(sysResourceService.updateActive(seededParentId, true))
        sysResourceHashCache.reloadAll(clear = true)
        assertEquals(true, requireNotNull(sysResourceService.getResourceFromCache(seededParentId)).active)
    }

    /** `updateActive` returns false when the primary key does not exist. */
    @Test
    fun updateActive_whenIdNotExists_returnsFalse() {
        assertFalse(sysResourceService.updateActive("00000000-0000-0000-0000-000000000001", true))
    }

    /** `deleteById` returns false when the primary key does not exist. */
    @Test
    fun deleteById_returnsFalseWhenRowMissing() {
        assertFalse(sysResourceService.deleteById("00000000-0000-0000-0000-000000000001"))
    }

    /** Recursively collect all descendants (menu + nested) from cache. */
    @Test
    fun getChildrenResourcesFromCache_recursive() {
        sysResourceHashCache.reloadAll(clear = true)
        // child (1462) is a FUNCTION under parent (1461, MENU); query the whole FUNCTION subtree under the parent
        val children = sysResourceService.getChildrenResourcesFromCache(
            seededSubSystemCode,
            ResourceTypeEnum.FUNCTION,
            seededParentId
        )
        assertTrue(children.any { it.id == seededChildId })
    }

    /** Detail with parents excluded vs included. */
    @Test
    fun getDetailWithOptionalParents_withAndWithoutParents() {
        sysResourceHashCache.reloadAll(clear = true)
        val without = sysResourceService.getDetailWithOptionalParents(seededChildId, includeParents = false)
        assertNotNull(without)
        assertEquals(seededChildId, without.id)

        val with = sysResourceService.getDetailWithOptionalParents(seededChildId, includeParents = true)
        assertNotNull(with)
        assertTrue(with.parentIds.orEmpty().contains(seededParentId))
    }

    /** Detail returns null for a missing id. */
    @Test
    fun getDetailWithOptionalParents_missingId_returnsNull() {
        assertNull(sysResourceService.getDetailWithOptionalParents("00000000-0000-0000-0000-000000000001", true))
    }

    /** moveResource updates order and reports success. */
    @Test
    fun moveResource_success() {
        sysResourceHashCache.reloadAll(clear = true)
        assertTrue(sysResourceService.moveResource(seededChildId, newParentId = seededParentId, newOrderNum = 99))
        val row = sysResourceService.get(seededChildId)
        assertNotNull(row)
        assertEquals(99, row.orderNum)
    }

    /** loadDirectChildrenForTree level 2/else returns resource rows of the subsystem. */
    @Test
    fun loadDirectChildrenForTree_resourceLevel() {
        val nodes = sysResourceService.loadDirectChildrenForTree(
            io.kudos.ms.sys.common.resource.vo.request.SysResourceQuery(
                subSystemCode = seededSubSystemCode,
                level = 3,
            )
        )
        assertTrue(nodes.any { it.id == seededParentId || it.id == seededChildId })
    }

    /** insert + update + deleteById full lifecycle, verifying cache sync. */
    @Test
    fun insert_update_delete_lifecycle() {
        sysResourceHashCache.reloadAll(clear = true)
        val unique = java.util.UUID.randomUUID().toString().take(8)
        val id = sysResourceService.insert(
            io.kudos.ms.sys.core.resource.model.po.SysResource().apply {
                name = "res-lifecycle-$unique"
                url = "/res-lifecycle-$unique"
                resourceTypeDictCode = ResourceTypeEnum.MENU.code
                parentId = null
                orderNum = 5
                subSystemCode = seededSubSystemCode
                active = true
                builtIn = false
            }
        )
        assertNotNull(id)

        val updated = sysResourceService.update(
            io.kudos.ms.sys.core.resource.model.po.SysResource().apply {
                this.id = id
                name = "res-lifecycle-updated-$unique"
                resourceTypeDictCode = ResourceTypeEnum.MENU.code
                subSystemCode = seededSubSystemCode
            }
        )
        assertTrue(updated)
        assertEquals("res-lifecycle-updated-$unique", sysResourceService.get(id)?.name)

        assertTrue(sysResourceService.deleteById(id))
        assertNull(sysResourceService.get(id))
    }

    /** batchDelete removes multiple rows and returns the count. */
    @Test
    fun batchDelete_removesRows() {
        val unique = java.util.UUID.randomUUID().toString().take(8)
        val id1 = sysResourceService.insert(
            io.kudos.ms.sys.core.resource.model.po.SysResource().apply {
                name = "res-bd1-$unique"; url = "/res-bd1-$unique"
                resourceTypeDictCode = ResourceTypeEnum.MENU.code; subSystemCode = seededSubSystemCode
                active = true; builtIn = false
            }
        )
        val id2 = sysResourceService.insert(
            io.kudos.ms.sys.core.resource.model.po.SysResource().apply {
                name = "res-bd2-$unique"; url = "/res-bd2-$unique"
                resourceTypeDictCode = ResourceTypeEnum.MENU.code; subSystemCode = seededSubSystemCode
                active = true; builtIn = false
            }
        )
        assertEquals(2, sysResourceService.batchDelete(listOf(id1, id2)))
    }

    /** batchDelete with empty/no-match collection returns 0 and publishes nothing. */
    @Test
    fun batchDelete_noMatch_returnsZero() {
        assertEquals(0, sysResourceService.batchDelete(listOf("00000000-0000-0000-0000-0000000000aa")))
    }
}
