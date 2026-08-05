package io.kudos.ms.sys.api.admin.controller.param

import io.kudos.ms.sys.core.param.service.iservice.ISysParamService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Pure unit test for [SysParamAdminController]: `updateActive` delegation. No Spring context.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysParamAdminControllerTest {

    private val service = mock(ISysParamService::class.java)
    private val controller = SysParamAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    @Test
    fun updateActiveDelegatesBothOutcomes() {
        whenCalled(service.updateActive("p1", true)).thenReturn(true)
        whenCalled(service.updateActive("p2", false)).thenReturn(false)

        assertTrue(controller.updateActive("p1", true))
        assertFalse(controller.updateActive("p2", false))
        verify(service).updateActive("p1", true)
        verify(service).updateActive("p2", false)
    }
}
