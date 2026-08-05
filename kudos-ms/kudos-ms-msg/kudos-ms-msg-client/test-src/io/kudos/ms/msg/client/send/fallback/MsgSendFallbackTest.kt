package io.kudos.ms.msg.client.send.fallback

import io.kudos.ms.msg.common.send.enums.MsgPublishMethodEnum
import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest
import kotlin.test.Test
import kotlin.test.assertNull


/**
 * Unit tests for [MsgSendFallback].
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgSendFallbackTest {

    private val request = MsgPublishRequest(
        tenantId = "t-1",
        eventTypeDictCode = "user_registered",
        msgTypeDictCode = "welcome",
        publishMethod = MsgPublishMethodEnum.EMAIL,
        receiverIds = setOf("u-1", "u-2"),
        params = mapOf("name" to "K"),
        localeDictCode = "zh_TW",
        idempotencyKey = "req-1",
    )

    @Test
    fun publish_returnsNull_withoutCause() {
        assertNull(MsgSendFallback().publish(request))
    }

    @Test
    fun publish_returnsNull_withCause() {
        assertNull(MsgSendFallback(RuntimeException("remote down")).publish(request))
    }

    @Test
    fun publish_returnsNull_withMinimalRequest() {
        val minimal = MsgPublishRequest(
            tenantId = "t",
            eventTypeDictCode = "e",
            msgTypeDictCode = "m",
            publishMethod = MsgPublishMethodEnum.SITE_MSG,
            receiverIds = emptySet(),
        )
        assertNull(MsgSendFallback().publish(minimal))
    }
}
