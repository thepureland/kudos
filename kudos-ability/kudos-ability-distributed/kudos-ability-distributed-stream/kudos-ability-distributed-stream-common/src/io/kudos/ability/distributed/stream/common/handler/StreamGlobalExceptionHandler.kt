package io.kudos.ability.distributed.stream.common.handler

import io.kudos.ability.distributed.stream.common.biz.ISysMqFailMsgService
import io.kudos.ability.distributed.stream.common.model.po.SysMqFailMsg
import io.kudos.ability.distributed.stream.common.model.vo.StreamHeader
import io.kudos.ability.distributed.stream.common.model.vo.StreamMessageVo
import io.kudos.ability.distributed.stream.common.model.vo.StreamProducerMsgVo
import io.kudos.base.data.json.JsonKit
import io.kudos.base.lang.SerializationKit
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.context.IntegrationContextUtils
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHandlingException
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.support.ErrorMessage
import java.time.LocalDateTime
import java.util.*

/**
 * @Description Stream消费异常全局处理类
 * @Author paul
 * @Date 2022/9/21 18:23
 */
class StreamGlobalExceptionHandler {

    @Value("\${kudos.ability.distributed.stream.save-exception:true}")
    private val saveException = true

    @Autowired
    private lateinit var streamExceptionService: ISysMqFailMsgService

    /**
     * 监听全局异常消息
     *
     * @param errorMessage
     */
    @ServiceActivator(inputChannel = IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME)
    fun globalHandleError(errorMessage: ErrorMessage) {
        if (!saveException) {
            return
        }
        try {
            val messageHandlingException = errorMessage.payload
            if (messageHandlingException is MessageHandlingException) {
                val message = messageHandlingException.failedMessage
                val headers = message!!.headers
                if (!isFromConsumer(headers)) {
                    return
                }
                LOG.warn("收到stream异常消息,开始持久化...")
                val body = SerializationKit.deserialize(message.getPayload() as ByteArray)

                val topic = headers.get(StreamHeader.TOPIC_KEY).toString()
                var msgHeaderJson = JsonKit.toJson(headers)
                if (msgHeaderJson.isEmpty()) {
                    msgHeaderJson = headers.toString()
                }
                val msgBodyJson = JsonKit.toJson(body)

                //保存异常消息
                val exceptionMsg = SysMqFailMsg()
                exceptionMsg.topic = topic
                exceptionMsg.msgHeaderJson = msgHeaderJson
                exceptionMsg.msgBodyJson = msgBodyJson
                exceptionMsg.createTime = LocalDateTime.now()
                val success = streamExceptionService.save(exceptionMsg)
                LOG.info("stream异常消息持久化结果:{0},id:{1}", success, headers.id)
            }
        } catch (e: Exception) {
            LOG.error(e, "stream异常消息持久化失败")
        }
    }

    @ServiceActivator(inputChannel = IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME)
    fun handleProducerError(errorMessage: ErrorMessage) {
        doRealChannelErrorHandl(errorMessage)
    }

    @ServiceActivator(inputChannel = IStreamFailHandler.Companion.CHANNEL_BEN_NAME)
    fun handSyncChannelError(errorMessage: ErrorMessage) {
        doRealChannelErrorHandl(errorMessage)
    }

    private fun isFromConsumer(headers: MessageHeaders): Boolean {
        return headers.keys.stream()
            .map<String?> { obj: String? -> obj!!.lowercase(Locale.getDefault()) }
            .anyMatch { k: String? ->
                k!!.startsWith("kafka_")
                        || k.startsWith("amqp_")
                        || k.startsWith("rocket_")
            }
    }

    private fun doRealChannelErrorHandl(errorMessage: ErrorMessage) {
        try {
            val messageHandlingException = errorMessage.payload
            if (messageHandlingException is MessageHandlingException) {
                // 取出 MessagingException 和原始消息
                val message = messageHandlingException.failedMessage
                processProducerError(message!!)
            } else if (errorMessage.originalMessage != null) {
                val message = errorMessage.originalMessage
                processProducerError(message!!)
            }
        } catch (e: Exception) {
            LOG.error(e, "文件持久化失败！")
        }
    }

    private fun processProducerError(message: Message<*>) {
        if (isFromConsumer(message.headers)) {
            //比较恶心的兼容全局异常问题。
            return
        }
        val haveProducerBindName = message.headers.containsKey(StreamHeader.SCST_BIND_NAME)
        if (!haveProducerBindName) {
            LOG.debug("找不到异常的绑定信息，忽略...")
            return
        }
        LOG.warn("收到stream异常消息,开始持久化...")
        val headers = message.headers
        val body: Any? = SerializationKit.deserialize(message.getPayload() as ByteArray)
        if (body is StreamMessageVo<*>) {
            val bindName = headers.get(StreamHeader.SCST_BIND_NAME).toString()
            var msgHeaderJson = JsonKit.toJson(headers)
            if (msgHeaderJson.isEmpty()) {
                msgHeaderJson = headers.toString()
            }
            val msgBodyJson = JsonKit.toJson(body.data)
            val producerMsgVo = StreamProducerMsgVo()
            producerMsgVo.bindName = bindName
            producerMsgVo.msgHeaderJson = msgHeaderJson
            producerMsgVo.msgBodyJson = msgBodyJson
            val handler = StreamFailHandlerItem.get(bindName)
            handler?.persistFailedData(producerMsgVo)
        }
    }

    private val LOG = LogFactory.getLog(this)

}
