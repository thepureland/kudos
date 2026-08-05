package io.kudos.ms.sys.core.tenant.api

import io.kudos.ms.sys.common.tenant.vo.SysTenantCacheEntry
import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysTenantApi (pure delegation, service mocked with Mockito, no Spring container)
 *
 * Coverage:
 * - getTenantFromCache delegates to the service (hit and null miss)
 * - getTenantsFromCacheByIds delegates unchanged (incl. empty input)
 * - getTenantsBySubSystemCode delegates then keeps only active tenants
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantApiTest {

    private val service = mock(ISysTenantService::class.java)
    private val api = SysTenantApi(service)

    private fun entry(id: String, active: Boolean) = SysTenantCacheEntry(
        id = id,
        name = "name-$id",
        timezone = null,
        defaultLanguageCode = null,
        remark = null,
        active = active,
        builtIn = false,
    )

    @Test
    fun getTenantFromCache_delegates() {
        val tenant = entry("t-1", true)
        whenCalled(service.getTenantFromCache("t-1")).thenReturn(tenant)
        assertEquals(tenant, api.getTenantFromCache("t-1"))
        assertNull(api.getTenantFromCache("missing"))
        verify(service).getTenantFromCache("t-1")
    }

    @Test
    fun getTenantsFromCacheByIds_delegates() {
        val map = mapOf("t-1" to entry("t-1", true))
        whenCalled(service.getTenantsFromCacheByIds(listOf("t-1"))).thenReturn(map)
        assertEquals(map, api.getTenantsFromCacheByIds(listOf("t-1")))

        whenCalled(service.getTenantsFromCacheByIds(emptyList())).thenReturn(emptyMap())
        assertTrue(api.getTenantsFromCacheByIds(emptyList()).isEmpty())
    }

    @Test
    fun getTenantsBySubSystemCode_filtersInactiveTenants() {
        val active = entry("t-active", true)
        val inactive = entry("t-inactive", false)
        whenCalled(service.getTenantsForSubSystemFromCache("sub-a")).thenReturn(listOf(active, inactive))

        val result = api.getTenantsBySubSystemCode("sub-a")
        assertEquals(listOf(active), result)
    }

    @Test
    fun getTenantsBySubSystemCode_emptyWhenServiceReturnsNothing() {
        whenCalled(service.getTenantsForSubSystemFromCache("sub-none")).thenReturn(emptyList())
        assertTrue(api.getTenantsBySubSystemCode("sub-none").isEmpty())
    }
}
