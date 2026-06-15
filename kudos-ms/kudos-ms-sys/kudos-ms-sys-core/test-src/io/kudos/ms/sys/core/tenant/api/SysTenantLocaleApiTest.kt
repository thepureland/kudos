package io.kudos.ms.sys.core.tenant.api

import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantLocaleService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * test for SysTenantLocaleApi (pure delegation, service mocked with Mockito, no Spring container)
 *
 * Coverage: all 5 delegate methods forward arguments and return values unchanged
 * (getLocaleCodesByTenantId / getTenantIdsByLocaleCode / batchBind / unbind / exists).
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantLocaleApiTest {

    private val service = mock(ISysTenantLocaleService::class.java)
    private val api = SysTenantLocaleApi(service)

    @Test
    fun getLocaleCodesByTenantId_delegates() {
        whenCalled(service.getLocaleCodesByTenantId("t-1")).thenReturn(setOf("zh_CN", "en_US"))
        assertEquals(setOf("zh_CN", "en_US"), api.getLocaleCodesByTenantId("t-1"))
        verify(service).getLocaleCodesByTenantId("t-1")
    }

    @Test
    fun getTenantIdsByLocaleCode_delegates() {
        whenCalled(service.getTenantIdsByLocaleCode("zh_CN")).thenReturn(setOf("t-1"))
        assertEquals(setOf("t-1"), api.getTenantIdsByLocaleCode("zh_CN"))
        assertTrue(api.getTenantIdsByLocaleCode("xx_XX").isEmpty())
    }

    @Test
    fun batchBind_delegates() {
        whenCalled(service.batchBind("t-1", listOf("zh_CN"))).thenReturn(1)
        assertEquals(1, api.batchBind("t-1", listOf("zh_CN")))
        assertEquals(0, api.batchBind("t-1", emptyList()))
    }

    @Test
    fun unbind_delegates() {
        whenCalled(service.unbind("t-1", "zh_CN")).thenReturn(true)
        assertTrue(api.unbind("t-1", "zh_CN"))
        assertFalse(api.unbind("t-1", "missing"))
    }

    @Test
    fun exists_delegates() {
        whenCalled(service.exists("t-1", "zh_CN")).thenReturn(true)
        assertTrue(api.exists("t-1", "zh_CN"))
        assertFalse(api.exists("t-1", "missing"))
    }
}
