package io.kudos.ms.sys.api.internal.controller.resource

import io.kudos.ms.sys.common.resource.enums.ResourceTypeEnum
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.sys.common.resource.vo.response.BaseMenuTreeNode
import io.kudos.ms.sys.common.resource.vo.response.MenuTreeNode
import io.kudos.ms.sys.core.resource.api.SysResourceApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for SysResourceInternalController (pure delegation, core api mocked with Mockito, no Spring container)
 *
 * Coverage:
 * - getResource delegates (hit and null miss)
 * - getResources(Collection) delegates returning the same map (incl. empty map)
 * - getResources(ResourceTypeEnum, subSystemCode) delegates
 * - getSimpleMenus / getMenus delegate (incl. empty)
 * - getResourceId delegates (hit and null miss)
 * - getDirectChildrenResources delegates with non-null and null parentId
 * - getChildrenResources delegates
 *
 * @author K
 * @since 1.0.0
 */
internal class SysResourceInternalControllerTest {

    private val api = mock(SysResourceApi::class.java)
    private val controller = SysResourceInternalController(api)

    @Test
    fun getResource_delegates() {
        val entry = mock(SysResourceCacheEntry::class.java)
        whenCalled(api.getResource("r-1")).thenReturn(entry)
        whenCalled(api.getResource("r-missing")).thenReturn(null)

        assertSame(entry, controller.getResource("r-1"))
        assertNull(controller.getResource("r-missing"))
        verify(api).getResource("r-1")
        verify(api).getResource("r-missing")
    }

    @Test
    fun getResources_byIds_delegates() {
        val ids = listOf("r-1", "r-2")
        val map = mapOf("r-1" to mock(SysResourceCacheEntry::class.java))
        whenCalled(api.getResources(ids)).thenReturn(map)

        assertSame(map, controller.getResources(ids))
        verify(api).getResources(ids)
    }

    @Test
    fun getResources_byIds_emptyResult() {
        val ids = emptyList<String>()
        whenCalled(api.getResources(ids)).thenReturn(emptyMap())

        assertTrue(controller.getResources(ids).isEmpty())
        verify(api).getResources(ids)
    }

    @Test
    fun getResources_byTypeAndSubSystem_delegates() {
        val resources = listOf(mock(SysResourceCacheEntry::class.java))
        whenCalled(api.getResources(ResourceTypeEnum.MENU, "sub-a")).thenReturn(resources)

        assertSame(resources, controller.getResources(ResourceTypeEnum.MENU, "sub-a"))
        verify(api).getResources(ResourceTypeEnum.MENU, "sub-a")
    }

    @Test
    fun getSimpleMenus_delegates() {
        val menus = listOf(mock(BaseMenuTreeNode::class.java))
        whenCalled(api.getSimpleMenus("sub-a")).thenReturn(menus)

        assertSame(menus, controller.getSimpleMenus("sub-a"))
        assertTrue(controller.getSimpleMenus("sub-empty").isEmpty())
        verify(api).getSimpleMenus("sub-a")
    }

    @Test
    fun getMenus_delegates() {
        val menus = listOf(mock(MenuTreeNode::class.java))
        whenCalled(api.getMenus("sub-a")).thenReturn(menus)

        assertSame(menus, controller.getMenus("sub-a"))
        verify(api).getMenus("sub-a")
    }

    @Test
    fun getResourceId_delegates() {
        whenCalled(api.getResourceId("sub-a", "/url/a")).thenReturn("r-9")
        whenCalled(api.getResourceId("sub-a", "/url/missing")).thenReturn(null)

        assertSame("r-9", controller.getResourceId("sub-a", "/url/a"))
        assertNull(controller.getResourceId("sub-a", "/url/missing"))
        verify(api).getResourceId("sub-a", "/url/a")
        verify(api).getResourceId("sub-a", "/url/missing")
    }

    @Test
    fun getDirectChildrenResources_delegates_withParentId() {
        val children = listOf(mock(SysResourceCacheEntry::class.java))
        whenCalled(api.getDirectChildrenResources(ResourceTypeEnum.MENU, "p-1", "sub-a")).thenReturn(children)

        assertSame(children, controller.getDirectChildrenResources(ResourceTypeEnum.MENU, "p-1", "sub-a"))
        verify(api).getDirectChildrenResources(ResourceTypeEnum.MENU, "p-1", "sub-a")
    }

    @Test
    fun getDirectChildrenResources_delegates_withNullParentId() {
        whenCalled(api.getDirectChildrenResources(ResourceTypeEnum.MENU, null, "sub-a")).thenReturn(emptyList())

        assertTrue(controller.getDirectChildrenResources(ResourceTypeEnum.MENU, null, "sub-a").isEmpty())
        verify(api).getDirectChildrenResources(ResourceTypeEnum.MENU, null, "sub-a")
    }

    @Test
    fun getChildrenResources_delegates() {
        val children = listOf(mock(SysResourceCacheEntry::class.java))
        whenCalled(api.getChildrenResources("sub-a", ResourceTypeEnum.MENU, "p-1")).thenReturn(children)

        assertSame(children, controller.getChildrenResources("sub-a", ResourceTypeEnum.MENU, "p-1"))
        verify(api).getChildrenResources("sub-a", ResourceTypeEnum.MENU, "p-1")
    }
}
