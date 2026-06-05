package io.kudos.ms.msg.core.receiver.service

import io.kudos.ms.msg.common.receiver.enums.MsgUnreceivedReasonEnum
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgUnreceivedService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for MsgUnreceivedService — batch failure recording, unresolved query, resolve and retry bump.
 *
 * Test data source: `receiver/MsgUnreceivedServiceTest.sql`.
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class MsgUnreceivedServiceTest : RdbTestBase() {

    @Resource
    private lateinit var msgUnreceivedService: IMsgUnreceivedService

    private val tenantId = "svc-tenant-msg-unrecv"
    private val sendId = "c3000000-0000-0000-0000-0000000000b1"
    private val unresolvedId = "e5000000-0000-0000-0000-000000000001" // resolved=false, retry_count=2
    private val resolvedId = "e5000000-0000-0000-0000-000000000002"    // resolved=true

    @Test
    fun recordFailuresWithEmptyReceiversIsNoOp() {
        val count = msgUnreceivedService.recordFailures(
            sendId, emptyList(), "EMAIL", MsgUnreceivedReasonEnum.NO_CONTACT, tenantId
        )
        assertEquals(0, count)
        // unchanged: still only the single pre-seeded unresolved row
        assertEquals(1, msgUnreceivedService.findUnresolvedBySend(sendId).size)
    }

    @Test
    fun recordFailuresInsertsOneRowPerReceiver() {
        val count = msgUnreceivedService.recordFailures(
            sendId, listOf("x1", "x2"), "EMAIL", MsgUnreceivedReasonEnum.CHANNEL_REJECT, tenantId
        )
        assertEquals(2, count)

        val unresolved = msgUnreceivedService.findUnresolvedBySend(sendId)
        // 1 pre-seeded + 2 newly inserted
        assertEquals(3, unresolved.size)
        // exclude the pre-seeded row by id; the remaining 2 are the freshly inserted ones.
        // (receiver_id is CHAR(36), space-padded on read, so match by id rather than receiverId)
        val fresh = unresolved.filterNot { it.id == unresolvedId }
        assertEquals(2, fresh.size)
        assertTrue(fresh.all { it.retryCount == 0 && !it.resolved })
    }

    @Test
    fun findUnresolvedExcludesResolvedRows() {
        val unresolved = msgUnreceivedService.findUnresolvedBySend(sendId)
        assertEquals(1, unresolved.size)
        assertEquals(unresolvedId, unresolved.single().id)
    }

    @Test
    fun resolveMarksRowResolvedAndRemovesFromUnresolved() {
        assertTrue(msgUnreceivedService.resolve(unresolvedId))
        assertTrue(msgUnreceivedService.get(unresolvedId)!!.resolved)
        assertTrue(msgUnreceivedService.findUnresolvedBySend(sendId).isEmpty())
    }

    @Test
    fun bumpRetryIncrementsCountAndStampsTime() {
        assertTrue(msgUnreceivedService.bumpRetry(unresolvedId))
        val after = msgUnreceivedService.get(unresolvedId)!!
        assertEquals(3, after.retryCount, "2 + 1 = 3")
        assertNotNull(after.lastRetryTime)
        // bumpRetry must not change resolved
        assertFalse(after.resolved)
    }

    @Test
    fun bumpRetryReturnsFalseWhenMissing() {
        assertFalse(msgUnreceivedService.bumpRetry("no-such-id"))
    }
}
