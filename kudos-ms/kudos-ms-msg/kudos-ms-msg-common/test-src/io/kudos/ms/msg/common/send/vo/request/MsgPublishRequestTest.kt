package io.kudos.ms.msg.common.send.vo.request

import io.kudos.ms.msg.common.send.enums.MsgPublishMethodEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgPublishRequest
 *
 * Covers required fields, the three defaulted fields (params=emptyMap, localeDictCode=null,
 * idempotencyKey=null), the MsgPublishMethodEnum field and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgPublishRequestTest {

    @Test
    fun defaultsForOptionalFields() {
        val req = MsgPublishRequest(
            tenantId = "tn-1",
            eventTypeDictCode = "user_registered",
            msgTypeDictCode = "m",
            publishMethod = MsgPublishMethodEnum.EMAIL,
            receiverIds = setOf("u-1"),
        )
        assertEquals(emptyMap(), req.params)
        assertNull(req.localeDictCode)
        assertNull(req.idempotencyKey)
    }

    @Test
    fun fullConstruction() {
        val req = MsgPublishRequest(
            tenantId = "tn-1",
            eventTypeDictCode = "order_paid",
            msgTypeDictCode = "m",
            publishMethod = MsgPublishMethodEnum.SMS,
            receiverIds = setOf("u-1", "u-2"),
            params = mapOf("name" to "Alice", "amount" to "100"),
            localeDictCode = "en_US",
            idempotencyKey = "req-123",
        )
        assertEquals("tn-1", req.tenantId)
        assertEquals("order_paid", req.eventTypeDictCode)
        assertEquals(MsgPublishMethodEnum.SMS, req.publishMethod)
        assertEquals(setOf("u-1", "u-2"), req.receiverIds)
        assertEquals("Alice", req.params["name"])
        assertEquals("100", req.params["amount"])
        assertEquals("en_US", req.localeDictCode)
        assertEquals("req-123", req.idempotencyKey)
    }

    @Test
    fun emptyReceiverIdsAllowed() {
        val req = MsgPublishRequest("tn", "e", "m", MsgPublishMethodEnum.SITE_MSG, emptySet())
        assertTrue(req.receiverIds.isEmpty())
    }

    @Test
    fun dataClassContract() {
        val a = MsgPublishRequest("tn", "e", "m", MsgPublishMethodEnum.EMAIL, setOf("u-1"))
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(publishMethod = MsgPublishMethodEnum.SMS))
        assertTrue(a.toString().contains("tn"))
    }

}
