package io.kudos.ms.sys.api.internal.controller.tenant

import io.kudos.ms.sys.core.tenant.api.SysTenantSystemApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for SysTenantSystemInternalController (pure delegation, core api mocked with Mockito, no Spring container)
 *
 * Coverage:
 * - searchSystemCodesByTenantId / searchTenantIdsBySystemCode delegate (incl. empty set)
 * - groupingSystemCodesByTenantIds delegates with a non-null collection and with null
 * - groupingTenantIdsBySystemCodes delegates with a non-null collection and with null
 * - batchBind delegates and returns the affected-row count
 * - unbind / exists delegate for both true and false results
 * - deleteByTenantId / batchDeleteByTenantIds delegate and return the affected-row count
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantSystemInternalControllerTest {

    private val api = mock(SysTenantSystemApi::class.java)
    private val controller = SysTenantSystemInternalController(api)

    @Test
    fun searchSystemCodesByTenantId_delegates() {
        val codes = setOf("sys-a", "sys-b")
        whenCalled(api.searchSystemCodesByTenantId("t-1")).thenReturn(codes)

        assertSame(codes, controller.searchSystemCodesByTenantId("t-1"))
        assertTrue(controller.searchSystemCodesByTenantId("t-empty").isEmpty())
        verify(api).searchSystemCodesByTenantId("t-1")
    }

    @Test
    fun searchTenantIdsBySystemCode_delegates() {
        val ids = setOf("t-1", "t-2")
        whenCalled(api.searchTenantIdsBySystemCode("sys-a")).thenReturn(ids)

        assertSame(ids, controller.searchTenantIdsBySystemCode("sys-a"))
        verify(api).searchTenantIdsBySystemCode("sys-a")
    }

    @Test
    fun groupingSystemCodesByTenantIds_delegates_nonNull() {
        val tenantIds = listOf("t-1", "t-2")
        val grouping = mapOf("t-1" to listOf("sys-a"), "t-2" to listOf("sys-b"))
        whenCalled(api.groupingSystemCodesByTenantIds(tenantIds)).thenReturn(grouping)

        assertSame(grouping, controller.groupingSystemCodesByTenantIds(tenantIds))
        verify(api).groupingSystemCodesByTenantIds(tenantIds)
    }

    @Test
    fun groupingSystemCodesByTenantIds_delegates_null() {
        whenCalled(api.groupingSystemCodesByTenantIds(null)).thenReturn(emptyMap())

        assertTrue(controller.groupingSystemCodesByTenantIds(null).isEmpty())
        verify(api).groupingSystemCodesByTenantIds(null)
    }

    @Test
    fun groupingTenantIdsBySystemCodes_delegates_nonNull() {
        val systemCodes = listOf("sys-a")
        val grouping = mapOf("sys-a" to listOf("t-1", "t-2"))
        whenCalled(api.groupingTenantIdsBySystemCodes(systemCodes)).thenReturn(grouping)

        assertSame(grouping, controller.groupingTenantIdsBySystemCodes(systemCodes))
        verify(api).groupingTenantIdsBySystemCodes(systemCodes)
    }

    @Test
    fun groupingTenantIdsBySystemCodes_delegates_null() {
        whenCalled(api.groupingTenantIdsBySystemCodes(null)).thenReturn(emptyMap())

        assertTrue(controller.groupingTenantIdsBySystemCodes(null).isEmpty())
        verify(api).groupingTenantIdsBySystemCodes(null)
    }

    @Test
    fun batchBind_delegates() {
        val codes = listOf("sys-a", "sys-b")
        whenCalled(api.batchBind("t-1", codes)).thenReturn(2)

        assertEquals(2, controller.batchBind("t-1", codes))
        verify(api).batchBind("t-1", codes)
    }

    @Test
    fun unbind_delegates() {
        whenCalled(api.unbind("t-1", "sys-a")).thenReturn(true)
        whenCalled(api.unbind("t-1", "missing")).thenReturn(false)

        assertTrue(controller.unbind("t-1", "sys-a"))
        assertFalse(controller.unbind("t-1", "missing"))
        verify(api).unbind("t-1", "sys-a")
        verify(api).unbind("t-1", "missing")
    }

    @Test
    fun exists_delegates() {
        whenCalled(api.exists("t-1", "sys-a")).thenReturn(true)
        whenCalled(api.exists("t-1", "missing")).thenReturn(false)

        assertTrue(controller.exists("t-1", "sys-a"))
        assertFalse(controller.exists("t-1", "missing"))
        verify(api).exists("t-1", "sys-a")
        verify(api).exists("t-1", "missing")
    }

    @Test
    fun deleteByTenantId_delegates() {
        whenCalled(api.deleteByTenantId("t-1")).thenReturn(5)

        assertEquals(5, controller.deleteByTenantId("t-1"))
        verify(api).deleteByTenantId("t-1")
    }

    @Test
    fun batchDeleteByTenantIds_delegates() {
        val ids = listOf("t-1", "t-2")
        whenCalled(api.batchDeleteByTenantIds(ids)).thenReturn(7)

        assertEquals(7, controller.batchDeleteByTenantIds(ids))
        verify(api).batchDeleteByTenantIds(ids)
    }
}
