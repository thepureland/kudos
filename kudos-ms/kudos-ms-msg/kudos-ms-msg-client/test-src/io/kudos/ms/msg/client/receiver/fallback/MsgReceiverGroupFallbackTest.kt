package io.kudos.ms.msg.client.receiver.fallback

import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue


internal class MsgReceiverGroupFallbackTest {

    private val fallback = MsgReceiverGroupFallback()

    @Test
    fun getReceiverGroupById_returnsNullOnFallback() {
        assertNull(fallback.getReceiverGroupById("group-1"))
    }

    @Test
    fun listActiveReceiverGroups_returnsEmptyListOnFallback() {
        assertTrue(fallback.listActiveReceiverGroups("role").isEmpty())
    }

}
