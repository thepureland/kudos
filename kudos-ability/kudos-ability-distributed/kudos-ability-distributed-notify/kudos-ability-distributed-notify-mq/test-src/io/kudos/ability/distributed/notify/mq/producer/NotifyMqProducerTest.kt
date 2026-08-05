package io.kudos.ability.distributed.notify.mq.producer

import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [NotifyMqProducer] unit tests.
 *
 * Covers: blank / whitespace-only notifyType rejection (returns false, send cancelled),
 * normal and unicode notifyType acceptance (returns true, actual send delegated to the
 * @MqProducer aspect placeholder pattern).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class NotifyMqProducerTest {

    private val producer = NotifyMqProducer()

    @Test
    fun notify_returnsFalseWhenNotifyTypeIsEmpty() {
        // no-arg constructor defaults notifyType to ""
        assertFalse(producer.notify(NotifyMessageVo("body")))
    }

    @Test
    fun notify_returnsFalseWhenNotifyTypeIsWhitespaceOnly() {
        assertFalse(producer.notify(NotifyMessageVo("   ", "body")))
        assertFalse(producer.notify(NotifyMessageVo("\t\n", "body")))
    }

    @Test
    fun notify_returnsTrueWhenNotifyTypeIsPresent() {
        assertTrue(producer.notify(NotifyMessageVo("notify.type.a", "body")))
    }

    @Test
    fun notify_returnsTrueForUnicodeNotifyType() {
        assertTrue(producer.notify(NotifyMessageVo("通知类型-中文", "消息体")))
    }

    @Test
    fun notify_returnsTrueForVeryLongNotifyType() {
        assertTrue(producer.notify(NotifyMessageVo("x".repeat(10_000), "body")))
    }
}
