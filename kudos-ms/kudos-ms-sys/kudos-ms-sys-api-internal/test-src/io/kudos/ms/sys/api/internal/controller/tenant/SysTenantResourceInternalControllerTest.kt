package io.kudos.ms.sys.api.internal.controller.tenant

import io.kudos.ms.sys.core.tenant.api.SysTenantResourceApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for SysTenantResourceInternalController (pure delegation, core api mocked with Mockito, no Spring container)
 *
 * Coverage:
 * - getResourceIdsByTenantId delegates (incl. empty set)
 * - getTenantIdsByResourceId delegates
 * - batchBind delegates and returns the affected-row count
 * - unbind delegates for both true and false results
 * - exists delegates for both true and false results
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantResourceInternalControllerTest {

    private val api = mock(SysTenantResourceApi::class.java)
    private val controller = SysTenantResourceInternalController(api)

    @Test
    fun getResourceIdsByTenantId_delegates() {
        val ids = setOf("r-1", "r-2")
        whenCalled(api.getResourceIdsByTenantId("t-1")).thenReturn(ids)

        assertSame(ids, controller.getResourceIdsByTenantId("t-1"))
        assertTrue(controller.getResourceIdsByTenantId("t-empty").isEmpty())
        verify(api).getResourceIdsByTenantId("t-1")
    }

    @Test
    fun getTenantIdsByResourceId_delegates() {
        val ids = setOf("t-1", "t-2")
        whenCalled(api.getTenantIdsByResourceId("r-1")).thenReturn(ids)

        assertSame(ids, controller.getTenantIdsByResourceId("r-1"))
        verify(api).getTenantIdsByResourceId("r-1")
    }

    @Test
    fun batchBind_delegates() {
        val ids = listOf("r-1", "r-2", "r-3")
        whenCalled(api.batchBind("t-1", ids)).thenReturn(3)

        assertEquals(3, controller.batchBind("t-1", ids))
        verify(api).batchBind("t-1", ids)
    }

    @Test
    fun unbind_delegates() {
        whenCalled(api.unbind("t-1", "r-1")).thenReturn(true)
        whenCalled(api.unbind("t-1", "missing")).thenReturn(false)

        assertTrue(controller.unbind("t-1", "r-1"))
        assertFalse(controller.unbind("t-1", "missing"))
        verify(api).unbind("t-1", "r-1")
        verify(api).unbind("t-1", "missing")
    }

    @Test
    fun exists_delegates() {
        whenCalled(api.exists("t-1", "r-1")).thenReturn(true)
        whenCalled(api.exists("t-1", "missing")).thenReturn(false)

        assertTrue(controller.exists("t-1", "r-1"))
        assertFalse(controller.exists("t-1", "missing"))
        verify(api).exists("t-1", "r-1")
        verify(api).exists("t-1", "missing")
    }
}
