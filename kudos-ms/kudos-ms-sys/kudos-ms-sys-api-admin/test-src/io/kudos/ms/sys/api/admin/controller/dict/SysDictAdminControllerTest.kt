package io.kudos.ms.sys.api.admin.controller.dict

import io.kudos.base.query.PagingSearchResult
import io.kudos.ms.sys.common.dict.vo.SysDictCacheEntry
import io.kudos.ms.sys.common.dict.vo.request.SysDictQuery
import io.kudos.ms.sys.common.dict.vo.response.SysDictRow
import io.kudos.ms.sys.core.dict.service.iservice.ISysDictService
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
 * Pure unit test for [SysDictAdminController]: getDict, pagingSearchDict (incl. the unchecked cast),
 * getDictTypesByAtomicServiceCode (and its default `activeOnly`), and updateActive. The service is
 * mocked and injected via reflection; no Spring context.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysDictAdminControllerTest {

    private val service = mock(ISysDictService::class.java)
    private val controller = SysDictAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    @Test
    fun getDictDelegatesHit() {
        val entry = mock(SysDictCacheEntry::class.java)
        whenCalled(service.getDictFromCache("d1")).thenReturn(entry)
        assertSame(entry, controller.getDict("d1"))
        verify(service).getDictFromCache("d1")
    }

    @Test
    fun getDictDelegatesMiss() {
        whenCalled(service.getDictFromCache("none")).thenReturn(null)
        assertNull(controller.getDict("none"))
    }

    @Test
    fun pagingSearchDictDelegatesAndCasts() {
        val query = SysDictQuery(dictType = "color")
        val page: PagingSearchResult<*> = PagingSearchResult(
            data = listOf(SysDictRow(id = "r1")),
            totalCount = 1,
        )
        whenCalled(service.pagingSearch(query)).thenReturn(page)

        val result: PagingSearchResult<SysDictRow> = controller.pagingSearchDict(query)
        assertEquals(1, result.totalCount)
        assertEquals("r1", result.data.single().id)
        verify(service).pagingSearch(query)
    }

    @Test
    fun getDictTypesByAtomicServiceCodeDefaultsActiveOnlyTrue() {
        val map = linkedMapOf("id1" to "type-a", "id2" to "type-b")
        whenCalled(service.getDictTypesByAtomicServiceCode("svc", true)).thenReturn(map)

        // default param activeOnly = true
        val result = controller.getDictTypesByAtomicServiceCode("svc")
        assertEquals(map, result)
        assertEquals(listOf("id1", "id2"), result.keys.toList()) // order preserved
        verify(service).getDictTypesByAtomicServiceCode("svc", true)
    }

    @Test
    fun getDictTypesByAtomicServiceCodeHonorsActiveOnlyFalse() {
        whenCalled(service.getDictTypesByAtomicServiceCode("svc", false)).thenReturn(emptyMap())
        assertTrue(controller.getDictTypesByAtomicServiceCode("svc", false).isEmpty())
        verify(service).getDictTypesByAtomicServiceCode("svc", false)
    }

    @Test
    fun updateActiveDelegatesBothOutcomes() {
        whenCalled(service.updateActive("d1", true)).thenReturn(true)
        whenCalled(service.updateActive("d2", false)).thenReturn(false)
        assertTrue(controller.updateActive("d1", true))
        assertFalse(controller.updateActive("d2", false))
        verify(service).updateActive("d1", true)
        verify(service).updateActive("d2", false)
    }
}
