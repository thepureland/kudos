package io.kudos.ms.msg.core.send.service

import io.kudos.ms.msg.core.send.service.iservice.IMsgSendService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * junit test for MsgSendService — counter accumulation, status update and idempotency-key lookup.
 *
 * Test data source: `send/MsgSendServiceTest.sql`.
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class MsgSendServiceTest : RdbTestBase() {

    @Resource
    private lateinit var msgSendService: IMsgSendService

    private val tenantId = "svc-tenant-msg-send"
    private val seededId = "c3000000-0000-0000-0000-000000000001"
    private val nullCountsId = "c3000000-0000-0000-0000-000000000002"

    @Test
    fun finishSendAccumulatesCountsAndSetsStatus() {
        val ok = msgSendService.finishSend(seededId, successDelta = 3, failDelta = 1, finalStatusDictCode = "02")
        assertTrue(ok)

        val after = msgSendService.get(seededId)
        assertNotNull(after)
        assertEquals(5, after.successCount, "2 + 3 = 5")
        assertEquals(2, after.failCount, "1 + 1 = 2")
        assertEquals("02", after.sendStatusDictCode)
        assertNotNull(after.updateTime, "finishSend must stamp update_time")
    }

    @Test
    fun finishSendTreatsNullCountsAsZero() {
        val ok = msgSendService.finishSend(nullCountsId, successDelta = 4, failDelta = 2, finalStatusDictCode = "03")
        assertTrue(ok)

        val after = msgSendService.get(nullCountsId)
        assertNotNull(after)
        assertEquals(4, after.successCount, "null treated as 0, 0 + 4 = 4")
        assertEquals(2, after.failCount, "null treated as 0, 0 + 2 = 2")
    }

    @Test
    fun finishSendReturnsFalseWhenRecordMissing() {
        val ok = msgSendService.finishSend("no-such-send-id", 1, 0, "02")
        assertFalse(ok, "finishSend on a missing record must return false, not throw")
    }

    @Test
    fun updateSendStatusChangesOnlyStatusNotCounts() {
        val ok = msgSendService.updateSendStatus(seededId, "09")
        assertTrue(ok)

        val after = msgSendService.get(seededId)
        assertNotNull(after)
        assertEquals("09", after.sendStatusDictCode)
        // counts must be untouched by updateSendStatus
        assertEquals(2, after.successCount)
        assertEquals(1, after.failCount)
    }

    @Test
    fun findByIdempotencyKeyReturnsSeededRecord() {
        val found = msgSendService.findByIdempotencyKey(tenantId, "send-idem-1")
        assertNotNull(found)
        assertEquals(seededId, found.id)
    }

    @Test
    fun findByIdempotencyKeyReturnsNullForUnknownKey() {
        assertNull(msgSendService.findByIdempotencyKey(tenantId, "no-such-key"))
    }
}
