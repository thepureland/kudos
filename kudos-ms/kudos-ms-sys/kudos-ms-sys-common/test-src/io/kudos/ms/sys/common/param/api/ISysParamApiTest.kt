package io.kudos.ms.sys.common.param.api

import io.kudos.ms.sys.common.param.vo.SysParamCacheEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * test for ISysParamApi
 *
 * Covers the default parameter value of getParam (atomicServiceCode = "default")
 * via a fake implementation, plus explicit argument passing and not-found.
 *
 * @author K
 * @since 1.0.0
 */
internal class ISysParamApiTest {

    private class FakeApi : ISysParamApi {
        var lastParamName: String? = null
        var lastAtomicServiceCode: String? = null

        override fun getParam(paramName: String, atomicServiceCode: String): SysParamCacheEntry? {
            lastParamName = paramName
            lastAtomicServiceCode = atomicServiceCode
            if (paramName == "missing") return null
            return SysParamCacheEntry(
                id = "p-1", paramName = paramName, paramValue = "100", defaultValue = null,
                atomicServiceCode = atomicServiceCode, orderNum = null, remark = null,
                active = true, builtIn = false,
            )
        }
    }

    @Test
    fun defaultAtomicServiceCode() {
        val api = FakeApi()
        val entry = api.getParam("max.size")
        assertEquals("max.size", api.lastParamName)
        assertEquals("default", api.lastAtomicServiceCode)
        assertEquals("default", entry?.atomicServiceCode)
    }

    @Test
    fun explicitAtomicServiceCode() {
        val api = FakeApi()
        api.getParam("max.size", "sys")
        assertEquals("sys", api.lastAtomicServiceCode)
    }

    @Test
    fun notFoundReturnsNull() {
        val api = FakeApi()
        assertNull(api.getParam("missing"))
    }

}
