package io.kudos.ms.sys.api.internal.controller.microservice

import io.kudos.ms.sys.common.microservice.vo.SysMicroServiceCacheEntry
import io.kudos.ms.sys.core.microservice.api.SysMicroServiceApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for SysMicroServiceInternalController (pure delegation, core api mocked with Mockito, no Spring container)
 *
 * Coverage:
 * - getMicroServiceFromCache delegates (hit and null miss)
 * - getAllMicroServicesFromCache / getMicroServicesExcludeAtomicFromCache / getAtomicServicesFromCache delegate
 *   (incl. empty results)
 * - getSubMicroServicesFromCache / getAtomicServicesByParentCodeFromCache delegate by parentCode
 * - updateActive delegates for both true and false results
 *
 * @author K
 * @since 1.0.0
 */
internal class SysMicroServiceInternalControllerTest {

    private val api = mock(SysMicroServiceApi::class.java)
    private val controller = SysMicroServiceInternalController(api)

    @Test
    fun getMicroServiceFromCache_delegates() {
        val entry = mock(SysMicroServiceCacheEntry::class.java)
        whenCalled(api.getMicroServiceFromCache("ms-a")).thenReturn(entry)

        assertSame(entry, controller.getMicroServiceFromCache("ms-a"))
        assertNull(controller.getMicroServiceFromCache("ms-missing"))
        verify(api).getMicroServiceFromCache("ms-a")
    }

    @Test
    fun getAllMicroServicesFromCache_delegates() {
        val all = listOf(mock(SysMicroServiceCacheEntry::class.java))
        whenCalled(api.getAllMicroServicesFromCache()).thenReturn(all)

        assertSame(all, controller.getAllMicroServicesFromCache())
        verify(api).getAllMicroServicesFromCache()
    }

    @Test
    fun getMicroServicesExcludeAtomicFromCache_delegates() {
        val services = listOf(mock(SysMicroServiceCacheEntry::class.java))
        whenCalled(api.getMicroServicesExcludeAtomicFromCache()).thenReturn(services)

        assertSame(services, controller.getMicroServicesExcludeAtomicFromCache())
        verify(api).getMicroServicesExcludeAtomicFromCache()
    }

    @Test
    fun getAtomicServicesFromCache_delegates() {
        whenCalled(api.getAtomicServicesFromCache()).thenReturn(emptyList())

        assertTrue(controller.getAtomicServicesFromCache().isEmpty())
        verify(api).getAtomicServicesFromCache()
    }

    @Test
    fun getSubMicroServicesFromCache_delegates() {
        val subs = listOf(mock(SysMicroServiceCacheEntry::class.java))
        whenCalled(api.getSubMicroServicesFromCache("parent-a")).thenReturn(subs)

        assertSame(subs, controller.getSubMicroServicesFromCache("parent-a"))
        assertTrue(controller.getSubMicroServicesFromCache("parent-leaf").isEmpty())
        verify(api).getSubMicroServicesFromCache("parent-a")
    }

    @Test
    fun getAtomicServicesByParentCodeFromCache_delegates() {
        val atomics = listOf(mock(SysMicroServiceCacheEntry::class.java))
        whenCalled(api.getAtomicServicesByParentCodeFromCache("parent-a")).thenReturn(atomics)

        assertSame(atomics, controller.getAtomicServicesByParentCodeFromCache("parent-a"))
        assertTrue(controller.getAtomicServicesByParentCodeFromCache("parent-leaf").isEmpty())
        verify(api).getAtomicServicesByParentCodeFromCache("parent-a")
    }

    @Test
    fun updateActive_delegates() {
        whenCalled(api.updateActive("ms-a", true)).thenReturn(true)
        whenCalled(api.updateActive("ms-b", false)).thenReturn(false)

        assertTrue(controller.updateActive("ms-a", true))
        assertFalse(controller.updateActive("ms-b", false))
        verify(api).updateActive("ms-a", true)
        verify(api).updateActive("ms-b", false)
    }
}
