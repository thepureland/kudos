package io.kudos.ms.msg.api.internal.controller.template

import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry
import io.kudos.ms.msg.core.template.api.MsgTemplateApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame
import org.mockito.Mockito.`when` as whenCalled

/**
 * test for MsgTemplateInternalController (pure delegation, core api mocked with Mockito, no Spring container)
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgTemplateInternalControllerTest {

    private val api = mock(MsgTemplateApi::class.java)
    private val controller = MsgTemplateInternalController(api)

    @Test
    fun getTemplateById_delegatesAndReturnsSameInstance() {
        val entry = mock(MsgTemplateCacheEntry::class.java)
        whenCalled(api.getTemplateById("tpl-1")).thenReturn(entry)

        assertSame(entry, controller.getTemplateById("tpl-1"))
        verify(api).getTemplateById("tpl-1")
    }

    @Test
    fun getTemplateById_returnsNullWhenApiReturnsNull() {
        whenCalled(api.getTemplateById("missing")).thenReturn(null)

        assertNull(controller.getTemplateById("missing"))
        verify(api).getTemplateById("missing")
    }

    @Test
    fun getTemplateByEvent_delegatesWithLocale() {
        val entry = mock(MsgTemplateCacheEntry::class.java)
        whenCalled(api.getTemplateByEvent("t-1", "order_paid", "sms", "zh_CN")).thenReturn(entry)

        assertSame(entry, controller.getTemplateByEvent("t-1", "order_paid", "sms", "zh_CN"))
        verify(api).getTemplateByEvent("t-1", "order_paid", "sms", "zh_CN")
    }

    @Test
    fun getTemplateByEvent_delegatesWithNullLocaleFallback() {
        val entry = mock(MsgTemplateCacheEntry::class.java)
        whenCalled(api.getTemplateByEvent("t-1", "order_paid", "sms", null)).thenReturn(entry)

        assertSame(entry, controller.getTemplateByEvent("t-1", "order_paid", "sms", null))
        verify(api).getTemplateByEvent("t-1", "order_paid", "sms", null)
    }

    @Test
    fun getTemplateByEvent_returnsNullWhenNoMatch() {
        whenCalled(api.getTemplateByEvent("t-x", "unknown_event", "email", "en_US")).thenReturn(null)

        assertNull(controller.getTemplateByEvent("t-x", "unknown_event", "email", "en_US"))
        verify(api).getTemplateByEvent("t-x", "unknown_event", "email", "en_US")
    }
}
