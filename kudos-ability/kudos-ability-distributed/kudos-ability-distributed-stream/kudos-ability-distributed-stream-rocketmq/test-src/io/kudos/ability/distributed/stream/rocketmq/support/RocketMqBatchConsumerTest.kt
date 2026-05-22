package io.kudos.ability.distributed.stream.rocketmq.support

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.io.InvalidClassException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * [RocketMqBatchConsumer] JDK 反序列化过滤策略测试。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
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

    /**
     * RocketMQ 批量消费反序列化测试 DTO。
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private data class TestPayload(val value: String) : Serializable
}
