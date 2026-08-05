package io.kudos.ability.distributed.notify.common.support

import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.init.properties.NotifyCommonProperties
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [NotifyTool].
 *
 * Covers all branches of `notify`:
 *  - producer present -> delegates and returns the producer's result (true / false), message passed through unchanged
 *  - producer absent + fail-on-missing-producer = true -> throws [IllegalStateException] with the documented message
 *  - producer absent + fail-on-missing-producer = false -> degrades: warns and returns false
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class NotifyToolTest {

    /** Recording fake producer returning a fixed result. */
    private class FakeProducer(private val result: Boolean) : INotifyProducer {
        var received: NotifyMessageVo<out Serializable>? = null
        var invocations = 0
        override fun notify(messageVo: NotifyMessageVo<out Serializable>): Boolean {
            invocations++
            received = messageVo
            return result
        }
    }

    @Test
    fun notify_withProducer_delegatesAndReturnsTrue() {
        val producer = FakeProducer(true)
        val tool = NotifyTool(producer, NotifyCommonProperties())
        val message = NotifyMessageVo("notify.tool.ok", "payload")

        assertTrue(tool.notify(message))
        assertEquals(1, producer.invocations)
        assertSame(message, producer.received)
    }

    @Test
    fun notify_withProducer_propagatesFalseResult() {
        val producer = FakeProducer(false)
        val tool = NotifyTool(producer, NotifyCommonProperties())

        assertFalse(tool.notify(NotifyMessageVo("notify.tool.fail", "payload")))
        assertEquals(1, producer.invocations)
    }

    @Test
    fun notify_withProducer_ignoresFailOnMissingProducerFlag() {
        // The flag only governs the missing-producer path; with a producer present it must not interfere.
        val producer = FakeProducer(true)
        val properties = NotifyCommonProperties().apply { failOnMissingProducer = true }
        val tool = NotifyTool(producer, properties)

        assertTrue(tool.notify(NotifyMessageVo("notify.tool.flag", "payload")))
    }

    @Test
    fun notify_withoutProducer_failFastEnabled_throwsIllegalState() {
        val properties = NotifyCommonProperties().apply { failOnMissingProducer = true }
        val tool = NotifyTool(null, properties)

        val ex = assertFailsWith<IllegalStateException> {
            tool.notify(NotifyMessageVo("notify.tool.missing", "payload"))
        }
        assertEquals("No INotifyProducer implementation registered", ex.message)
    }

    @Test
    fun notify_withoutProducer_failFastDisabled_warnsAndReturnsFalse() {
        val tool = NotifyTool(null, NotifyCommonProperties()) // failOnMissingProducer defaults to false

        assertFalse(tool.notify(NotifyMessageVo("notify.tool.degrade", "payload")))
    }

}
