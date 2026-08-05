package io.kudos.ms.sys.core.tenant.api

import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantSystemService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * test for SysTenantSystemApi (pure delegation, service mocked with Mockito, no Spring container)
 *
 * Coverage: all 9 delegate methods forward arguments and return values unchanged
 * (searchSystemCodesByTenantId / searchTenantIdsBySystemCode / groupingSystemCodesByTenantIds (incl. null) /
 * groupingTenantIdsBySystemCodes (incl. null) / batchBind / unbind / exists / deleteByTenantId / batchDeleteByTenantIds).
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantSystemApiTest {

    private val service = mock(ISysTenantSystemService::class.java)
    private val api = SysTenantSystemApi(service)

    @Test
    fun searchSystemCodesByTenantId_delegates() {
        whenCalled(service.searchSystemCodesByTenantId("t-1")).thenReturn(setOf("sys-a"))
        assertEquals(setOf("sys-a"), api.searchSystemCodesByTenantId("t-1"))
        verify(service).searchSystemCodesByTenantId("t-1")
    }

    @Test
    fun searchTenantIdsBySystemCode_delegates() {
        whenCalled(service.searchTenantIdsBySystemCode("sys-a")).thenReturn(setOf("t-1"))
        assertEquals(setOf("t-1"), api.searchTenantIdsBySystemCode("sys-a"))
    }

    @Test
    fun groupingSystemCodesByTenantIds_delegates_includingNull() {
        val grouped = mapOf("t-1" to listOf("sys-a", "sys-b"))
        whenCalled(service.groupingSystemCodesByTenantIds(listOf("t-1"))).thenReturn(grouped)
        assertEquals(grouped, api.groupingSystemCodesByTenantIds(listOf("t-1")))

        val all = mapOf("t-1" to listOf("sys-a"), "t-2" to listOf("sys-b"))
        whenCalled(service.groupingSystemCodesByTenantIds(null)).thenReturn(all)
        assertEquals(all, api.groupingSystemCodesByTenantIds(null))
    }

    @Test
    fun groupingTenantIdsBySystemCodes_delegates_includingNull() {
        val grouped = mapOf("sys-a" to listOf("t-1"))
        whenCalled(service.groupingTenantIdsBySystemCodes(listOf("sys-a"))).thenReturn(grouped)
        assertEquals(grouped, api.groupingTenantIdsBySystemCodes(listOf("sys-a")))

        val all = mapOf("sys-a" to listOf("t-1", "t-2"))
        whenCalled(service.groupingTenantIdsBySystemCodes(null)).thenReturn(all)
        assertEquals(all, api.groupingTenantIdsBySystemCodes(null))
    }

    @Test
    fun batchBind_delegates() {
        whenCalled(service.batchBind("t-1", listOf("sys-a", "sys-b"))).thenReturn(2)
        assertEquals(2, api.batchBind("t-1", listOf("sys-a", "sys-b")))
        assertEquals(0, api.batchBind("t-1", emptyList()))
    }

    @Test
    fun unbind_delegates() {
        whenCalled(service.unbind("t-1", "sys-a")).thenReturn(true)
        assertTrue(api.unbind("t-1", "sys-a"))
        assertFalse(api.unbind("t-1", "missing"))
    }

    @Test
    fun exists_delegates() {
        whenCalled(service.exists("t-1", "sys-a")).thenReturn(true)
        assertTrue(api.exists("t-1", "sys-a"))
        assertFalse(api.exists("t-1", "missing"))
    }

    @Test
    fun deleteByTenantId_delegates() {
        whenCalled(service.deleteByTenantId("t-1")).thenReturn(3)
        assertEquals(3, api.deleteByTenantId("t-1"))
    }

    @Test
    fun batchDeleteByTenantIds_delegates() {
        whenCalled(service.batchDeleteByTenantIds(listOf("t-1", "t-2"))).thenReturn(5)
        assertEquals(5, api.batchDeleteByTenantIds(listOf("t-1", "t-2")))
        assertEquals(0, api.batchDeleteByTenantIds(emptyList()))
    }
}
