package io.kudos.ms.msg.client.instance.fallback

import io.kudos.ms.msg.client.instance.proxy.IMsgInstanceProxy
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue


/**
 * Unit tests for [MsgInstanceFallbackFactory].
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgInstanceFallbackFactoryTest {

    private val factory = MsgInstanceFallbackFactory()

    @Test
    fun create_returnsFallbackThatDegradesGracefully() {
        val proxy: IMsgInstanceProxy = factory.create(RuntimeException("boom"))
        assertNotNull(proxy)
        assertTrue(proxy is MsgInstanceFallback)
        assertNull(proxy.getInstanceById("x"))
    }

    @Test
    fun create_returnsNewInstanceEachTime() {
        val cause = IllegalStateException("err")
        assertNotSame(factory.create(cause), factory.create(cause))
    }
}
