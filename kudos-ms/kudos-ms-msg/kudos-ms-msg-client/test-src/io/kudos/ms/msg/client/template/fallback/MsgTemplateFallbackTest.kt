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
    private val fallbackWithCause = MsgTemplateFallback(RuntimeException("remote down"))

    @Test
    fun getTemplateById_returnsNull() {
        assertNull(fallback.getTemplateById("tpl-1"))
        assertNull(fallbackWithCause.getTemplateById("範本-😀"))
    }

    @Test
    fun getTemplateByEvent_returnsNull_withLocale() {
        assertNull(fallback.getTemplateByEvent("t", "e", "m", "zh_TW"))
    }

    @Test
    fun getTemplateByEvent_returnsNull_withNullLocale() {
        assertNull(fallback.getTemplateByEvent("t", "e", "m", null))
        assertNull(fallbackWithCause.getTemplateByEvent("t", "e", "m", null))
    }
}
