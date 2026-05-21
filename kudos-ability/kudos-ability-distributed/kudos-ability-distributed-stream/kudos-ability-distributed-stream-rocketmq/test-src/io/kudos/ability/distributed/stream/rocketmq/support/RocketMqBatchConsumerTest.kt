package io.kudos.ability.distributed.stream.rocketmq.support

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.io.InvalidClassException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class RocketMqBatchConsumerTest {

    @Test
    fun decodeJdkBody_allowsClassMatchedByFilter() {
        val payload = TestPayload("ok")

        val decoded = RocketMqBatchConsumer.decodeJdkBody(
            serialize(payload),
            "io.kudos.ability.distributed.stream.rocketmq.support.*;java.base/*;!*"
        )

        assertEquals(payload, decoded)
    }

    @Test
    fun decodeJdkBody_rejectsClassNotMatchedByFilter() {
        val payload = TestPayload("blocked")

        assertFailsWith<InvalidClassException> {
            RocketMqBatchConsumer.decodeJdkBody(serialize(payload), "java.base/*;!*")
        }
    }

    private fun serialize(value: Serializable): ByteArray {
        val output = ByteArrayOutputStream()
        ObjectOutputStream(output).use { it.writeObject(value) }
        return output.toByteArray()
    }

    private data class TestPayload(val value: String) : Serializable
}
