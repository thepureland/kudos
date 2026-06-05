package io.kudos.ms.msg.core.receiver.service

import io.kudos.ms.msg.common.receiver.enums.MsgReceiveStatusEnum
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgReceiveService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for MsgReceiveService — unread counting, single mark-read idempotency and batch mark-read.
 *
 * Test data source: `receiver/MsgReceiveServiceTest.sql`.
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class MsgReceiveServiceTest : RdbTestBase() {

    @Resource
    private lateinit var msgReceiveService: IMsgReceiveService

    private val user1 = "ruser-1"
    private val user2 = "ruser-2"
    private val receivedId = "d4000000-0000-0000-0000-000000000001" // status 11 (RECEIVED, unread)
    private val unreadId = "d4000000-0000-0000-0000-000000000002"   // status 01 (UNREAD)
    private val readId = "d4000000-0000-0000-0000-000000000003"     // status 12 (READ)
    private val deletedId = "d4000000-0000-0000-0000-000000000004"  // status 21 (DELETED)

    @Test
    fun unreadCountCountsReceivedAndUnreadOnly() {
        // user1 has 11 + 01 unread, but 12/21 excluded => 2
        assertEquals(2, msgReceiveService.getUnreadCountByUserId(user1))
        assertEquals(1, msgReceiveService.getUnreadCountByUserId(user2))
    }

    @Test
    fun unreadCountIsZeroForUnknownUser() {
        assertEquals(0, msgReceiveService.getUnreadCountByUserId("nobody"))
    }

    @Test
    fun markReadFlipsUnreadRecord() {
        assertTrue(msgReceiveService.markRead(receivedId))
        val after = msgReceiveService.get(receivedId)!!
        assertEquals(MsgReceiveStatusEnum.READ.dictCode, after.receiveStatusDictCode)
        // one fewer unread for user1
        assertEquals(1, msgReceiveService.getUnreadCountByUserId(user1))
    }

    @Test
    fun markReadIsIdempotentOnAlreadyReadOrDeleted() {
        assertFalse(msgReceiveService.markRead(readId), "already READ must return false")
        assertFalse(msgReceiveService.markRead(deletedId), "DELETED must return false")
    }

    @Test
    fun markReadReturnsFalseWhenMissing() {
        assertFalse(msgReceiveService.markRead("no-such-receive-id"))
    }

    @Test
    fun markAllReadOnlyAffectsUnreadOfThatUser() {
        val updated = msgReceiveService.markAllReadByUserId(user1)
        assertEquals(2, updated, "only the 2 unread records (11/01) should be updated")

        assertEquals(0, msgReceiveService.getUnreadCountByUserId(user1))
        // user2 untouched
        assertEquals(1, msgReceiveService.getUnreadCountByUserId(user2))
        // previously READ/DELETED rows are unchanged
        assertEquals(MsgReceiveStatusEnum.READ.dictCode, msgReceiveService.get(readId)!!.receiveStatusDictCode)
        assertEquals(MsgReceiveStatusEnum.DELETED.dictCode, msgReceiveService.get(deletedId)!!.receiveStatusDictCode)
    }

    @Test
    fun getReceivesByUserIdReturnsWholeInbox() {
        // all four rows for user1 regardless of status
        assertEquals(4, msgReceiveService.getReceivesByUserId(user1).size)
        assertEquals(1, msgReceiveService.getReceivesByUserId(user2).size)
    }
}
