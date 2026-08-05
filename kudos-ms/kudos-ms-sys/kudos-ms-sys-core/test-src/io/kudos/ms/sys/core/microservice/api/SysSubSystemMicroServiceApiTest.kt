package io.kudos.ms.sys.core.microservice.api

import io.kudos.ms.sys.core.microservice.service.iservice.ISysSubSystemMicroServiceService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure unit test for [SysSubSystemMicroServiceApi]: verifies pure delegation to
 * [ISysSubSystemMicroServiceService] for every method. Service mocked with Mockito; no Spring container.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysSubSystemMicroServiceApiTest {

    private val service = mock(ISysSubSystemMicroServiceService::class.java)
    private val api = SysSubSystemMicroServiceApi(service)

    @Test
    fun getMicroServiceCodesBySubSystemCode_delegates() {
        whenCalled(service.getMicroServiceCodesBySubSystemCode("sub")).thenReturn(setOf("m1", "m2"))
        assertEquals(setOf("m1", "m2"), api.getMicroServiceCodesBySubSystemCode("sub"))
        verify(service).getMicroServiceCodesBySubSystemCode("sub")
    }

    @Test
    fun getMicroServiceCodesBySubSystemCode_emptySet() {
        whenCalled(service.getMicroServiceCodesBySubSystemCode("none")).thenReturn(emptySet())
        assertTrue(api.getMicroServiceCodesBySubSystemCode("none").isEmpty())
    }

    @Test
    fun getSubSystemCodesByMicroServiceCode_delegates() {
        whenCalled(service.getSubSystemCodesByMicroServiceCode("ms")).thenReturn(setOf("s1"))
        assertEquals(setOf("s1"), api.getSubSystemCodesByMicroServiceCode("ms"))
        verify(service).getSubSystemCodesByMicroServiceCode("ms")
    }

    @Test
    fun batchBind_delegates_returnsCount() {
        val codes = listOf("m1", "m2", "m3")
        whenCalled(service.batchBind("sub", codes)).thenReturn(3)
        assertEquals(3, api.batchBind("sub", codes))
        verify(service).batchBind("sub", codes)
    }

    @Test
    fun batchBind_emptyCollection_delegatesZero() {
        whenCalled(service.batchBind("sub", emptyList())).thenReturn(0)
        assertEquals(0, api.batchBind("sub", emptyList()))
    }

    @Test
    fun unbind_delegates_trueAndFalse() {
        whenCalled(service.unbind("sub", "m1")).thenReturn(true)
        whenCalled(service.unbind("sub", "m2")).thenReturn(false)
        assertTrue(api.unbind("sub", "m1"))
        assertFalse(api.unbind("sub", "m2"))
    }

    @Test
    fun exists_delegates_trueAndFalse() {
        whenCalled(service.exists("sub", "m1")).thenReturn(true)
        whenCalled(service.exists("sub", "m2")).thenReturn(false)
        assertTrue(api.exists("sub", "m1"))
        assertFalse(api.exists("sub", "m2"))
    }
}
