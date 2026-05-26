package io.kudos.ability.distributed.stream.common.handler

import io.kudos.base.data.json.JsonKit
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for [StreamProducerExceptionHandler] failed-message body deserialization.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class StreamProducerExceptionHandlerTest {

    @Test
    fun readMessageBody_restoresPayloadByPersistedClassName() {
        val handler = StreamProducerExceptionHandler()
        val payload = RetryPayload("order-1", 3)

        val restored = handler.readMessageBody(JsonKit.toJson(payload), RetryPayload::class.java.name)

        assertEquals(payload, assertIs<RetryPayload>(restored))
    }

    @Test
    fun readMessageBody_fallsBackToDynamicJsonWhenClassNameIsMissing() {
        val handler = StreamProducerExceptionHandler()

        val restored = handler.readMessageBody("""{"id":"order-2","count":5}""", null)

        val map = assertIs<Map<*, *>>(restored)
        assertEquals("order-2", map["id"])
        assertEquals(5, map["count"])
    }

    private fun StreamProducerExceptionHandler.readMessageBody(msgBodyJson: String, className: String?): Any? {
        val method = StreamProducerExceptionHandler::class.java.getDeclaredMethod(
            "readMessageBody",
            String::class.java,
            String::class.java
        )
        method.isAccessible = true
        return method.invoke(this, msgBodyJson, className)
    }

}

/**
 * Test DTO for stream producer retry payloads.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Serializable
internal data class RetryPayload(
    val id: String,
    val count: Int
)
