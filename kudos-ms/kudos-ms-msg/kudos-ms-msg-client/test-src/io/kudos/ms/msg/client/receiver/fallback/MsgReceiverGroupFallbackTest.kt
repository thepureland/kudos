package io.kudos.ms.msg.client.receiver.fallback

import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue


internal class MsgReceiverGroupFallbackTest {

    private val fallback = MsgReceiverGroupFallback()
    /** 传给 Throwable 首参重载——Spring Cloud 在降级时优先解析的就是这个签名。 */
    private val cause = RuntimeException("remote down")

    @Test
    fun getReceiverGroupById_returnsNullOnFallback() {
        assertNull(fallback.getReceiverGroupById("group-1"))
        assertNull(fallback.getReceiverGroupById(cause, "群組-😀"))
    }

    @Test
    fun listActiveReceiverGroups_returnsEmptyListOnFallback() {
        assertTrue(fallback.listActiveReceiverGroups("role").isEmpty())
    }

    @Test
    fun listActiveReceiverGroups_acceptsNullTypeCode() {
        assertTrue(fallback.listActiveReceiverGroups(null).isEmpty())
        assertTrue(fallback.listActiveReceiverGroups(cause, null).isEmpty())
    }

}
