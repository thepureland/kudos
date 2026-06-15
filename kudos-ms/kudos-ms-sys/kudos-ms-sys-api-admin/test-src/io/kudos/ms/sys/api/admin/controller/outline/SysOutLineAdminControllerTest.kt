package io.kudos.ms.sys.api.admin.controller.outline

import io.kudos.ms.sys.core.outline.service.iservice.ISysOutLineService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Pure unit test for [SysOutLineAdminController]: `updateActive` delegation. No Spring context.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysOutLineAdminControllerTest {

    private val service = mock(ISysOutLineService::class.java)
    private val controller = SysOutLineAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    @Test
    fun updateActiveDelegatesBothOutcomes() {
        whenCalled(service.updateActive("o1", true)).thenReturn(true)
        whenCalled(service.updateActive("o2", false)).thenReturn(false)

        assertTrue(controller.updateActive("o1", true))
        assertFalse(controller.updateActive("o2", false))
        verify(service).updateActive("o1", true)
        verify(service).updateActive("o2", false)
    }
}
