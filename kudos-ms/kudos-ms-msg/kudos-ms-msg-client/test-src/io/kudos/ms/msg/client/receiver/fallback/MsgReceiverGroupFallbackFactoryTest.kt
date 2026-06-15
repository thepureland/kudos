package io.kudos.ms.msg.client.receiver.fallback

import io.kudos.ms.msg.client.receiver.proxy.IMsgReceiverGroupProxy
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue


/**
 * Unit tests for [MsgReceiverGroupFallbackFactory].
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiverGroupFallbackFactoryTest {

    private val factory = MsgReceiverGroupFallbackFactory()

    @Test
    fun create_returnsFallbackThatDegradesGracefully() {
        val proxy: IMsgReceiverGroupProxy = factory.create(RuntimeException("boom"))
        assertTrue(proxy is MsgReceiverGroupFallback)
        assertNull(proxy.getReceiverGroupById("g"))
        assertTrue(proxy.listActiveReceiverGroups(null).isEmpty())
    }

    @Test
    fun create_returnsNewInstanceEachTime() {
        val cause = IllegalStateException("err")
        assertNotSame(factory.create(cause), factory.create(cause))
    }
}
