package io.kudos.ability.distributed.stream.common.support

import io.kudos.base.lang.SerializationKit
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.converter.AbstractMessageConverter
import org.springframework.util.MimeType
import java.io.Serializable

/**
 * Stream message converter.
 *
 * Implements message serialization and deserialization using JDK serialization;
 * supports any serializable object.
 *
 * Core features:
 * 1. Message serialization: serializes a Java object into a byte array.
 * 2. Message deserialization: deserializes a byte array into a Java object.
 * 3. Type support: supports all objects implementing the Serializable interface.
 *
 * Serialization approach:
 * - Uses the JDK-native ObjectOutputStream / ObjectInputStream.
 * - Wraps the serialization logic via SerializationKit.
 * - Supports any object implementing the Serializable interface.
 *
 * MIME type:
 * - Uses "application/JdkSerializa" as the message type identifier.
 * - Spring Cloud Stream picks the converter based on this type.
 *
 * Notes:
 * - The message body class must implement Serializable.
 * - The serialized byte array becomes the message payload.
 * - If deserialization returns null, IllegalArgumentException is thrown.
 */
class StreamMessageConverter : AbstractMessageConverter(MESSAGE_TYPE) {

    /**
     * Determines whether the given type is supported.
     *
     * This converter supports all types, because JDK serialization can serialize any
     * object implementing the Serializable interface.
     *
     * @param clazz the target type
     * @return always true, indicating all types are supported
     */
    override fun supports(clazz: Class<*>): Boolean {
        return true
    }

    /**
     * Deserializes the object from the message.
     *
     * Deserializes the message payload (byte array) into a Java object.
     *
     * Workflow:
     * 1. Get the payload from the Message.
     * 2. Cast the payload to ByteArray.
     * 3. Deserialize the byte array into an object via SerializationKit.deserialize.
     * 4. Return the deserialized object.
     *
     * Notes:
     * - The payload must be a ByteArray.
     * - Throws IllegalArgumentException when deserialization returns null.
     * - Any deserialization failure surfaces as the corresponding exception.
     *
     * @param message the Spring Message object
     * @param targetClass the target type (unused in this implementation)
     * @param conversionHint the conversion hint (unused in this implementation)
     * @return the deserialized object
     * @throws ClassNotFoundException if the class is not found
     * @throws IOException if an I/O operation fails
     */
    override fun convertFromInternal(
        message: Message<*>,
        targetClass: Class<*>,
        conversionHint: Any?
    ): Any {
        val payload = message.getPayload()
        return requireNotNull(SerializationKit.deserialize(payload as ByteArray)) { "deserialize returned null" }
    }

    /**
     * Serializes the object into the internal message format.
     *
     * Serializes a Java object into a byte array used as the message payload.
     *
     * Workflow:
     * 1. Type check: ensure the payload implements Serializable.
     * 2. Serialize: convert the object to a byte array via SerializationKit.serialize.
     * 3. Return the serialized byte array.
     *
     * Notes:
     * - The payload must implement Serializable.
     * - The serialized byte array becomes the message payload.
     * - Any serialization failure surfaces as the corresponding exception.
     *
     * @param payload the object to serialize
     * @param headers message headers (unused in this implementation)
     * @param conversionHint conversion hint (unused in this implementation)
     * @return the serialized byte array
     * @throws NotSerializableException if the object does not implement Serializable
     * @throws IOException if an I/O operation fails
     */
    override fun convertToInternal(
        payload: Any,
        headers: MessageHeaders?,
        conversionHint: Any?
    ): Any {
        return SerializationKit.serialize(payload as Serializable)
    }

    companion object Companion {
        /**
         * Message MIME type.
         *
         * Identifies messages using JDK serialization; Spring Cloud Stream selects the
         * converter based on this type.
         */
        val MESSAGE_TYPE = MimeType("application", "JdkSerializa")
    }
}
