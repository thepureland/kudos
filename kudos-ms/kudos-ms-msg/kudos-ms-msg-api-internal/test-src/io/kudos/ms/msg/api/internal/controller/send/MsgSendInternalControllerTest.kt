package io.kudos.ms.msg.api.internal.controller.send

import io.kudos.ms.msg.common.send.enums.MsgPublishMethodEnum
import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest
import io.kudos.ms.msg.core.send.api.MsgSendApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.mockito.Mockito.`when` as whenCalled

/**
 * test for MsgSendInternalController (pure delegation, core api mocked with Mockito, no Spring container)
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgSendInternalControllerTest {

    private val api = mock(MsgSendApi::class.java)
    private val controller = MsgSendInternalController(api)

    private fun request() = MsgPublishRequest(
        tenantId = "t-1",
        eventTypeDictCode = "user_registered",
        msgTypeDictCode = "welcome",
        publishMethod = MsgPublishMethodEnum.EMAIL,
        receiverIds = setOf("u-1", "u-2"),
    )

    @Test
    fun publish_delegatesAndReturnsSendId() {
        val req = request()
        whenCalled(api.publish(req)).thenReturn("send-id-1")

        assertEquals("send-id-1", controller.publish(req))
        verify(api).publish(req)
    }

    @Test
    fun publish_returnsNullWhenTemplateMissingOrRequestInvalid() {
        val req = request()
        whenCalled(api.publish(req)).thenReturn(null)

        assertNull(controller.publish(req))
        verify(api).publish(req)
    }

    @Test
    fun publish_passesSameRequestInstanceThrough() {
        val req = request()
        whenCalled(api.publish(req)).thenReturn("x")

        controller.publish(req)

        // the request must be forwarded unchanged (same value), verified via Mockito argument equality
        verify(api).publish(req)
    }
}
