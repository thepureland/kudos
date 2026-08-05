package io.kudos.ms.sys.common.resource.api

import io.kudos.ms.sys.common.resource.enums.ResourceTypeEnum
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.sys.common.resource.vo.response.BaseMenuTreeNode
import io.kudos.ms.sys.common.resource.vo.response.MenuTreeNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for ISysResourceApi
 *
 * Covers all interface methods (including the two overloaded getResources signatures
 * and the nullable parentId of getDirectChildrenResources) through a fake implementation
 * that records the dispatched arguments.
 *
 * @author K
 * @since 1.0.0
 */
internal class ISysResourceApiTest {

    private fun cacheEntry(id: String) = SysResourceCacheEntry(
        id = id, name = id, url = null, resourceTypeDictCode = "1", parentId = null,
        orderNum = null, icon = null, subSystemCode = "sys", remark = null,
        active = true, builtIn = false, createUserId = null, createUserName = null,
        createTime = null, updateUserId = null, updateUserName = null, updateTime = null,
    )

    private inner class FakeApi : ISysResourceApi {
        var lastParentId: String? = "untouched"

        override fun getResource(resourceId: String): SysResourceCacheEntry? =
            if (resourceId == "r-1") cacheEntry("r-1") else null

        override fun getResources(resourceIds: Collection<String>): Map<String, SysResourceCacheEntry> =
            resourceIds.associateWith { cacheEntry(it) }

        override fun getResources(resourceType: ResourceTypeEnum, subSystemCode: String): List<SysResourceCacheEntry> =
            listOf(cacheEntry("${resourceType.code}-$subSystemCode"))

        override fun getSimpleMenus(subSystemCode: String): List<BaseMenuTreeNode> =
            listOf(BaseMenuTreeNode().apply { id = subSystemCode })

        override fun getMenus(subSystemCode: String): List<MenuTreeNode> =
            listOf(MenuTreeNode().apply { id = subSystemCode })

        override fun getResourceId(subSysDictCode: String, url: String): String? =
            if (url == "/u") "r-1" else null

        override fun getDirectChildrenResources(
            resourceType: ResourceTypeEnum,
            parentId: String?,
            subSystemCode: String,
        ): List<SysResourceCacheEntry> {
            lastParentId = parentId
            return listOf(cacheEntry("c-1"))
        }

        override fun getChildrenResources(
            subSystemCode: String,
            resourceType: ResourceTypeEnum,
            parentId: String,
        ): List<SysResourceCacheEntry> = listOf(cacheEntry(parentId))
    }

    @Test
    fun getResourceSingle() {
        val api = FakeApi()
        assertEquals("r-1", api.getResource("r-1")?.id)
        assertNull(api.getResource("missing"))
    }

    @Test
    fun getResourcesByIds() {
        val api = FakeApi()
        val map = api.getResources(listOf("a", "b"))
        assertEquals(setOf("a", "b"), map.keys)
        assertEquals("a", map["a"]?.id)
    }

    @Test
    fun getResourcesByType() {
        val api = FakeApi()
        val list = api.getResources(ResourceTypeEnum.MENU, "sys")
        assertEquals("1-sys", list.single().id)
    }

    @Test
    fun menus() {
        val api = FakeApi()
        assertEquals("sys", api.getSimpleMenus("sys").single().id)
        assertEquals("sys", api.getMenus("sys").single().id)
    }

    @Test
    fun getResourceId() {
        val api = FakeApi()
        assertEquals("r-1", api.getResourceId("sd", "/u"))
        assertNull(api.getResourceId("sd", "/other"))
    }

    @Test
    fun directChildrenWithNullParent() {
        val api = FakeApi()
        val list = api.getDirectChildrenResources(ResourceTypeEnum.MENU, null, "sys")
        assertNull(api.lastParentId)
        assertEquals("c-1", list.single().id)
    }

    @Test
    fun directChildrenWithParent() {
        val api = FakeApi()
        api.getDirectChildrenResources(ResourceTypeEnum.MENU, "p-1", "sys")
        assertEquals("p-1", api.lastParentId)
    }

    @Test
    fun childrenResources() {
        val api = FakeApi()
        assertEquals("p-1", api.getChildrenResources("sys", ResourceTypeEnum.FUNCTION, "p-1").single().id)
        assertTrue(api.getChildrenResources("sys", ResourceTypeEnum.FUNCTION, "p-1").isNotEmpty())
    }

}
