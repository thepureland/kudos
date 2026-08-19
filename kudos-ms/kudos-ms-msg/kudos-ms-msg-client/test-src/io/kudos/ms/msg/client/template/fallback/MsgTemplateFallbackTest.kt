package io.kudos.ms.msg.client.template.fallback

import kotlin.test.Test
import kotlin.test.assertNull


/**
 * Unit tests for [MsgTemplateFallback].
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgTemplateFallbackTest {

    private val fallback = MsgTemplateFallback()
    /** 传给 Throwable 首参重载——Spring Cloud 在降级时优先解析的就是这个签名。 */
    private val cause = RuntimeException("remote down")

    @Test
    fun getTemplateById_returnsNull() {
        assertNull(fallback.getTemplateById("tpl-1"))
        assertNull(fallback.getTemplateById(cause, "範本-😀"))
    }

    @Test
    fun getTemplateByEvent_returnsNull_withLocale() {
        assertNull(fallback.getTemplateByEvent("t", "e", "m", "zh_TW"))
    }

    @Test
    fun getTemplateByEvent_returnsNull_withNullLocale() {
        assertNull(fallback.getTemplateByEvent("t", "e", "m", null))
        assertNull(fallback.getTemplateByEvent(cause, "t", "e", "m", null))
    }
}
