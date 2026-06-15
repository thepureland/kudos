package io.kudos.ms.sys.api.internal.controller.datasource

import io.kudos.ms.sys.common.datasource.vo.SysDataSourceCacheEntry
import io.kudos.ms.sys.core.datasource.api.SysDataSourceApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * test for SysDataSourceInternalController (pure delegation, core api mocked with Mockito, no Spring container)
 *
 * Coverage:
 * - getDataSourceFromCache delegates with non-null and null atomicServiceCode (hit and null miss)
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDataSourceInternalControllerTest {

    private val api = mock(SysDataSourceApi::class.java)
    private val controller = SysDataSourceInternalController(api)

    @Test
    fun getDataSourceFromCache_delegates() {
        val entry = mock(SysDataSourceCacheEntry::class.java)
        whenCalled(api.getDataSourceFromCache("tenant-1", "as-a")).thenReturn(entry)
        whenCalled(api.getDataSourceFromCache("tenant-1", null)).thenReturn(entry)

        assertSame(entry, controller.getDataSourceFromCache("tenant-1", "as-a"))
        assertSame(entry, controller.getDataSourceFromCache("tenant-1", null))
        assertNull(controller.getDataSourceFromCache("tenant-missing", "as-a"))
        verify(api).getDataSourceFromCache("tenant-1", "as-a")
        verify(api).getDataSourceFromCache("tenant-1", null)
    }
}
