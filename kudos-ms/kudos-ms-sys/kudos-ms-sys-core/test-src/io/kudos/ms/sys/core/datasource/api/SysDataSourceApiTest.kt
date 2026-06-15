package io.kudos.ms.sys.core.datasource.api

import io.kudos.ms.sys.common.datasource.vo.SysDataSourceCacheEntry
import io.kudos.ms.sys.core.datasource.service.iservice.ISysDataSourceService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pure unit test for [SysDataSourceApi]: getDataSourceFromCache delegates to the service unchanged,
 * including the nullable atomicServiceCode argument. Service mocked with Mockito; no Spring container.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysDataSourceApiTest {

    private val service = mock(ISysDataSourceService::class.java)
    private val api = SysDataSourceApi(service)

    private fun entry(id: String) = SysDataSourceCacheEntry(
        id = id,
        name = "ds-$id",
        subSystemCode = "sub",
        microServiceCode = null,
        tenantId = "t1",
        url = "jdbc:h2:mem:test",
        username = "sa",
        password = null,
        initialSize = null,
        maxActive = null,
        maxIdle = null,
        minIdle = null,
        maxWait = null,
        maxAge = null,
        remark = null,
        active = true,
        builtIn = false,
    )

    @Test
    fun getDataSourceFromCache_delegates_withAtomicServiceCode() {
        val e = entry("d1")
        whenCalled(service.getDataSourceFromCache("t1", "svc-a")).thenReturn(e)
        assertEquals(e, api.getDataSourceFromCache("t1", "svc-a"))
        verify(service).getDataSourceFromCache("t1", "svc-a")
    }

    @Test
    fun getDataSourceFromCache_delegates_nullAtomicServiceCode() {
        val e = entry("d2")
        whenCalled(service.getDataSourceFromCache("t2", null)).thenReturn(e)
        assertEquals(e, api.getDataSourceFromCache("t2", null))
        verify(service).getDataSourceFromCache("t2", null)
    }

    @Test
    fun getDataSourceFromCache_returnsNullOnMiss() {
        whenCalled(service.getDataSourceFromCache("none", "svc")).thenReturn(null)
        assertNull(api.getDataSourceFromCache("none", "svc"))
    }
}
