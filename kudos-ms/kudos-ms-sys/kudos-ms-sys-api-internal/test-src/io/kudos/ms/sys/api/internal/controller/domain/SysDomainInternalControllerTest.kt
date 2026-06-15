package io.kudos.ms.sys.api.internal.controller.domain

import io.kudos.ms.sys.common.domain.vo.SysDomainCacheEntry
import io.kudos.ms.sys.core.domain.api.SysDomainApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * test for SysDomainInternalController (pure delegation, core api mocked with Mockito, no Spring container)
 *
 * Coverage:
 * - getDomainByName delegates (hit, null miss, and Unicode domain name passed through unchanged)
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDomainInternalControllerTest {

    private val api = mock(SysDomainApi::class.java)
    private val controller = SysDomainInternalController(api)

    @Test
    fun getDomainByName_delegates() {
        val entry = mock(SysDomainCacheEntry::class.java)
        whenCalled(api.getDomainByName("www.example.com")).thenReturn(entry)

        assertSame(entry, controller.getDomainByName("www.example.com"))
        assertNull(controller.getDomainByName("missing.example.com"))
        verify(api).getDomainByName("www.example.com")
    }

    @Test
    fun getDomainByName_passesUnicodeNameThrough() {
        val entry = mock(SysDomainCacheEntry::class.java)
        whenCalled(api.getDomainByName("例子.中国")).thenReturn(entry)

        assertSame(entry, controller.getDomainByName("例子.中国"))
        verify(api).getDomainByName("例子.中国")
    }
}
