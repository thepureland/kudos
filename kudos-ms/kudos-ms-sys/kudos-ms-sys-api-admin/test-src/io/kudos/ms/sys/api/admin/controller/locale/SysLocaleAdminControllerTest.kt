package io.kudos.ms.sys.api.admin.controller.locale

import io.kudos.ms.sys.common.locale.vo.SysLocaleCacheEntry
import io.kudos.ms.sys.core.locale.service.iservice.ISysLocaleService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Pure unit test for [SysLocaleAdminController]: listActive delegation and updateActive. The
 * service is mocked and injected via reflection; no Spring context.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysLocaleAdminControllerTest {

    private val service = mock(ISysLocaleService::class.java)
    private val controller = SysLocaleAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    @Test
    fun listActiveDelegatesAndPreservesOrder() {
        val l1 = mock(SysLocaleCacheEntry::class.java)
        val l2 = mock(SysLocaleCacheEntry::class.java)
        val list = listOf(l1, l2)
        whenCalled(service.listActiveLocales()).thenReturn(list)

        val result = controller.listActive()
        assertSame(list, result)
        assertSame(l1, result[0])
        assertSame(l2, result[1])
        verify(service).listActiveLocales()
    }

    @Test
    fun listActiveEmpty() {
        whenCalled(service.listActiveLocales()).thenReturn(emptyList())
        assertTrue(controller.listActive().isEmpty())
    }

    @Test
    fun updateActiveDelegatesBothOutcomes() {
        whenCalled(service.updateActive("l1", true)).thenReturn(true)
        whenCalled(service.updateActive("l2", false)).thenReturn(false)
        assertTrue(controller.updateActive("l1", true))
        assertFalse(controller.updateActive("l2", false))
        verify(service).updateActive("l1", true)
        verify(service).updateActive("l2", false)
    }
}
