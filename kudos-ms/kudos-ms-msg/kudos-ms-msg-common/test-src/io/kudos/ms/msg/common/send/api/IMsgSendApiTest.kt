package io.kudos.ms.msg.common.send.api

import io.kudos.ms.msg.common.send.enums.MsgPublishMethodEnum
import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * test for IMsgSendApi
 *
 * Drives the contract through a fake implementation: request pass-through and both the
 * success (send id) and the template-missing/invalid (null) return.
 *
 * @author K
 * @since 1.0.0
 */
internal class IMsgSendApiTest {

    private class FakeApi : IMsgSendApi {
        var lastRequest: MsgPublishRequest? = null
        var result: String? = null
        override fun publish(request: MsgPublishRequest): String? {
            lastRequest = request
            return result
        }
    }

    private fun request() = MsgPublishRequest(
        tenantId = "tn-1",
        eventTypeDictCode = "user_registered",
        msgTypeDictCode = "m",
        publishMethod = MsgPublishMethodEnum.EMAIL,
        receiverIds = setOf("u-1"),
    )

    @Test
    fun publishReturnsSendId() {
        val api = FakeApi()
        api.result = "send-1"
        val req = request()
        assertEquals("send-1", api.publish(req))
        assertSame(req, api.lastRequest)
    }

    @Test
    fun publishReturnsNullWhenTemplateMissing() {
        val api = FakeApi()
        assertNull(api.publish(request()))
    }

}
