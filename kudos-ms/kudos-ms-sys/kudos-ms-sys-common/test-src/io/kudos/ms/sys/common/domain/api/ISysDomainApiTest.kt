package io.kudos.ms.sys.common.domain.api

import io.kudos.ms.sys.common.domain.vo.SysDomainCacheEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * test for ISysDomainApi
 *
 * Covers getDomainByName through a fake implementation, including the found and
 * not-found (null) branches.
 *
 * @author K
 * @since 1.0.0
 */
internal class ISysDomainApiTest {

    private class FakeApi : ISysDomainApi {
        var lastDomainName: String? = null

        override fun getDomainByName(domainName: String): SysDomainCacheEntry? {
            lastDomainName = domainName
            return if (domainName == "example.com") {
                SysDomainCacheEntry(
                    id = "d-1", domain = "example.com", systemCode = "sys",
                    tenantId = "t-1", remark = null, active = true, builtIn = false,
                )
            } else null
        }
    }

    @Test
    fun found() {
        val api = FakeApi()
        val entry = api.getDomainByName("example.com")
        assertEquals("example.com", api.lastDomainName)
        assertEquals("d-1", entry?.id)
        assertEquals("example.com", entry?.domain)
    }

    @Test
    fun notFound() {
        val api = FakeApi()
        assertNull(api.getDomainByName("missing.com"))
        assertEquals("missing.com", api.lastDomainName)
    }

}
