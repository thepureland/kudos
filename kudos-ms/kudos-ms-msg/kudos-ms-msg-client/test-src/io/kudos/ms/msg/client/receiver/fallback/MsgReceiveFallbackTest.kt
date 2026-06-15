package io.kudos.ms.msg.client.receiver.fallback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * Unit tests for [MsgReceiveFallback].
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiveFallbackTest {

    private val fallback = MsgReceiveFallback()
    private val fallbackWithCause = MsgReceiveFallback(RuntimeException("remote down"))

    @Test
    fun getReceivesByUserId_returnsEmptyList() {
        assertTrue(fallback.getReceivesByUserId("u-1").isEmpty())
        assertTrue(fallbackWithCause.getReceivesByUserId("實體-😀").isEmpty())
    }

    @Test
    fun getUnreadCountByUserId_returnsZero() {
        assertEquals(0, fallback.getUnreadCountByUserId("u-1"))
        assertEquals(0, fallbackWithCause.getUnreadCountByUserId(""))
    }

    @Test
    fun markRead_returnsFalse() {
        assertFalse(fallback.markRead("id-1"))
        assertFalse(fallbackWithCause.markRead(""))
    }

    @Test
    fun markAllReadByUserId_returnsZero() {
        assertEquals(0, fallback.markAllReadByUserId("u-1"))
        assertEquals(0, fallbackWithCause.markAllReadByUserId("u-2"))
    }
}
