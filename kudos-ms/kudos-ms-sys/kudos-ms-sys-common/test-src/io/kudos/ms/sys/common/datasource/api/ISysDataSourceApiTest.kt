package io.kudos.ms.sys.common.datasource.api

import io.kudos.ms.sys.common.datasource.vo.SysDataSourceCacheEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * test for ISysDataSourceApi
 *
 * Covers the default parameter value of getDataSourceFromCache (atomicServiceCode = null)
 * via a fake implementation, plus explicit argument passing.
 *
 * @author K
 * @since 1.0.0
 */
internal class ISysDataSourceApiTest {

    private class FakeApi : ISysDataSourceApi {
        var lastTenantId: String? = null
        var lastAtomicServiceCode: String? = "INIT"

        override fun getDataSourceFromCache(tenantId: String, atomicServiceCode: String?): SysDataSourceCacheEntry? {
            lastTenantId = tenantId
            lastAtomicServiceCode = atomicServiceCode
            if (tenantId == "missing") return null
            return SysDataSourceCacheEntry(
                id = "ds-1", name = "ds1", subSystemCode = "sys", microServiceCode = atomicServiceCode,
                tenantId = tenantId, url = "jdbc:h2:mem:test", username = "sa", password = null,
                initialSize = null, maxActive = null, maxIdle = null, minIdle = null,
                maxWait = null, maxAge = null, remark = null, active = true, builtIn = false,
            )
        }
    }

    @Test
    fun defaultAtomicServiceCodeIsNull() {
        val api = FakeApi()
        val entry = api.getDataSourceFromCache("t-1")
        assertEquals("t-1", api.lastTenantId)
        assertNull(api.lastAtomicServiceCode)
        assertEquals("ds-1", entry?.id)
    }

    @Test
    fun explicitAtomicServiceCode() {
        val api = FakeApi()
        api.getDataSourceFromCache("t-1", "sys")
        assertEquals("sys", api.lastAtomicServiceCode)
    }

    @Test
    fun notFoundReturnsNull() {
        val api = FakeApi()
        assertNull(api.getDataSourceFromCache("missing"))
    }

}
