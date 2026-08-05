package io.kudos.ms.sys.api.admin.controller.dict

import io.kudos.base.query.PagingSearchResult
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.common.dict.vo.request.SysDictItemQuery
import io.kudos.ms.sys.common.dict.vo.response.SysDictItemNode
import io.kudos.ms.sys.common.dict.vo.response.SysDictItemRow
import io.kudos.ms.sys.core.dict.service.impl.VSysDictItemService
import io.kudos.ms.sys.core.dict.service.iservice.ISysDictItemService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Pure unit test for [SysDictItemAdminController]: every extra endpoint delegating either to the
 * read-only view service or the dict-item service, plus the default `activeOnly` parameters and
 * order-preserving map returns. Both collaborators are mocked and injected via reflection; no
 * Spring context.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysDictItemAdminControllerTest {

    private val service = mock(ISysDictItemService::class.java)
    private val vService = mock(VSysDictItemService::class.java)
    private val itemService = mock(ISysDictItemService::class.java)
    private val controller = SysDictItemAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
        ReflectionTestUtils.setField(it, "vSysDictItemService", vService)
        ReflectionTestUtils.setField(it, "sysDictItemService", itemService)
    }

    @Test
    fun getDictItemDelegatesToViewServiceHit() {
        val entry = mock(SysDictItemCacheEntry::class.java)
        whenCalled(vService.getFromCache("i1")).thenReturn(entry)
        assertSame(entry, controller.getDictItem("i1"))
        verify(vService).getFromCache("i1")
    }

    @Test
    fun getDictItemMiss() {
        whenCalled(vService.getFromCache("none")).thenReturn(null)
        assertNull(controller.getDictItem("none"))
    }

    @Test
    fun pagingSearchDictDelegatesToViewService() {
        val query = SysDictItemQuery(dictType = "color")
        val page = PagingSearchResult(data = listOf(SysDictItemRow()), totalCount = 1)
        whenCalled(vService.pagingSearchWithParentCode(query)).thenReturn(page)
        assertSame(page, controller.pagingSearchDict(query))
        verify(vService).pagingSearchWithParentCode(query)
    }

    @Test
    fun getDirectChildrenOfDictDefaultsActiveOnlyTrue() {
        val nodes = listOf(mock(SysDictItemNode::class.java))
        whenCalled(itemService.getDirectChildrenOfDictAsNodes("svc", "color", true)).thenReturn(nodes)
        assertSame(nodes, controller.getDirectChildrenOfDict("svc", "color"))
        verify(itemService).getDirectChildrenOfDictAsNodes("svc", "color", true)
    }

    @Test
    fun getDirectChildrenOfDictHonorsActiveOnlyFalse() {
        whenCalled(itemService.getDirectChildrenOfDictAsNodes("svc", "color", false)).thenReturn(emptyList())
        assertTrue(controller.getDirectChildrenOfDict("svc", "color", false).isEmpty())
        verify(itemService).getDirectChildrenOfDictAsNodes("svc", "color", false)
    }

    @Test
    fun getDirectChildrenOfItemDefaultsActiveOnlyTrue() {
        val nodes = listOf(mock(SysDictItemNode::class.java))
        whenCalled(itemService.getDirectChildrenOfItemAsNodes("svc", "color", "RED", true)).thenReturn(nodes)
        assertSame(nodes, controller.getDirectChildrenOfItem("svc", "color", "RED"))
        verify(itemService).getDirectChildrenOfItemAsNodes("svc", "color", "RED", true)
    }

    @Test
    fun getDirectChildrenOfItemHonorsActiveOnlyFalse() {
        whenCalled(itemService.getDirectChildrenOfItemAsNodes("svc", "color", "RED", false)).thenReturn(emptyList())
        assertTrue(controller.getDirectChildrenOfItem("svc", "color", "RED", false).isEmpty())
    }

    @Test
    fun getDictItemsDelegates() {
        val items = listOf(mock(SysDictItemCacheEntry::class.java))
        whenCalled(itemService.getDictItemsFromCache("color", "svc")).thenReturn(items)
        assertSame(items, controller.getDictItems("color", "svc"))
        verify(itemService).getDictItemsFromCache("color", "svc")
    }

    @Test
    fun batchGetDictItemsDelegates() {
        val arg = mapOf("svc" to listOf("color", "size"))
        val ret = mapOf("svc" to mapOf("color" to listOf(mock(SysDictItemCacheEntry::class.java))))
        whenCalled(itemService.batchGetDictItemsFromCache(arg)).thenReturn(ret)
        assertSame(ret, controller.batchGetDictItems(arg))
        verify(itemService).batchGetDictItemsFromCache(arg)
    }

    @Test
    fun getDictItemMapPreservesOrder() {
        val map = linkedMapOf("RED" to "Red", "GREEN" to "Green")
        whenCalled(itemService.getDictItemMapFromCache("color", "svc")).thenReturn(map)
        val result = controller.getDictItemMap("color", "svc")
        assertSame(map, result)
        assertEquals(listOf("RED", "GREEN"), result.keys.toList())
    }

    @Test
    fun batchGetDictItemMapDelegates() {
        val arg = mapOf("svc" to listOf("color"))
        val ret = mapOf("svc" to mapOf("color" to linkedMapOf("RED" to "Red")))
        whenCalled(itemService.batchGetDictItemMapFromCache(arg)).thenReturn(ret)
        assertSame(ret, controller.batchGetDictItemMap(arg))
        verify(itemService).batchGetDictItemMapFromCache(arg)
    }

    @Test
    fun updateActiveDelegatesBothOutcomes() {
        whenCalled(service.updateActive("i1", true)).thenReturn(true)
        whenCalled(service.updateActive("i2", false)).thenReturn(false)
        assertTrue(controller.updateActive("i1", true))
        assertFalse(controller.updateActive("i2", false))
        verify(service).updateActive("i1", true)
        verify(service).updateActive("i2", false)
    }
}
