package io.kudos.ms.sys.api.internal.controller.tenant

import io.kudos.ms.sys.core.tenant.api.SysTenantLocaleApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for SysTenantLocaleInternalController (pure delegation, core api mocked with Mockito, no Spring container)
 *
 * Coverage:
 * - getLocaleCodesByTenantId delegates (incl. empty set)
 * - getTenantIdsByLocaleCode delegates
 * - batchBind delegates and returns the affected-row count
 * - unbind delegates for both true and false results
 * - exists delegates for both true and false results
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantLocaleInternalControllerTest {

    private val api = mock(SysTenantLocaleApi::class.java)
    private val controller = SysTenantLocaleInternalController(api)

    @Test
    fun getLocaleCodesByTenantId_delegates() {
        val codes = setOf("zh_CN", "en_US")
        whenCalled(api.getLocaleCodesByTenantId("t-1")).thenReturn(codes)

        assertSame(codes, controller.getLocaleCodesByTenantId("t-1"))
        assertTrue(controller.getLocaleCodesByTenantId("t-empty").isEmpty())
        verify(api).getLocaleCodesByTenantId("t-1")
    }

    @Test
    fun getTenantIdsByLocaleCode_delegates() {
        val ids = setOf("t-1", "t-2")
        whenCalled(api.getTenantIdsByLocaleCode("zh_CN")).thenReturn(ids)

        assertSame(ids, controller.getTenantIdsByLocaleCode("zh_CN"))
        verify(api).getTenantIdsByLocaleCode("zh_CN")
    }

    @Test
    fun batchBind_delegates() {
        val codes = listOf("zh_CN", "en_US")
        whenCalled(api.batchBind("t-1", codes)).thenReturn(2)

        assertEquals(2, controller.batchBind("t-1", codes))
        verify(api).batchBind("t-1", codes)
    }

    @Test
    fun unbind_delegates() {
        whenCalled(api.unbind("t-1", "zh_CN")).thenReturn(true)
        whenCalled(api.unbind("t-1", "missing")).thenReturn(false)

        assertTrue(controller.unbind("t-1", "zh_CN"))
        assertFalse(controller.unbind("t-1", "missing"))
        verify(api).unbind("t-1", "zh_CN")
        verify(api).unbind("t-1", "missing")
    }

    @Test
    fun exists_delegates() {
        whenCalled(api.exists("t-1", "zh_CN")).thenReturn(true)
        whenCalled(api.exists("t-1", "missing")).thenReturn(false)

        assertTrue(controller.exists("t-1", "zh_CN"))
        assertFalse(controller.exists("t-1", "missing"))
        verify(api).exists("t-1", "zh_CN")
        verify(api).exists("t-1", "missing")
    }
}
