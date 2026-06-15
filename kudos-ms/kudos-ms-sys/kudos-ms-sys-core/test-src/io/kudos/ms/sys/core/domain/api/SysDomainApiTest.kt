package io.kudos.ms.sys.core.domain.api

import io.kudos.ms.sys.common.domain.vo.SysDomainCacheEntry
import io.kudos.ms.sys.core.domain.service.iservice.ISysDomainService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pure unit test for [SysDomainApi]: getDomainByName delegates to the service's getDomainFromCache.
 * Service mocked with Mockito; no Spring container.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysDomainApiTest {

    private val service = mock(ISysDomainService::class.java)
    private val api = SysDomainApi(service)

    @Test
    fun getDomainByName_delegatesToGetDomainFromCache_hit() {
        val entry = SysDomainCacheEntry(
            id = "d1",
            domain = "example.com",
            systemCode = "sys",
            tenantId = "t1",
            remark = null,
            active = true,
            builtIn = false,
        )
        whenCalled(service.getDomainFromCache("example.com")).thenReturn(entry)
        assertEquals(entry, api.getDomainByName("example.com"))
        verify(service).getDomainFromCache("example.com")
    }

    @Test
    fun getDomainByName_returnsNullOnMiss() {
        whenCalled(service.getDomainFromCache("missing.com")).thenReturn(null)
        assertNull(api.getDomainByName("missing.com"))
    }
}
