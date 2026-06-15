package io.kudos.ms.msg.client.receiver.fallback

import io.kudos.ms.msg.client.receiver.proxy.IMsgReceiveProxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue


/**
 * Unit tests for [MsgReceiveFallbackFactory].
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiveFallbackFactoryTest {

    private val factory = MsgReceiveFallbackFactory()

    @Test
    fun create_returnsFallbackThatDegradesGracefully() {
        val proxy: IMsgReceiveProxy = factory.create(RuntimeException("boom"))
        assertTrue(proxy is MsgReceiveFallback)
        assertTrue(proxy.getReceivesByUserId("u").isEmpty())
        assertEquals(0, proxy.getUnreadCountByUserId("u"))
        assertFalse(proxy.markRead("id"))
        assertEquals(0, proxy.markAllReadByUserId("u"))
    }

    @Test
    fun create_returnsNewInstanceEachTime() {
        val cause = IllegalStateException("err")
        assertNotSame(factory.create(cause), factory.create(cause))
    }
}
