package io.kudos.ability.distributed.stream.common.support

import io.kudos.base.lang.SerializationKit
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.converter.AbstractMessageConverter
import org.springframework.util.MimeType
import java.io.Serializable

/** Legacy JDK wire converter with a shared class allowlist and bounded deserialization. */
class StreamMessageConverter(
    private val serialization: StreamMessageSerialization = StreamMessageSerialization()
) : AbstractMessageConverter(MESSAGE_TYPE) {
    override fun supports(clazz: Class<*>): Boolean = true

    override fun convertFromInternal(message: Message<*>, targetClass: Class<*>, conversionHint: Any?): Any =
        serialization.deserialize(message.payload as ByteArray, targetClass)

    override fun convertToInternal(payload: Any, headers: MessageHeaders?, conversionHint: Any?): Any =
        SerializationKit.serialize(payload as Serializable)

    companion object {
        val MESSAGE_TYPE = MimeType("application", "JdkSerializa")
    }
}
