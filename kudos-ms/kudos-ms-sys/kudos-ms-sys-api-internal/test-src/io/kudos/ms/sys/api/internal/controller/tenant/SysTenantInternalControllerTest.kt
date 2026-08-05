package io.kudos.ms.sys.api.internal.controller.tenant

import io.kudos.ms.sys.common.tenant.vo.SysTenantCacheEntry
import io.kudos.ms.sys.core.tenant.api.SysTenantApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for SysTenantInternalController (pure delegation, core api mocked with Mockito, no Spring container)
 *
 * Coverage:
 * - getTenantFromCache delegates (hit and null miss)
 * - getTenantsFromCacheByIds delegates returning the same map (incl. empty)
 * - getTenantsBySubSystemCode delegates by subSystemCode (incl. empty)
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantInternalControllerTest {

    private val api = mock(SysTenantApi::class.java)
    private val controller = SysTenantInternalController(api)

    @Test
    fun getTenantFromCache_delegates() {
        val entry = mock(SysTenantCacheEntry::class.java)
        whenCalled(api.getTenantFromCache("t-1")).thenReturn(entry)
        whenCalled(api.getTenantFromCache("t-missing")).thenReturn(null)

        assertSame(entry, controller.getTenantFromCache("t-1"))
        assertNull(controller.getTenantFromCache("t-missing"))
        verify(api).getTenantFromCache("t-1")
        verify(api).getTenantFromCache("t-missing")
    }

    @Test
    fun getTenantsFromCacheByIds_delegates() {
        val ids = listOf("t-1", "t-2")
        val map = mapOf("t-1" to mock(SysTenantCacheEntry::class.java))
        whenCalled(api.getTenantsFromCacheByIds(ids)).thenReturn(map)

        assertSame(map, controller.getTenantsFromCacheByIds(ids))
        verify(api).getTenantsFromCacheByIds(ids)
    }

    @Test
    fun getTenantsFromCacheByIds_emptyResult() {
        val ids = emptyList<String>()
        whenCalled(api.getTenantsFromCacheByIds(ids)).thenReturn(emptyMap())

        assertTrue(controller.getTenantsFromCacheByIds(ids).isEmpty())
        verify(api).getTenantsFromCacheByIds(ids)
    }

    @Test
    fun getTenantsBySubSystemCode_delegates() {
        val tenants = listOf(mock(SysTenantCacheEntry::class.java))
        whenCalled(api.getTenantsBySubSystemCode("sub-a")).thenReturn(tenants)

        assertSame(tenants, controller.getTenantsBySubSystemCode("sub-a"))
        assertTrue(controller.getTenantsBySubSystemCode("sub-empty").isEmpty())
        verify(api).getTenantsBySubSystemCode("sub-a")
    }
}
