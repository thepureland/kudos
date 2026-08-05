package io.kudos.ms.sys.common.outline.api

import io.kudos.ms.sys.common.outline.vo.SysOutLineCacheEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for ISysOutLineApi
 *
 * Covers listOutLines through a fake implementation, including the default tenantId argument
 * (null = platform-level) and explicit tenant lookup.
 *
 * @author K
 * @since 1.0.0
 */
internal class ISysOutLineApiTest {

    private class FakeApi : ISysOutLineApi {
        var lastSystemCode: String? = null
        var lastTenantId: String? = "untouched"

        override fun listOutLines(systemCode: String, tenantId: String?): List<SysOutLineCacheEntry> {
            lastSystemCode = systemCode
            lastTenantId = tenantId
            return listOf(
                SysOutLineCacheEntry(
                    id = "o-1", name = "wl", host = "example.com", port = 443, protocol = "https",
                    systemCode = systemCode, tenantId = tenantId, remark = null, active = true, builtIn = false,
                )
            )
        }
    }

    @Test
    fun defaultTenantIdIsNull() {
        val api = FakeApi()
        val list = api.listOutLines("sys")
        assertEquals("sys", api.lastSystemCode)
        assertNull(api.lastTenantId)
        assertEquals("sys", list.single().systemCode)
    }

    @Test
    fun explicitTenantId() {
        val api = FakeApi()
        val list = api.listOutLines("sys", "t-1")
        assertEquals("t-1", api.lastTenantId)
        assertEquals("t-1", list.single().tenantId)
        assertTrue(list.single().active)
    }

}
