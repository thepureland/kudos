package io.kudos.ms.sys.api.internal.controller.microservice

import io.kudos.ms.sys.core.microservice.api.SysSubSystemMicroServiceApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * test for SysSubSystemMicroServiceInternalController (pure delegation, core api mocked with Mockito,
 * no Spring container)
 *
 * Coverage:
 * - getMicroServiceCodesBySubSystemCode / getSubSystemCodesByMicroServiceCode delegate (incl. empty sets)
 * - batchBind delegates (non-empty and empty collections -> 0)
 * - unbind / exists delegate for both true and false results
 *
 * @author K
 * @since 1.0.0
 */
internal class SysSubSystemMicroServiceInternalControllerTest {

    private val api = mock(SysSubSystemMicroServiceApi::class.java)
    private val controller = SysSubSystemMicroServiceInternalController(api)

    @Test
    fun getMicroServiceCodesBySubSystemCode_delegates() {
        whenCalled(api.getMicroServiceCodesBySubSystemCode("sub-a")).thenReturn(setOf("ms-1", "ms-2"))

        assertEquals(setOf("ms-1", "ms-2"), controller.getMicroServiceCodesBySubSystemCode("sub-a"))
        assertTrue(controller.getMicroServiceCodesBySubSystemCode("sub-none").isEmpty())
        verify(api).getMicroServiceCodesBySubSystemCode("sub-a")
    }

    @Test
    fun getSubSystemCodesByMicroServiceCode_delegates() {
        whenCalled(api.getSubSystemCodesByMicroServiceCode("ms-1")).thenReturn(setOf("sub-a"))

        assertEquals(setOf("sub-a"), controller.getSubSystemCodesByMicroServiceCode("ms-1"))
        assertTrue(controller.getSubSystemCodesByMicroServiceCode("ms-none").isEmpty())
        verify(api).getSubSystemCodesByMicroServiceCode("ms-1")
    }

    @Test
    fun batchBind_delegates() {
        whenCalled(api.batchBind("sub-a", listOf("ms-1", "ms-2"))).thenReturn(2)

        assertEquals(2, controller.batchBind("sub-a", listOf("ms-1", "ms-2")))
        assertEquals(0, controller.batchBind("sub-a", emptyList()))
        verify(api).batchBind("sub-a", listOf("ms-1", "ms-2"))
        verify(api).batchBind("sub-a", emptyList())
    }

    @Test
    fun unbind_delegates() {
        whenCalled(api.unbind("sub-a", "ms-1")).thenReturn(true)

        assertTrue(controller.unbind("sub-a", "ms-1"))
        assertFalse(controller.unbind("sub-a", "ms-none"))
        verify(api).unbind("sub-a", "ms-1")
    }

    @Test
    fun exists_delegates() {
        whenCalled(api.exists("sub-a", "ms-1")).thenReturn(true)

        assertTrue(controller.exists("sub-a", "ms-1"))
        assertFalse(controller.exists("sub-a", "ms-none"))
        verify(api).exists("sub-a", "ms-1")
    }
}
