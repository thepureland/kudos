package io.kudos.ability.distributed.stream.common.support

import io.kudos.base.lang.SerializationKit
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.converter.AbstractMessageConverter
import org.springframework.util.MimeType
import java.io.Serializable

/**
 * 流式消息转换器
 * 
 * 使用JDK序列化方式实现消息的序列化和反序列化，支持任意可序列化对象。
 * 
 * 核心功能：
 * 1. 消息序列化：将Java对象序列化为字节数组
 * 2. 消息反序列化：将字节数组反序列化为Java对象
 * 3. 类型支持：支持所有实现Serializable接口的对象
 * 
 * 序列化方式：
 * - 使用JDK原生的ObjectOutputStream/ObjectInputStream
 * - 通过SerializationKit封装序列化逻辑
 * - 支持任意实现Serializable接口的对象
 * 
 * MIME类型：
 * - 使用"application/JdkSerializa"作为消息类型标识
 * - Spring Cloud Stream根据此类型选择转换器
 * 
 * 注意事项：
 * - 消息体类必须实现Serializable接口
 * - 序列化后的字节数组会作为消息的payload
 * - 反序列化时使用非空断言（!!），确保类型正确
 */
class StreamMessageConverter : AbstractMessageConverter(MESSAGE_TYPE) {

    /**
     * 判断是否支持指定的类型
     * 
     * 此转换器支持所有类型，因为JDK序列化可以序列化任何实现Serializable接口的对象。
     * 
     * @param clazz 目标类型
     * @return 始终返回true，表示支持所有类型
     */
    override fun supports(clazz: Class<*>): Boolean {
        return true
    }

    /**
     * 从消息中反序列化对象
     * 
     * 将消息的payload（字节数组）反序列化为Java对象。
     * 
     * 工作流程：
     * 1. 获取消息payload：从Message中提取payload
     * 2. 类型转换：将payload转换为ByteArray
     * 3. 反序列化：使用SerializationKit.deserialize将字节数组反序列化为对象
     * 4. 返回结果：返回反序列化后的对象
     * 
     * 注意事项：
     * - payload必须是ByteArray类型
     * - 使用非空断言（!!），确保反序列化结果不为null
     * - 如果反序列化失败，会抛出相应的异常
     * 
     * @param message Spring消息对象
     * @param targetClass 目标类型（此参数在此实现中未使用）
     * @param conversionHint 转换提示（此参数在此实现中未使用）
     * @return 反序列化后的对象
     * @throws ClassNotFoundException 如果类找不到
     * @throws IOException 如果IO操作失败
     */
    override fun convertFromInternal(
        message: Message<*>,
        targetClass: Class<*>,
        conversionHint: Any?
    ): Any {
        val payload = message.getPayload()
        return SerializationKit.deserialize(payload as ByteArray)!!
    }

    /**
     * 将对象序列化为消息内部格式
     * 
     * 将Java对象序列化为字节数组，作为消息的payload。
     * 
     * 工作流程：
     * 1. 类型检查：确保payload实现Serializable接口
     * 2. 序列化：使用SerializationKit.serialize将对象序列化为字节数组
     * 3. 返回结果：返回序列化后的字节数组
     * 
     * 注意事项：
     * - payload必须实现Serializable接口
     * - 序列化后的字节数组会作为消息的payload
     * - 如果序列化失败，会抛出相应的异常
     * 
     * @param payload 待序列化的对象
     * @param headers 消息头（此参数在此实现中未使用）
     * @param conversionHint 转换提示（此参数在此实现中未使用）
     * @return 序列化后的字节数组
     * @throws NotSerializableException 如果对象未实现Serializable接口
     * @throws IOException 如果IO操作失败
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
         * 消息MIME类型
         * 
         * 标识使用JDK序列化的消息类型，Spring Cloud Stream根据此类型选择转换器。
         */
        val MESSAGE_TYPE = MimeType("application", "JdkSerializa")
    }
}
