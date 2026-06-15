package io.kudos.ms.sys.core.microservice.api

import io.kudos.ms.sys.common.microservice.vo.SysMicroServiceCacheEntry
import io.kudos.ms.sys.core.microservice.service.iservice.ISysMicroServiceService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure unit test for [SysMicroServiceApi]: verifies it delegates every method to the underlying
 * [ISysMicroServiceService] unchanged (no transformation). Service is mocked with Mockito; no Spring container.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysMicroServiceApiTest {

    private val service = mock(ISysMicroServiceService::class.java)
    private val api = SysMicroServiceApi(service)

    private fun entry(code: String) = SysMicroServiceCacheEntry(
        id = code,
        code = code,
        name = "name-$code",
        context = "/$code",
        atomicService = false,
        parentCode = null,
        remark = null,
        active = true,
        builtIn = false,
    )

    @Test
    fun getMicroServiceFromCache_delegates_hitAndMiss() {
        val e = entry("c1")
        whenCalled(service.getMicroServiceFromCache("c1")).thenReturn(e)
        assertEquals(e, api.getMicroServiceFromCache("c1"))
        assertNull(api.getMicroServiceFromCache("missing"))
        verify(service).getMicroServiceFromCache("c1")
    }

    @Test
    fun getAllMicroServicesFromCache_delegates() {
        val list = listOf(entry("a"), entry("b"))
        whenCalled(service.getAllMicroServicesFromCache()).thenReturn(list)
        assertEquals(list, api.getAllMicroServicesFromCache())
    }

    @Test
    fun getMicroServicesExcludeAtomicFromCache_delegates_emptyOk() {
        whenCalled(service.getMicroServicesExcludeAtomicFromCache()).thenReturn(emptyList())
        assertTrue(api.getMicroServicesExcludeAtomicFromCache().isEmpty())
    }

    @Test
    fun getAtomicServicesFromCache_delegates() {
        val list = listOf(entry("at"))
        whenCalled(service.getAtomicServicesFromCache()).thenReturn(list)
        assertEquals(list, api.getAtomicServicesFromCache())
    }

    @Test
    fun getSubMicroServicesFromCache_delegates_withParentArg() {
        val list = listOf(entry("sub"))
        whenCalled(service.getSubMicroServicesFromCache("parent")).thenReturn(list)
        assertEquals(list, api.getSubMicroServicesFromCache("parent"))
        verify(service).getSubMicroServicesFromCache("parent")
    }

    @Test
    fun getAtomicServicesByParentCodeFromCache_delegates_withParentArg() {
        val list = listOf(entry("subAtomic"))
        whenCalled(service.getAtomicServicesByParentCodeFromCache("parent")).thenReturn(list)
        assertEquals(list, api.getAtomicServicesByParentCodeFromCache("parent"))
        verify(service).getAtomicServicesByParentCodeFromCache("parent")
    }

    @Test
    fun updateActive_delegates_trueAndFalse() {
        whenCalled(service.updateActive("c1", true)).thenReturn(true)
        whenCalled(service.updateActive("c2", false)).thenReturn(false)
        assertTrue(api.updateActive("c1", true))
        assertEquals(false, api.updateActive("c2", false))
        verify(service).updateActive("c1", true)
        verify(service).updateActive("c2", false)
    }
}
