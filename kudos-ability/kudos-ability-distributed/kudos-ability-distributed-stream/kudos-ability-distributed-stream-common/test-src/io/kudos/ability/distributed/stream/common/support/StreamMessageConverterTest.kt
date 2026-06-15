package io.kudos.ability.distributed.stream.common.support

import io.kudos.ability.distributed.stream.common.model.vo.StreamMessageVo
import io.kudos.base.lang.SerializationKit
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.support.GenericMessage
import java.io.Serializable
import java.lang.reflect.InvocationTargetException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for [StreamMessageConverter]:
 * - JDK-serialization round trip through convertToInternal / convertFromInternal
 * - [StreamMessageConverter.supports] accepting any type
 * - the wire-compatible MIME type constant `application/JdkSerializa`
 * - convertFromInternal rejecting payloads that deserialize to null with an
 *   IllegalArgumentException carrying the documented message
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class StreamMessageConverterTest {

    @Test
    fun convertToAndFromInternal_roundTripsSerializablePayload() {
        val converter = TestStreamMessageConverter()
        val source = StreamMessageVo(TestPayload("alpha", 7))

        val bytes = converter.toInternal(source)
        val restored = converter.fromInternal(GenericMessage(bytes), StreamMessageVo::class.java)

        val message = assertIs<StreamMessageVo<*>>(restored)
        assertEquals(source.data, message.data)
    }

    @Test
    fun supports_acceptsAnyType() {
        val method = StreamMessageConverter::class.java.getDeclaredMethod("supports", Class::class.java)
        method.isAccessible = true
        assertEquals(true, method.invoke(StreamMessageConverter(), String::class.java))
        assertEquals(true, method.invoke(StreamMessageConverter(), IntArray::class.java))
    }

    @Test
    fun messageType_keepsWireCompatibleMimeType() {
        // MimeType normalizes type and subtype to lowercase; equality stays case-insensitive.
        assertEquals("application", StreamMessageConverter.MESSAGE_TYPE.type)
        assertEquals(org.springframework.util.MimeType("application", "JdkSerializa"), StreamMessageConverter.MESSAGE_TYPE)
    }

    @Test
    fun convertFromInternal_failsWhenPayloadDeserializesToNull() {
        val converter = TestStreamMessageConverter()
        val nullBytes = SerializationKit.serialize(null)

        val e = assertFailsWith<InvocationTargetException> {
            converter.fromInternal(GenericMessage(nullBytes), StreamMessageVo::class.java)
        }
        val cause = assertIs<IllegalArgumentException>(e.cause)
        assertTrue(cause.message!!.contains("deserialize returned null"), "unexpected message: ${cause.message}")
    }

    private data class TestPayload(
        val name: String,
        val count: Int
    ) : Serializable

    private class TestStreamMessageConverter {
        private val delegate = StreamMessageConverter()

        fun toInternal(payload: Any): Any {
            val method = StreamMessageConverter::class.java.getDeclaredMethod(
                "convertToInternal",
                Any::class.java,
                MessageHeaders::class.java,
                Any::class.java
            )
            method.isAccessible = true
            return method.invoke(delegate, payload, null, null)
        }

        fun fromInternal(message: GenericMessage<Any>, targetClass: Class<*>): Any {
            val method = StreamMessageConverter::class.java.getDeclaredMethod(
                "convertFromInternal",
                Message::class.java,
                Class::class.java,
                Any::class.java
            )
            method.isAccessible = true
            return method.invoke(delegate, message, targetClass, null)
        }
    }

}
