package io.kudos.ms.sys.core.locale.api

import io.kudos.ms.sys.common.locale.vo.SysLocaleCacheEntry
import io.kudos.ms.sys.core.locale.service.iservice.ISysLocaleService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for SysLocaleApi (pure delegation, service mocked with Mockito, no Spring container).
 *
 * Coverage:
 * - getLocaleByCode delegates to the service (hit and null miss)
 * - listActiveLocales delegates unchanged (incl. empty result)
 *
 * @author K
 * @since 1.0.0
 */
internal class SysLocaleApiTest {

    private val service = mock(ISysLocaleService::class.java)
    private val api = SysLocaleApi(service)

    private fun entry(code: String) = SysLocaleCacheEntry(
        id = "id-$code",
        code = code,
        displayName = "name-$code",
        englishName = "en-$code",
        sortNo = 1,
        remark = null,
        active = true,
        builtIn = false,
    )

    @Test
    fun getLocaleByCode_delegates() {
        val e = entry("zh_CN")
        whenCalled(service.getLocaleByCode("zh_CN")).thenReturn(e)
        assertSame(e, api.getLocaleByCode("zh_CN"))
        verify(service).getLocaleByCode("zh_CN")
    }

    @Test
    fun getLocaleByCode_nullWhenMiss() {
        whenCalled(service.getLocaleByCode("miss")).thenReturn(null)
        assertNull(api.getLocaleByCode("miss"))
    }

    @Test
    fun listActiveLocales_delegates() {
        val list = listOf(entry("zh_CN"), entry("en_US"))
        whenCalled(service.listActiveLocales()).thenReturn(list)
        assertEquals(list, api.listActiveLocales())

        whenCalled(service.listActiveLocales()).thenReturn(emptyList())
        assertTrue(api.listActiveLocales().isEmpty())
    }
}
