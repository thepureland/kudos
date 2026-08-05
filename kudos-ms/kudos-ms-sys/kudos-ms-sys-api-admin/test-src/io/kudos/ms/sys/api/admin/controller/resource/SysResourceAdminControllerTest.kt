package io.kudos.ms.sys.api.admin.controller.resource

import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.ms.sys.common.platform.consts.SysConsts
import io.kudos.ms.sys.common.resource.enums.ResourceTypeEnum
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.sys.common.resource.vo.request.SysResourceQuery
import io.kudos.ms.sys.common.resource.vo.response.BaseMenuTreeNode
import io.kudos.ms.sys.common.resource.vo.response.MenuTreeNode
import io.kudos.ms.sys.common.resource.vo.response.SysResourceDetail
import io.kudos.ms.sys.core.resource.service.iservice.ISysResourceService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Pure unit test for [SysResourceAdminController]'s extra endpoints. All exercised paths return a
 * non-null service result, so none of them fall back to the base-controller getDetail (which would
 * need a Spring-resolved generic type); that fallback line is left to the serial integration run.
 *
 * Default-parameter coverage: every endpoint with a `subSystemCode` default is invoked once without
 * the argument to assert it forwards [SysConsts.DEFAULT_SUB_SYSTEM_CODE]. The service is mocked and
 * injected via reflection; no Spring context.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysResourceAdminControllerTest {

    private val service = mock(ISysResourceService::class.java)
    private val controller = SysResourceAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    private val defaultSub = SysConsts.DEFAULT_SUB_SYSTEM_CODE

    @Test
    fun getResourceDetailDefaultsFetchAllParentIdsFalse() {
        val detail = mock(SysResourceDetail::class.java)
        whenCalled(service.getDetailWithOptionalParents("r1", false)).thenReturn(detail)
        assertSame(detail, controller.getResourceDetail("r1"))
        verify(service).getDetailWithOptionalParents("r1", false)
    }

    @Test
    fun getResourceDetailHonorsFetchAllParentIdsTrue() {
        val detail = mock(SysResourceDetail::class.java)
        whenCalled(service.getDetailWithOptionalParents("r1", true)).thenReturn(detail)
        assertSame(detail, controller.getResourceDetail("r1", true))
        verify(service).getDetailWithOptionalParents("r1", true)
    }

    @Test
    fun getResourceDetailFallsBackToBaseGetDetailWhenServiceReturnsNull() {
        // getDetailWithOptionalParents returns null -> the elvis branch calls super.getDetail(id),
        // which resolves the detail VO class via reflection and delegates to the generic
        // service.get(id, KClass) overload (stubbed with raw values, no eq() matcher).
        val fallback = mock(SysResourceDetail::class.java)
        whenCalled(service.getDetailWithOptionalParents("missing", false)).thenReturn(null)
        whenCalled(service.get("missing", SysResourceDetail::class)).thenReturn(fallback)

        assertSame(fallback, controller.getResourceDetail("missing"))
        verify(service).getDetailWithOptionalParents("missing", false)
        verify(service).get("missing", SysResourceDetail::class)
    }

    @Test
    fun getResourcesDefaultsSubSystemCode() {
        val list = listOf(mock(SysResourceCacheEntry::class.java))
        whenCalled(service.getResourcesFromCacheBySubSystemAndType(ResourceTypeEnum.MENU, defaultSub)).thenReturn(list)
        assertSame(list, controller.getResources(ResourceTypeEnum.MENU))
        verify(service).getResourcesFromCacheBySubSystemAndType(ResourceTypeEnum.MENU, defaultSub)
    }

    @Test
    fun getResourcesHonorsExplicitSubSystemCode() {
        whenCalled(service.getResourcesFromCacheBySubSystemAndType(ResourceTypeEnum.FUNCTION, "sub-x"))
            .thenReturn(emptyList())
        assertTrue(controller.getResources(ResourceTypeEnum.FUNCTION, "sub-x").isEmpty())
        verify(service).getResourcesFromCacheBySubSystemAndType(ResourceTypeEnum.FUNCTION, "sub-x")
    }

    @Test
    fun getSimpleMenusDefaultsSubSystemCode() {
        val menus = listOf(mock(BaseMenuTreeNode::class.java))
        whenCalled(service.getSimpleMenusFromCache(defaultSub)).thenReturn(menus)
        assertSame(menus, controller.getSimpleMenus())
        verify(service).getSimpleMenusFromCache(defaultSub)
    }

    @Test
    fun getMenusDefaultsSubSystemCode() {
        val menus = listOf(mock(MenuTreeNode::class.java))
        whenCalled(service.getMenusFromCache(defaultSub)).thenReturn(menus)
        assertSame(menus, controller.getMenus())
        verify(service).getMenusFromCache(defaultSub)
    }

    @Test
    fun getDirectChildrenResourcesDefaultsSubSystemCodeAndNullParent() {
        val list = listOf(mock(SysResourceCacheEntry::class.java))
        whenCalled(service.getDirectChildrenResourcesFromCache(ResourceTypeEnum.MENU, null, defaultSub))
            .thenReturn(list)
        assertSame(list, controller.getDirectChildrenResources(ResourceTypeEnum.MENU, null))
        verify(service).getDirectChildrenResourcesFromCache(ResourceTypeEnum.MENU, null, defaultSub)
    }

    @Test
    fun getDirectChildrenResourcesWithParentAndExplicitSub() {
        whenCalled(service.getDirectChildrenResourcesFromCache(ResourceTypeEnum.MENU, "p1", "sub-y"))
            .thenReturn(emptyList())
        assertTrue(controller.getDirectChildrenResources(ResourceTypeEnum.MENU, "p1", "sub-y").isEmpty())
        verify(service).getDirectChildrenResourcesFromCache(ResourceTypeEnum.MENU, "p1", "sub-y")
    }

    @Test
    fun getChildrenResourcesForwardsArgsInServiceOrder() {
        // Controller signature is (resourceType, parentId, subSystemCode) but the service is called
        // as (subSystemCode, resourceType, parentId) -- verify the re-ordering.
        val list = listOf(mock(SysResourceCacheEntry::class.java))
        whenCalled(service.getChildrenResourcesFromCache("sub-z", ResourceTypeEnum.ACTION, "p9")).thenReturn(list)
        assertSame(list, controller.getChildrenResources(ResourceTypeEnum.ACTION, "p9", "sub-z"))
        verify(service).getChildrenResourcesFromCache("sub-z", ResourceTypeEnum.ACTION, "p9")
    }

    @Test
    fun loadDirectChildrenForTreeDelegates() {
        val query = SysResourceQuery()
        @Suppress("UNCHECKED_CAST")
        val node = mock(IdAndNameTreeNode::class.java) as IdAndNameTreeNode<String>
        val tree = listOf(node)
        whenCalled(service.loadDirectChildrenForTree(query)).thenReturn(tree)
        assertSame(tree, controller.loadDirectChildrenForTree(query))
        verify(service).loadDirectChildrenForTree(query)
    }

    @Test
    fun updateActiveDelegatesBothOutcomes() {
        whenCalled(service.updateActive("r1", true)).thenReturn(true)
        whenCalled(service.updateActive("r2", false)).thenReturn(false)
        assertTrue(controller.updateActive("r1", true))
        assertFalse(controller.updateActive("r2", false))
        verify(service).updateActive("r1", true)
        verify(service).updateActive("r2", false)
    }
}
