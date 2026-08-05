package io.kudos.ms.sys.api.internal.controller.locale

import io.kudos.ms.sys.common.locale.vo.SysLocaleCacheEntry
import io.kudos.ms.sys.core.locale.api.SysLocaleApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for SysLocaleInternalController (pure delegation, core api mocked with Mockito, no Spring container)
 *
 * Coverage:
 * - getLocaleByCode delegates (hit and null miss)
 * - listActiveLocales delegates (non-empty and empty results)
 *
 * @author K
 * @since 1.0.0
 */
internal class SysLocaleInternalControllerTest {

    private val api = mock(SysLocaleApi::class.java)
    private val controller = SysLocaleInternalController(api)

    @Test
    fun getLocaleByCode_delegates() {
        val entry = mock(SysLocaleCacheEntry::class.java)
        whenCalled(api.getLocaleByCode("zh_CN")).thenReturn(entry)

        assertSame(entry, controller.getLocaleByCode("zh_CN"))
        assertNull(controller.getLocaleByCode("xx_XX"))
        verify(api).getLocaleByCode("zh_CN")
    }

    @Test
    fun listActiveLocales_delegates() {
        val locales = listOf(mock(SysLocaleCacheEntry::class.java), mock(SysLocaleCacheEntry::class.java))
        whenCalled(api.listActiveLocales()).thenReturn(locales)

        assertSame(locales, controller.listActiveLocales())
        verify(api).listActiveLocales()
    }

    @Test
    fun listActiveLocales_emptyWhenNoActiveLocale() {
        whenCalled(api.listActiveLocales()).thenReturn(emptyList())
        assertTrue(controller.listActiveLocales().isEmpty())
    }
}
