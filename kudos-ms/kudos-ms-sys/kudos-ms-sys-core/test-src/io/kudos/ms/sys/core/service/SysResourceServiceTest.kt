package io.kudos.ms.sys.core.service

import io.kudos.ms.sys.common.enums.resource.ResourceTypeEnum
import io.kudos.ms.sys.common.vo.resource.SysResourceCacheEntry
import io.kudos.ms.sys.core.cache.SysResourceHashCache
import io.kudos.ms.sys.core.service.iservice.ISysResourceService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysResourceService
 *
 * 测试数据来源：`SysResourceServiceTest.sql`
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

    /** 按主键 `get(id)` 取实体。 */
    @Test
    fun get_byId_entity() {
        val row = sysResourceService.get(seededParentId)
        assertNotNull(row)
        assertEquals(seededParentId, row.id)
    }

    /** `get(id, SysResourceCacheEntry::class)` 与 `getResourceFromCache` 在刷新 Hash 后一致。 */
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

    /** 按 id 从 Hash 缓存读取缓存项。 */
    @Test
    fun getResourceFromCache_byId() {
        sysResourceHashCache.reloadAll(clear = true)
        val item = sysResourceService.getResourceFromCache(seededParentId)
        assertNotNull(item)
        assertEquals(seededUrl, item.url)
    }

    /** 子系统 + URL 解析资源 id。 */
    @Test
    fun getResourceIdFromCacheBySubSystemAndUrl() {
        sysResourceHashCache.reloadAll(clear = true)
        val resourceId = sysResourceService.getResourceIdFromCacheBySubSystemAndUrl(seededSubSystemCode, seededUrl)
        assertEquals(seededParentId, resourceId)
    }

    /** 与 [getResourceIdFromCacheBySubSystemAndUrl] 等价的便捷方法。 */
    @Test
    fun getResourceIdFromCache_alias() {
        sysResourceHashCache.reloadAll(clear = true)
        assertEquals(
            sysResourceService.getResourceIdFromCacheBySubSystemAndUrl(seededSubSystemCode, seededUrl),
            sysResourceService.getResourceIdFromCache(seededSubSystemCode, seededUrl)
        )
    }

    /** 按子系统 + 资源类型字典码取 id 列表。 */
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

    /** 批量按 id 从缓存加载。 */
    @Test
    fun getResourcesFromCacheByIds() {
        sysResourceHashCache.reloadAll(clear = true)
        val map = sysResourceService.getResourcesFromCacheByIds(listOf(seededParentId, seededChildId))
        assertEquals(2, map.size)
        assertEquals(seededParentId, map[seededParentId]?.id)
    }

    /** 按资源类型 + 子系统从缓存加载列表。 */
    @Test
    fun getResourcesFromCacheBySubSystemAndType() {
        sysResourceHashCache.reloadAll(clear = true)
        val menus = sysResourceService.getResourcesFromCacheBySubSystemAndType(
            ResourceTypeEnum.MENU,
            seededSubSystemCode
        )
        assertTrue(menus.any { it.id == seededParentId })
    }

    /** 菜单类型资源的直接子结点（第一层）。 */
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

    /** 从缓存组装的菜单树非空。 */
    @Test
    fun getSimpleMenusFromCache_and_getMenusFromCache() {
        sysResourceHashCache.reloadAll(clear = true)
        assertTrue(sysResourceService.getSimpleMenusFromCache(seededSubSystemCode).isNotEmpty())
        assertTrue(sysResourceService.getMenusFromCache(seededSubSystemCode).isNotEmpty())
    }

    /** 按子系统编码查库列表行。 */
    @Test
    fun getResourcesBySubSystemCode() {
        val resources = sysResourceService.getResourcesBySubSystemCode(seededSubSystemCode)
        assertTrue(resources.isNotEmpty())
    }

    /** 子资源列表。 */
    @Test
    fun getChildResources() {
        val children = sysResourceService.getChildResources(seededParentId)
        assertTrue(children.any { it.id == seededChildId })
    }

    /** 资源树。 */
    @Test
    fun getResourceTree() {
        val tree = sysResourceService.getResourceTree(seededSubSystemCode, null)
        assertTrue(tree.isNotEmpty())
    }

    /** 子结点的祖先链包含父 id。 */
    @Test
    fun fetchAllParentIds() {
        sysResourceHashCache.reloadAll(clear = true)
        val parents = sysResourceService.fetchAllParentIds(seededChildId)
        assertTrue(parents.contains(seededParentId))
    }

    /** 启用状态更新后，Hash 缓存中条目的 `active` 与库一致（资源 Hash 缓存含未启用记录）。 */
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

    /** 主键不存在时 `updateActive` 返回 false。 */
    @Test
    fun updateActive_whenIdNotExists_returnsFalse() {
        assertFalse(sysResourceService.updateActive("00000000-0000-0000-0000-000000000001", true))
    }

    /** 主键不存在时 `deleteById` 返回 false。 */
    @Test
    fun deleteById_returnsFalseWhenRowMissing() {
        assertFalse(sysResourceService.deleteById("00000000-0000-0000-0000-000000000001"))
    }
}
