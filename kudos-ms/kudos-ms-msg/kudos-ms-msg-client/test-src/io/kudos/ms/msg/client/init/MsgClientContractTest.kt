package io.kudos.ms.msg.client.init

import io.kudos.ms.msg.client.receiver.proxy.IMsgReceiveProxy
import io.kudos.ms.msg.common.receiver.api.IMsgReceiveApi
import io.kudos.ms.msg.common.receiver.vo.MsgReceiveCacheEntry
import io.kudos.test.common.init.EnableKudosTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.web.bind.annotation.RestController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Round-trip test for the msg interface clients against a controller implementing the same
 * `IMsgReceiveApi` contract from `kudos-ms-msg-common`.
 *
 * Msg is the module that used `fallbackFactory` rather than `fallback`, so it also covers the shape
 * the other three do not: a `@PostExchange` whose arguments are **all `@RequestParam`** and whose
 * body is therefore empty — a case where the client has to send a POST with a query string and no
 * content, and the server has to bind it.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = [
        "server.port=${MsgClientContractTest.PORT}",
        "spring.http.serviceclient.${MsgClientAutoConfiguration.GROUP}.base-url=http://localhost:${MsgClientContractTest.PORT}",
    ]
)
@Import(MsgClientContractTest.MockReceiveController::class)
open class MsgClientContractTest {

    @Autowired
    private lateinit var receiveProxy: IMsgReceiveProxy

    @Test
    fun `GET returning a list round-trips`() {
        val received = receiveProxy.getReceivesByUserId("u-1")
        assertEquals(1, received.size)
        assertEquals("u-1", received.single().receiverId)
    }

    @Test
    fun `GET returning a primitive int round-trips`() {
        assertEquals(3, receiveProxy.getUnreadCountByUserId("u-1"))
        assertEquals(0, receiveProxy.getUnreadCountByUserId("u-2"))
    }

    @Test
    fun `POST with query params and no body round-trips`() {
        assertTrue(receiveProxy.markRead("m-1"))
        assertEquals(3, receiveProxy.markAllReadByUserId("u-1"))
    }

    /** Mock provider; routes come solely from the annotations on [IMsgReceiveApi]. */
    @RestController
    open class MockReceiveController : IMsgReceiveApi {

        override fun getReceivesByUserId(receiverId: String): List<MsgReceiveCacheEntry> =
            listOf(
                MsgReceiveCacheEntry(
                    id = "m-1",
                    receiverId = receiverId,
                    sendId = "s-1",
                    receiveStatusDictCode = "UNREAD",
                    createTime = null,
                    updateTime = null,
                    tenantId = "t-1",
                )
            )

        override fun getUnreadCountByUserId(receiverId: String): Int =
            if (receiverId == "u-1") 3 else 0

        override fun markRead(id: String): Boolean = id == "m-1"

        override fun markAllReadByUserId(receiverId: String): Int =
            if (receiverId == "u-1") 3 else 0
    }

    companion object {
        /** Fixed port: the group's base-url is resolved before a RANDOM_PORT server knows its port. */
        const val PORT: String = "18096"
    }

}
