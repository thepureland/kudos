package io.kudos.ms.sys.core.system.api

import io.kudos.ms.sys.common.system.vo.SysSystemCacheEntry
import io.kudos.ms.sys.core.system.service.iservice.ISysSystemService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysSystemApi (pure delegation, service mocked with Mockito, no Spring container)
 *
 * Coverage: all 5 delegate methods forward arguments and return values unchanged
 * (getSystemFromCache (hit and null) / getAllSystemsFromCache / getSystemsExcludeSubSystemFromCache /
 * updateActive / getSubSystemsFromCache).
 *
 * @author K
 * @since 1.0.0
 */
internal class SysSystemApiTest {

    private val service = mock(ISysSystemService::class.java)
    private val api = SysSystemApi(service)

    private fun entry(code: String, subSystem: Boolean = false) = SysSystemCacheEntry(
        id = code,
        code = code,
        name = "name-$code",
        subSystem = subSystem,
        parentCode = null,
        remark = null,
        active = true,
        builtIn = false,
    )

    @Test
    fun getSystemFromCache_delegates() {
        val system = entry("sys-a")
        whenCalled(service.getSystemFromCache("sys-a")).thenReturn(system)
        assertEquals(system, api.getSystemFromCache("sys-a"))
        assertNull(api.getSystemFromCache("missing"))
        verify(service).getSystemFromCache("sys-a")
    }

    @Test
    fun getAllSystemsFromCache_delegates() {
        val list = listOf(entry("sys-a"), entry("sub-b", subSystem = true))
        whenCalled(service.getAllSystemsFromCache()).thenReturn(list)
        assertEquals(list, api.getAllSystemsFromCache())
    }

    @Test
    fun getSystemsExcludeSubSystemFromCache_delegates() {
        val list = listOf(entry("sys-a"))
        whenCalled(service.getSystemsExcludeSubSystemFromCache()).thenReturn(list)
        assertEquals(list, api.getSystemsExcludeSubSystemFromCache())
    }

    @Test
    fun updateActive_delegates() {
        whenCalled(service.updateActive("sys-a", false)).thenReturn(true)
        assertTrue(api.updateActive("sys-a", false))
        assertFalse(api.updateActive("missing", true))
    }

    @Test
    fun getSubSystemsFromCache_delegates() {
        val subs = listOf(entry("sub-b", subSystem = true))
        whenCalled(service.getSubSystemsFromCache("sys-a")).thenReturn(subs)
        assertEquals(subs, api.getSubSystemsFromCache("sys-a"))
        assertTrue(api.getSubSystemsFromCache("missing").isEmpty())
    }
}
