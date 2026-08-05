package io.kudos.ms.msg.client.send.fallback

import io.kudos.ms.msg.client.send.proxy.IMsgSendProxy
import io.kudos.ms.msg.common.send.enums.MsgPublishMethodEnum
import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue


/**
 * Unit tests for [MsgSendFallbackFactory].
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgSendFallbackFactoryTest {

    private val factory = MsgSendFallbackFactory()

    private val request = MsgPublishRequest(
        tenantId = "t",
        eventTypeDictCode = "e",
        msgTypeDictCode = "m",
        publishMethod = MsgPublishMethodEnum.SMS,
        receiverIds = setOf("u"),
    )

    @Test
    fun create_returnsFallbackThatDegradesGracefully() {
        val proxy: IMsgSendProxy = factory.create(RuntimeException("boom"))
        assertTrue(proxy is MsgSendFallback)
        assertNull(proxy.publish(request))
    }

    @Test
    fun create_returnsNewInstanceEachTime() {
        val cause = IllegalStateException("err")
        assertNotSame(factory.create(cause), factory.create(cause))
    }
}
