package io.kudos.ms.sys.api.internal.controller.accessrule

import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleRow
import io.kudos.ms.sys.core.accessrule.api.SysAccessRuleApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for SysAccessRuleInternalController (pure delegation, core api mocked with Mockito, no Spring container)
 *
 * Coverage:
 * - getAccessRuleByTenantAndSystem delegates (hit and null miss)
 * - updateActive delegates for both true and false results
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAccessRuleInternalControllerTest {

    private val api = mock(SysAccessRuleApi::class.java)
    private val controller = SysAccessRuleInternalController(api)

    @Test
    fun getAccessRuleByTenantAndSystem_delegates() {
        val row = mock(SysAccessRuleRow::class.java)
        whenCalled(api.getAccessRuleByTenantAndSystem("sys-a", "tenant-1")).thenReturn(row)

        assertSame(row, controller.getAccessRuleByTenantAndSystem("sys-a", "tenant-1"))
        assertNull(controller.getAccessRuleByTenantAndSystem("sys-a", "tenant-missing"))
        verify(api).getAccessRuleByTenantAndSystem("sys-a", "tenant-1")
    }

    @Test
    fun updateActive_delegates() {
        whenCalled(api.updateActive("id-1", true)).thenReturn(true)
        whenCalled(api.updateActive("id-2", false)).thenReturn(false)

        assertTrue(controller.updateActive("id-1", true))
        assertFalse(controller.updateActive("id-2", false))
        verify(api).updateActive("id-1", true)
        verify(api).updateActive("id-2", false)
    }
}
