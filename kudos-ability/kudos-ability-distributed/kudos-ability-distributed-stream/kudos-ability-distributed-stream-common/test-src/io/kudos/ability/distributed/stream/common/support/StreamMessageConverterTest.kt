package io.kudos.ability.distributed.stream.common.support

import io.kudos.ability.distributed.stream.common.model.vo.StreamMessageVo
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.support.GenericMessage
import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs


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
