package io.kudos.ms.sys.api.admin.controller.tenant

import io.kudos.base.model.vo.IdAndName
import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Pure unit test for [SysTenantAdminController]: getTenantsBySubSystemCode delegation and
 * updateActive. The service is mocked and injected via reflection; no Spring context.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysTenantAdminControllerTest {

    private val service = mock(ISysTenantService::class.java)
    private val controller = SysTenantAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    @Test
    fun getTenantsBySubSystemCodeDelegatesAndPreservesOrder() {
        val tenants = listOf(IdAndName("t1", "Tenant 1"), IdAndName("t2", "Tenant 2"))
        whenCalled(service.getActiveTenantIdAndNamesForSubSystem("sub-a")).thenReturn(tenants)

        val result = controller.getTenantsBySubSystemCode("sub-a")
        assertSame(tenants, result)
        assertEquals(listOf("t1", "t2"), result.map { it.id })
        assertEquals("Tenant 1", result[0].name)
        verify(service).getActiveTenantIdAndNamesForSubSystem("sub-a")
    }

    @Test
    fun getTenantsBySubSystemCodeEmpty() {
        whenCalled(service.getActiveTenantIdAndNamesForSubSystem("none")).thenReturn(emptyList())
        assertTrue(controller.getTenantsBySubSystemCode("none").isEmpty())
    }

    @Test
    fun updateActiveDelegatesBothOutcomes() {
        whenCalled(service.updateActive("t1", true)).thenReturn(true)
        whenCalled(service.updateActive("t2", false)).thenReturn(false)
        assertTrue(controller.updateActive("t1", true))
        assertFalse(controller.updateActive("t2", false))
        verify(service).updateActive("t1", true)
        verify(service).updateActive("t2", false)
    }
}
