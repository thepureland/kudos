package io.kudos.ms.msg.client.template.fallback

import io.kudos.ms.msg.client.template.proxy.IMsgTemplateProxy
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue


/**
 * Unit tests for [MsgTemplateFallbackFactory].
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgTemplateFallbackFactoryTest {

    private val factory = MsgTemplateFallbackFactory()

    @Test
    fun create_returnsFallbackThatDegradesGracefully() {
        val proxy: IMsgTemplateProxy = factory.create(RuntimeException("boom"))
        assertTrue(proxy is MsgTemplateFallback)
        assertNull(proxy.getTemplateById("tpl"))
        assertNull(proxy.getTemplateByEvent("t", "e", "m", null))
    }

    @Test
    fun create_returnsNewInstanceEachTime() {
        val cause = IllegalStateException("err")
        assertNotSame(factory.create(cause), factory.create(cause))
    }
}
