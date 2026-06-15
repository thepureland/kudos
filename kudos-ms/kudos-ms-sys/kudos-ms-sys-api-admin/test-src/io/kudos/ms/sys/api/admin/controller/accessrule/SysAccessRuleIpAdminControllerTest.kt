package io.kudos.ms.sys.api.admin.controller.accessrule

import io.kudos.base.query.PagingSearchResult
import io.kudos.ms.sys.common.accessrule.vo.request.VSysAccessRuleWithIpQuery
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import io.kudos.ms.sys.common.accessrule.vo.response.VSysAccessRuleWithIpRow
import io.kudos.ms.sys.core.accessrule.service.impl.VSysAccessRuleIpService
import io.kudos.ms.sys.core.accessrule.service.iservice.ISysAccessRuleIpService
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
 * Pure unit test for [SysAccessRuleIpAdminController]: the combined "rule + IP" read endpoints
 * delegating to the view service, the IP-by-rule endpoint, the nullable/blank-tenant search, the
 * unchecked-cast paging, and updateActive. The three collaborators (base `service`, the view
 * service, and the IP service) are mocked and injected via reflection; no Spring context.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysAccessRuleIpAdminControllerTest {

    private val service = mock(ISysAccessRuleIpService::class.java)
    private val vService = mock(VSysAccessRuleIpService::class.java)
    private val ipService = mock(ISysAccessRuleIpService::class.java)
    private val controller = SysAccessRuleIpAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
        ReflectionTestUtils.setField(it, "vSysAccessRuleIpService", vService)
        ReflectionTestUtils.setField(it, "sysAccessRuleIpService", ipService)
    }

    @Test
    fun getIpsByRuleIdDelegatesToIpService() {
        val rows = listOf(mock(SysAccessRuleIpRow::class.java))
        whenCalled(ipService.getIpsByRuleId("rule-1")).thenReturn(rows)
        assertSame(rows, controller.getIpsByRuleId("rule-1"))
        verify(ipService).getIpsByRuleId("rule-1")
    }

    @Test
    fun getIpsByRuleIdEmpty() {
        whenCalled(ipService.getIpsByRuleId("none")).thenReturn(emptyList())
        assertTrue(controller.getIpsByRuleId("none").isEmpty())
    }

    @Test
    fun getAccessRuleWithIpDelegatesHit() {
        val row = VSysAccessRuleWithIpRow(id = "row-1")
        whenCalled(vService.get("row-1", VSysAccessRuleWithIpRow::class)).thenReturn(row)
        assertSame(row, controller.getAccessRuleWithIp("row-1"))
        verify(vService).get("row-1", VSysAccessRuleWithIpRow::class)
    }

    @Test
    fun getAccessRuleWithIpDelegatesMiss() {
        whenCalled(vService.get("none", VSysAccessRuleWithIpRow::class)).thenReturn(null)
        assertNull(controller.getAccessRuleWithIp("none"))
    }

    @Test
    fun pagingSearchAccessRuleWithIpDelegatesAndCasts() {
        val query = VSysAccessRuleWithIpQuery(systemCode = "sys-a")
        val page: PagingSearchResult<*> = PagingSearchResult(
            data = listOf(VSysAccessRuleWithIpRow(id = "r1")),
            totalCount = 1,
        )
        whenCalled(vService.pagingSearch(query)).thenReturn(page)

        val result = controller.pagingSearchAccessRuleWithIp(query)
        assertEquals(1, result.totalCount)
        assertEquals("r1", result.data.single().id)
        verify(vService).pagingSearch(query)
    }

    @Test
    fun searchByParentIdDelegates() {
        val rows = listOf(VSysAccessRuleWithIpRow(id = "x"))
        whenCalled(vService.searchByParentId("parent-1")).thenReturn(rows)
        assertSame(rows, controller.searchByParentId("parent-1"))
        verify(vService).searchByParentId("parent-1")
    }

    @Test
    fun searchBySystemCodeAndTenantIdForwardsTenant() {
        val rows = listOf(VSysAccessRuleWithIpRow(id = "x"))
        whenCalled(vService.searchBySystemCodeAndTenantId("sys-a", "t-1")).thenReturn(rows)
        assertSame(rows, controller.searchBySystemCodeAndTenantId("sys-a", "t-1"))
        verify(vService).searchBySystemCodeAndTenantId("sys-a", "t-1")
    }

    @Test
    fun searchBySystemCodeAndTenantIdForwardsNullTenant() {
        whenCalled(vService.searchBySystemCodeAndTenantId("sys-a", null)).thenReturn(emptyList())
        assertTrue(controller.searchBySystemCodeAndTenantId("sys-a", null).isEmpty())
        verify(vService).searchBySystemCodeAndTenantId("sys-a", null)
    }

    @Test
    fun updateActiveDelegatesBothOutcomes() {
        whenCalled(service.updateActive("ip1", true)).thenReturn(true)
        whenCalled(service.updateActive("ip2", false)).thenReturn(false)
        assertTrue(controller.updateActive("ip1", true))
        assertFalse(controller.updateActive("ip2", false))
        verify(service).updateActive("ip1", true)
        verify(service).updateActive("ip2", false)
    }
}
