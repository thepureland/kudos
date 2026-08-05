package io.kudos.ms.sys.api.internal.controller.accessrule

import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import io.kudos.ms.sys.core.accessrule.api.SysAccessRuleIpApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for SysAccessRuleIpInternalController (pure delegation, core api mocked with Mockito, no Spring container)
 *
 * Coverage:
 * - getIpsByRuleId delegates (non-empty and empty results)
 * - getIpsBySystemAndTenant delegates with null and non-null tenantId
 * - checkIpAccess delegates for allow/deny and null tenantId (incl. boundary ip 0)
 * - deleteByRuleId delegates (0 and positive counts)
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAccessRuleIpInternalControllerTest {

    private val api = mock(SysAccessRuleIpApi::class.java)
    private val controller = SysAccessRuleIpInternalController(api)

    @Test
    fun getIpsByRuleId_delegates() {
        val rows = listOf(mock(SysAccessRuleIpRow::class.java))
        whenCalled(api.getIpsByRuleId("rule-1")).thenReturn(rows)

        assertSame(rows, controller.getIpsByRuleId("rule-1"))
        assertTrue(controller.getIpsByRuleId("rule-none").isEmpty())
        verify(api).getIpsByRuleId("rule-1")
    }

    @Test
    fun getIpsBySystemAndTenant_delegatesWithNullableTenant() {
        val entries = listOf(mock(SysAccessRuleIpCacheEntry::class.java))
        whenCalled(api.getIpsBySystemAndTenant("sys-a", "tenant-1")).thenReturn(entries)
        whenCalled(api.getIpsBySystemAndTenant("sys-a", null)).thenReturn(emptyList())

        assertSame(entries, controller.getIpsBySystemAndTenant("sys-a", "tenant-1"))
        assertTrue(controller.getIpsBySystemAndTenant("sys-a", null).isEmpty())
        verify(api).getIpsBySystemAndTenant("sys-a", "tenant-1")
        verify(api).getIpsBySystemAndTenant("sys-a", null)
    }

    @Test
    fun checkIpAccess_delegates() {
        val allowedIp = BigDecimal("3232235777")
        val zeroIp = BigDecimal.ZERO
        whenCalled(api.checkIpAccess(allowedIp, "sys-a", "tenant-1")).thenReturn(true)
        whenCalled(api.checkIpAccess(zeroIp, "sys-a", null)).thenReturn(false)

        assertTrue(controller.checkIpAccess(allowedIp, "sys-a", "tenant-1"))
        assertFalse(controller.checkIpAccess(zeroIp, "sys-a", null))
        verify(api).checkIpAccess(allowedIp, "sys-a", "tenant-1")
        verify(api).checkIpAccess(zeroIp, "sys-a", null)
    }

    @Test
    fun deleteByRuleId_delegates() {
        whenCalled(api.deleteByRuleId("rule-1")).thenReturn(3)

        assertEquals(3, controller.deleteByRuleId("rule-1"))
        assertEquals(0, controller.deleteByRuleId("rule-none"))
        verify(api).deleteByRuleId("rule-1")
        verify(api).deleteByRuleId("rule-none")
    }
}
