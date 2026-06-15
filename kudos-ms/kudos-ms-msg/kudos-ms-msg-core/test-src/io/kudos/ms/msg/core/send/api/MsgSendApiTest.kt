package io.kudos.ms.msg.core.send.api

import io.kudos.ms.msg.common.send.enums.MsgPublishMethodEnum
import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest
import io.kudos.ms.msg.core.send.service.iservice.IMsgPublishService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * test for MsgSendApi (pure delegation, publish service mocked with Mockito, no Spring container).
 *
 * The service dependency is a private `@Resource lateinit var`, injected by reflection.
 *
 * Coverage: publish forwards the request unchanged and returns both a non-null id and null.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgSendApiTest {

    private val service = mock(IMsgPublishService::class.java)
    private val api = MsgSendApi().apply {
        val field = MsgSendApi::class.java.getDeclaredField("msgPublishService")
        field.isAccessible = true
        field.set(this, service)
    }

    private fun request() = MsgPublishRequest(
        tenantId = "t1",
        eventTypeDictCode = "user_registered",
        msgTypeDictCode = "welcome",
        publishMethod = MsgPublishMethodEnum.EMAIL,
        receiverIds = setOf("u1"),
    )

    @Test
    fun publish_delegatesAndReturnsId() {
        val req = request()
        whenCalled(service.publish(req)).thenReturn("send-1")
        assertEquals("send-1", api.publish(req))
        verify(service).publish(req)
    }

    @Test
    fun publish_delegatesNull() {
        val req = request()
        whenCalled(service.publish(req)).thenReturn(null)
        assertNull(api.publish(req))
        verify(service).publish(req)
    }
}
