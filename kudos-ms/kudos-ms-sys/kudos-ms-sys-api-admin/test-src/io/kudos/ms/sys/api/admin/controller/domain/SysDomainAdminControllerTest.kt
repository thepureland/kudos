package io.kudos.ms.sys.api.admin.controller.domain

import io.kudos.ms.sys.core.domain.service.iservice.ISysDomainService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Pure unit test for [SysDomainAdminController]: `updateActive` delegation. No Spring context.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysDomainAdminControllerTest {

    private val service = mock(ISysDomainService::class.java)
    private val controller = SysDomainAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
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
