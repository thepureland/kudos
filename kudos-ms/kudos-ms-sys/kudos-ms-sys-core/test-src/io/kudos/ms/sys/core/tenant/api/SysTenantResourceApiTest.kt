package io.kudos.ms.sys.core.tenant.api

import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantResourceService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * test for SysTenantResourceApi (pure delegation, service mocked with Mockito, no Spring container)
 *
 * Coverage: all 5 delegate methods forward arguments and return values unchanged
 * (getResourceIdsByTenantId / getTenantIdsByResourceId / batchBind / unbind / exists).
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantResourceApiTest {

    private val service = mock(ISysTenantResourceService::class.java)
    private val api = SysTenantResourceApi(service)

    @Test
    fun getResourceIdsByTenantId_delegates() {
        whenCalled(service.getResourceIdsByTenantId("t-1")).thenReturn(setOf("r-1", "r-2"))
        assertEquals(setOf("r-1", "r-2"), api.getResourceIdsByTenantId("t-1"))
        verify(service).getResourceIdsByTenantId("t-1")
    }

    @Test
    fun getTenantIdsByResourceId_delegates() {
        whenCalled(service.getTenantIdsByResourceId("r-1")).thenReturn(setOf("t-1"))
        assertEquals(setOf("t-1"), api.getTenantIdsByResourceId("r-1"))
        assertTrue(api.getTenantIdsByResourceId("missing").isEmpty())
    }

    @Test
    fun batchBind_delegates() {
        whenCalled(service.batchBind("t-1", listOf("r-1"))).thenReturn(1)
        assertEquals(1, api.batchBind("t-1", listOf("r-1")))
        assertEquals(0, api.batchBind("t-1", emptyList()))
    }

    @Test
    fun unbind_delegates() {
        whenCalled(service.unbind("t-1", "r-1")).thenReturn(true)
        assertTrue(api.unbind("t-1", "r-1"))
        assertFalse(api.unbind("t-1", "missing"))
    }

    @Test
    fun exists_delegates() {
        whenCalled(service.exists("t-1", "r-1")).thenReturn(true)
        assertTrue(api.exists("t-1", "r-1"))
        assertFalse(api.exists("t-1", "missing"))
    }
}
