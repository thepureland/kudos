package io.kudos.ms.sys.core.param.api

import io.kudos.ms.sys.common.param.vo.SysParamCacheEntry
import io.kudos.ms.sys.core.param.service.iservice.ISysParamService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pure unit test for [SysParamApi]: getParam delegates to the service's getParamFromCache, mapping the
 * (paramName, atomicServiceCode) arguments to (atomicServiceCode, paramName) order. Service mocked with
 * Mockito; no Spring container.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysParamApiTest {

    private val service = mock(ISysParamService::class.java)
    private val api = SysParamApi(service)

    @Test
    fun getParam_delegates_argumentOrderSwapped() {
        val entry = SysParamCacheEntry(
            id = "p1",
            paramName = "max.size",
            paramValue = "100",
            defaultValue = "50",
            atomicServiceCode = "svc-a",
            orderNum = 1,
            remark = null,
            active = true,
            builtIn = false,
        )
        // api.getParam(paramName, atomicServiceCode) -> service.getParamFromCache(atomicServiceCode, paramName)
        whenCalled(service.getParamFromCache("svc-a", "max.size")).thenReturn(entry)
        assertEquals(entry, api.getParam("max.size", "svc-a"))
        verify(service).getParamFromCache("svc-a", "max.size")
    }

    @Test
    fun getParam_returnsNullOnMiss() {
        whenCalled(service.getParamFromCache("svc-a", "missing")).thenReturn(null)
        assertNull(api.getParam("missing", "svc-a"))
    }
}
