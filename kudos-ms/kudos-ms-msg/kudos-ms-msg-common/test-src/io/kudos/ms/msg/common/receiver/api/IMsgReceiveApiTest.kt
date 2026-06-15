package io.kudos.ms.msg.common.receiver.api

import io.kudos.ms.msg.common.receiver.vo.MsgReceiveCacheEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * test for IMsgReceiveApi
 *
 * Drives the contract through a fake implementation: argument pass-through and the
 * declared return shapes (List / Int / Boolean) for each operation.
 *
 * @author K
 * @since 1.0.0
 */
internal class IMsgReceiveApiTest {

    private class FakeApi : IMsgReceiveApi {
        var lastReceiverId: String? = null
        var lastMarkReadId: String? = null
        var lastMarkAllReceiverId: String? = null
        var receives: List<MsgReceiveCacheEntry> = emptyList()
        var unreadCount: Int = 0
        var markReadResult: Boolean = false
        var markAllResult: Int = 0

        override fun getReceivesByUserId(receiverId: String): List<MsgReceiveCacheEntry> {
            lastReceiverId = receiverId
            return receives
        }

        override fun getUnreadCountByUserId(receiverId: String): Int {
            lastReceiverId = receiverId
            return unreadCount
        }

        override fun markRead(id: String): Boolean {
            lastMarkReadId = id
            return markReadResult
        }

        override fun markAllReadByUserId(receiverId: String): Int {
            lastMarkAllReceiverId = receiverId
            return markAllResult
        }
    }

    @Test
    fun getReceivesByUserId() {
        val api = FakeApi()
        val entry = MsgReceiveCacheEntry("r-1", "u-1", "s-1", "11", null, null, "tn")
        api.receives = listOf(entry)
        val result = api.getReceivesByUserId("u-1")
        assertEquals("u-1", api.lastReceiverId)
        assertEquals(1, result.size)
        assertEquals("r-1", result[0].id)
    }

    @Test
    fun getReceivesByUserIdEmpty() {
        val api = FakeApi()
        assertTrue(api.getReceivesByUserId("u-x").isEmpty())
        assertEquals("u-x", api.lastReceiverId)
    }

    @Test
    fun getUnreadCountByUserId() {
        val api = FakeApi()
        api.unreadCount = 5
        assertEquals(5, api.getUnreadCountByUserId("u-1"))
        assertEquals("u-1", api.lastReceiverId)
    }

    @Test
    fun markRead() {
        val api = FakeApi()
        api.markReadResult = true
        assertTrue(api.markRead("r-1"))
        assertEquals("r-1", api.lastMarkReadId)
        api.markReadResult = false
        assertFalse(api.markRead("r-2"))
        assertEquals("r-2", api.lastMarkReadId)
    }

    @Test
    fun markAllReadByUserId() {
        val api = FakeApi()
        api.markAllResult = 7
        assertEquals(7, api.markAllReadByUserId("u-1"))
        assertEquals("u-1", api.lastMarkAllReceiverId)
    }

}
