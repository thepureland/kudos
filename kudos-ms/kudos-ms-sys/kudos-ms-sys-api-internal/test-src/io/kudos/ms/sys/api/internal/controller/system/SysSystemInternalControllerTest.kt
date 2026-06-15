package io.kudos.ms.sys.api.internal.controller.system

import io.kudos.ms.sys.common.system.vo.SysSystemCacheEntry
import io.kudos.ms.sys.core.system.api.SysSystemApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for SysSystemInternalController (pure delegation, core api mocked with Mockito, no Spring container)
 *
 * Coverage:
 * - getSystemFromCache delegates (hit and null miss)
 * - getAllSystemsFromCache / getSystemsExcludeSubSystemFromCache delegate (incl. empty)
 * - updateActive delegates for both true and false results
 * - getSubSystemsFromCache delegates by systemCode
 *
 * @author K
 * @since 1.0.0
 */
internal class SysSystemInternalControllerTest {

    private val api = mock(SysSystemApi::class.java)
    private val controller = SysSystemInternalController(api)

    @Test
    fun getSystemFromCache_delegates() {
        val entry = mock(SysSystemCacheEntry::class.java)
        whenCalled(api.getSystemFromCache("sys-a")).thenReturn(entry)
        whenCalled(api.getSystemFromCache("sys-missing")).thenReturn(null)

        assertSame(entry, controller.getSystemFromCache("sys-a"))
        assertNull(controller.getSystemFromCache("sys-missing"))
        verify(api).getSystemFromCache("sys-a")
        verify(api).getSystemFromCache("sys-missing")
    }

    @Test
    fun getAllSystemsFromCache_delegates() {
        val all = listOf(mock(SysSystemCacheEntry::class.java))
        whenCalled(api.getAllSystemsFromCache()).thenReturn(all)

        assertSame(all, controller.getAllSystemsFromCache())
        verify(api).getAllSystemsFromCache()
    }

    @Test
    fun getSystemsExcludeSubSystemFromCache_delegates() {
        whenCalled(api.getSystemsExcludeSubSystemFromCache()).thenReturn(emptyList())

        assertTrue(controller.getSystemsExcludeSubSystemFromCache().isEmpty())
        verify(api).getSystemsExcludeSubSystemFromCache()
    }

    @Test
    fun updateActive_delegates() {
        whenCalled(api.updateActive("sys-a", true)).thenReturn(true)
        whenCalled(api.updateActive("sys-b", false)).thenReturn(false)

        assertTrue(controller.updateActive("sys-a", true))
        assertFalse(controller.updateActive("sys-b", false))
        verify(api).updateActive("sys-a", true)
        verify(api).updateActive("sys-b", false)
    }

    @Test
    fun getSubSystemsFromCache_delegates() {
        val subs = listOf(mock(SysSystemCacheEntry::class.java))
        whenCalled(api.getSubSystemsFromCache("sys-a")).thenReturn(subs)

        assertSame(subs, controller.getSubSystemsFromCache("sys-a"))
        assertTrue(controller.getSubSystemsFromCache("sys-leaf").isEmpty())
        verify(api).getSubSystemsFromCache("sys-a")
    }
}
