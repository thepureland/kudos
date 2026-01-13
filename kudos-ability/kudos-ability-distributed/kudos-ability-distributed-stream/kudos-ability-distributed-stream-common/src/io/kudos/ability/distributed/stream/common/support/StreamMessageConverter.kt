package io.kudos.ability.distributed.stream.common.support

import io.kudos.base.lang.SerializationKit
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.converter.AbstractMessageConverter
import org.springframework.util.MimeType
import java.io.Serializable

/**
 * 流式消息转换器
 * 使用JDK序列化方式实现消息的序列化和反序列化，支持任意可序列化对象
 */
class StreamMessageConverter : AbstractMessageConverter(MESSAGE_TYPE) {

    override fun supports(clazz: Class<*>): Boolean {
        return true
    }

    override fun convertFromInternal(
        message: Message<*>,
        targetClass: Class<*>,
        conversionHint: Any?
    ): Any {
        val payload = message.getPayload()
        return SerializationKit.deserialize(payload as ByteArray)!!
    }

    override fun convertToInternal(
        payload: Any,
        headers: MessageHeaders?,
        conversionHint: Any?
    ): Any {
        return SerializationKit.serialize(payload as Serializable)
    }

    companion object Companion {
        val MESSAGE_TYPE = MimeType("application", "JdkSerializa")
    }
}
