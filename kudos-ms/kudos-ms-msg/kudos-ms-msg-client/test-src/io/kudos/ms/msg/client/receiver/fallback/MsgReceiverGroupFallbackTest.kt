package io.kudos.ms.msg.client.receiver.fallback

import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue


internal class MsgReceiverGroupFallbackTest {

    private val fallback = MsgReceiverGroupFallback()
    private val fallbackWithCause = MsgReceiverGroupFallback(RuntimeException("remote down"))

    @Test
    fun getReceiverGroupById_returnsNullOnFallback() {
        assertNull(fallback.getReceiverGroupById("group-1"))
        assertNull(fallbackWithCause.getReceiverGroupById("群組-😀"))
    }

    @Test
    fun listActiveReceiverGroups_returnsEmptyListOnFallback() {
        assertTrue(fallback.listActiveReceiverGroups("role").isEmpty())
    }

    @Test
    fun listActiveReceiverGroups_acceptsNullTypeCode() {
        assertTrue(fallback.listActiveReceiverGroups(null).isEmpty())
        assertTrue(fallbackWithCause.listActiveReceiverGroups(null).isEmpty())
    }

}
