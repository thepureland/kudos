package io.kudos.ability.distributed.stream.common.handler

import io.kudos.ability.distributed.stream.common.biz.ISysMqFailMsgService
import io.kudos.ability.distributed.stream.common.model.po.SysMqFailMsg
import io.kudos.ability.distributed.stream.common.model.vo.StreamHeader
import io.kudos.ability.distributed.stream.common.model.vo.StreamMessageVo
import io.kudos.ability.distributed.stream.common.model.vo.StreamProducerMsgVo
import io.kudos.base.data.json.JsonKit
import io.kudos.base.lang.SerializationKit
import io.kudos.base.logger.LogFactory
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Value
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.context.IntegrationContextUtils
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHandlingException
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.support.ErrorMessage
import java.time.LocalDateTime
import java.util.Locale

/**
 * Stream 全局异常处理——监听 spring-integration error channel + 自定义 producer error channel。
 *
 * **多个 @ServiceActivator 在同一 ERROR_CHANNEL 上是有意为之**：
 *  - [globalHandleError] 负责 **consumer 端**异常（通过 `isFromConsumer` 识别 kafka_/amqp_/rocket_
 *    前缀的 headers）→ 持久化到 `sys_mq_fail_msg` 表
 *  - [handleProducerError] 负责 **producer 端**异常 → 调 `IStreamFailHandler.persistFailedData`
 *    走文件持久化（典型实现 [StreamProducerExceptionHandler]）
 *  - [handSyncChannelError] 监听自定义的 [IStreamFailHandler.CHANNEL_BEN_NAME]
 *    （`mqProducerChannel`）—— 同步发送失败时由 `StreamProducerHelper.doRealSend` 主动塞
 *    错误消息到这个通道
 *
 * 两条 ERROR_CHANNEL 上的 listener 不是 bug——各自的 `isFromConsumer` / `containsKey(SCST_BIND_NAME)`
 * 守卫做了分流。
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
class StreamGlobalExceptionHandler {

    @Value($$"${kudos.ability.distributed.stream.save-exception:true}")
    private val saveException = true

    @Resource
    private lateinit var streamExceptionService: ISysMqFailMsgService

    /**
     * 监听全局异常消息
     *
     * @param errorMessage
     */
    @ServiceActivator(inputChannel = IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME)
    fun globalHandleError(errorMessage: ErrorMessage) {
        if (!saveException) return
        try {
            val handlingException = errorMessage.payload as? MessageHandlingException ?: return
            val message = handlingException.failedMessage ?: return
            val headers = message.headers
            if (!isFromConsumer(headers)) return
            LOG.warn("收到stream异常消息,开始持久化...")
            val body = SerializationKit.deserialize(message.getPayload() as ByteArray)

            val exceptionMsg = SysMqFailMsg().apply {
                topic = headers.get(StreamHeader.TOPIC_KEY).toString()
                msgHeaderJson = JsonKit.toJson(headers).ifEmpty { headers.toString() }
                msgBodyJson = JsonKit.toJson(body)
                createTime = LocalDateTime.now()
            }
            val success = streamExceptionService.save(exceptionMsg)
            LOG.info("stream异常消息持久化结果:{0},id:{1}", success, headers.id)
        } catch (e: Exception) {
            LOG.error(e, "stream异常消息持久化失败")
        }
    }

    @ServiceActivator(inputChannel = IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME)
    fun handleProducerError(errorMessage: ErrorMessage) {
        doRealChannelErrorHandle(errorMessage)
    }

    @ServiceActivator(inputChannel = IStreamFailHandler.CHANNEL_BEN_NAME)
    fun handSyncChannelError(errorMessage: ErrorMessage) {
        doRealChannelErrorHandle(errorMessage)
    }

    // Locale.ROOT 而非 Locale.getDefault()——header key 用 ASCII 前缀对比，Turkish locale
    // 的 i→İ 大小写映射会让 "kafka_..." 误判
    private fun isFromConsumer(headers: MessageHeaders): Boolean =
        headers.keys.any { key ->
            val k = key.lowercase(Locale.ROOT)
            k.startsWith("kafka_") || k.startsWith("amqp_") || k.startsWith("rocket_")
        }

    private fun doRealChannelErrorHandle(errorMessage: ErrorMessage) {
        try {
            val message = (errorMessage.payload as? MessageHandlingException)?.failedMessage
                ?: errorMessage.originalMessage
                ?: return
            processProducerError(message)
        } catch (e: Exception) {
            LOG.error(e, "文件持久化失败！")
        }
    }

    private fun processProducerError(message: Message<*>) {
        //比较恶心的兼容全局异常问题。
        if (isFromConsumer(message.headers)) return
        if (!message.headers.containsKey(StreamHeader.SCST_BIND_NAME)) {
            LOG.debug("找不到异常的绑定信息，忽略...")
            return
        }
        LOG.warn("收到stream异常消息,开始持久化...")
        val headers = message.headers
        val body = SerializationKit.deserialize(message.getPayload() as ByteArray) as? StreamMessageVo<*>
            ?: return
        val bindName = headers.get(StreamHeader.SCST_BIND_NAME).toString()
        val producerMsgVo = StreamProducerMsgVo().apply {
            this.bindName = bindName
            msgHeaderJson = JsonKit.toJson(headers).ifEmpty { headers.toString() }
            msgBodyJson = JsonKit.toJson(body.data)
        }
        StreamFailHandlerItem.get(bindName)?.persistFailedData(producerMsgVo)
    }

    private val LOG = LogFactory.getLog(this::class)

}
