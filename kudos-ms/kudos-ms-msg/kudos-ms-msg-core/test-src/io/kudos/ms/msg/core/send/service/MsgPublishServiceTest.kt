package io.kudos.ms.msg.core.send.service

import io.kudos.ms.msg.common.send.enums.MsgPublishMethodEnum
import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest
import io.kudos.ms.msg.core.send.service.iservice.IMsgPublishService
import io.kudos.ms.msg.core.send.service.iservice.IMsgSendService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.Test

/**
 * junit test for MsgPublishService — focuses on the idempotency behavior.
 *
 * Test data source: `send/MsgPublishServiceTest.sql` (seeds one msg_template).
 *
 * Note: notify-mq is not on the msg-core test classpath, so dispatch lands in
 * FAILED_TO_SEND_TO_MQ; the publish call still persists the records and returns the sendId,
 * which is all these idempotency assertions rely on.
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class MsgPublishServiceTest : RdbTestBase() {

    @Resource
    private lateinit var msgPublishService: IMsgPublishService

    @Resource
    private lateinit var msgSendService: IMsgSendService

    private val tenantId = "svc-tenant-msg-idem"

    private fun request(idempotencyKey: String?) = MsgPublishRequest(
        tenantId = tenantId,
        eventTypeDictCode = "user_registered",
        msgTypeDictCode = "welcome",
        publishMethod = MsgPublishMethodEnum.EMAIL,
        receiverIds = setOf("u1"),
        params = mapOf("name" to "Alice"),
        idempotencyKey = idempotencyKey,
    )

    @Test
    fun sameIdempotencyKeyReturnsSameSendIdWithoutDuplicating() {
        val first = msgPublishService.publish(request("req-idem-1"))
        assertNotNull(first)

        val second = msgPublishService.publish(request("req-idem-1"))
        assertEquals(first, second, "Repeated publish with the same idempotencyKey must return the existing sendId")

        // Only one send record persisted for this key.
        val found = msgSendService.findByIdempotencyKey(tenantId, "req-idem-1")
        assertNotNull(found)
        assertEquals(first, found.id)
    }

    @Test
    fun distinctIdempotencyKeysProduceDistinctSends() {
        val a = msgPublishService.publish(request("req-idem-a"))
        val b = msgPublishService.publish(request("req-idem-b"))
        assertNotNull(a)
        assertNotNull(b)
        assertNotEquals(a, b, "Different idempotencyKeys must create separate send records")
    }

    @Test
    fun nullIdempotencyKeyKeepsLegacyBehavior() {
        val a = msgPublishService.publish(request(null))
        val b = msgPublishService.publish(request(null))
        assertNotNull(a)
        assertNotNull(b)
        assertNotEquals(a, b, "Without an idempotencyKey every publish must create a new send record")
    }
}
