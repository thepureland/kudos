package io.kudos.ms.sys.api.admin.controller.accessrule

import io.kudos.ms.sys.core.accessrule.service.iservice.ISysAccessRuleService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Pure unit test for [SysAccessRuleAdminController]: the `updateActive` delegation to the service.
 * The base-controller `service` field is injected via reflection; no Spring context.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysAccessRuleAdminControllerTest {

    private val service = mock(ISysAccessRuleService::class.java)
    private val controller = SysAccessRuleAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    @Test
    fun updateActiveDelegatesBothOutcomes() {
        whenCalled(service.updateActive("a1", true)).thenReturn(true)
        whenCalled(service.updateActive("a2", false)).thenReturn(false)

        assertTrue(controller.updateActive("a1", true))
        assertFalse(controller.updateActive("a2", false))
        verify(service).updateActive("a1", true)
        verify(service).updateActive("a2", false)
    }
}
