package io.kudos.ms.msg.client.instance.fallback

import kotlin.test.Test
import kotlin.test.assertNull


/**
 * Unit tests for [MsgInstanceFallback].
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgInstanceFallbackTest {

    @Test
    fun getInstanceById_returnsNull_withoutCause() {
        assertNull(MsgInstanceFallback().getInstanceById("inst-1"))
    }

    @Test
    fun getInstanceById_returnsNull_withCause() {
        val fallback = MsgInstanceFallback()
        assertNull(fallback.getInstanceById(RuntimeException("remote down"), "inst-2"))
    }

    @Test
    fun getInstanceById_handlesBlankAndUnicodeId() {
        val fallback = MsgInstanceFallback()
        assertNull(fallback.getInstanceById(""))
        assertNull(fallback.getInstanceById("實例-😀"))
    }
}
