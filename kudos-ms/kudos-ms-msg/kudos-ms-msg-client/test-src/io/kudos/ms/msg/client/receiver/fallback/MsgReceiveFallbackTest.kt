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
    /** 传给 Throwable 首参重载——Spring Cloud 在降级时优先解析的就是这个签名。 */
    private val cause = RuntimeException("remote down")

    @Test
    fun getReceivesByUserId_returnsEmptyList() {
        assertTrue(fallback.getReceivesByUserId("u-1").isEmpty())
        assertTrue(fallback.getReceivesByUserId(cause, "實體-😀").isEmpty())
    }

    @Test
    fun getUnreadCountByUserId_returnsZero() {
        assertEquals(0, fallback.getUnreadCountByUserId("u-1"))
        assertEquals(0, fallback.getUnreadCountByUserId(cause, ""))
    }

    @Test
    fun markRead_returnsFalse() {
        assertFalse(fallback.markRead("id-1"))
        assertFalse(fallback.markRead(cause, ""))
    }

    @Test
    fun markAllReadByUserId_returnsZero() {
        assertEquals(0, fallback.markAllReadByUserId("u-1"))
        assertEquals(0, fallback.markAllReadByUserId(cause, "u-2"))
    }
}
