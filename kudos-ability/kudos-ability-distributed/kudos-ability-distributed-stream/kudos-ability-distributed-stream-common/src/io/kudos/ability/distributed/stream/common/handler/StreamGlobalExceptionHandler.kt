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
 * @author AI: Codex
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

    /**
     * Producer 端异常分支：消费者端异常被 [globalHandleError] 拦下，剩余的 producer 端
     * 异常被本方法捕获后委托给 [doRealChannelErrorHandle]。
     *
     * 两个 listener 同绑 ERROR_CHANNEL 看起来重复，但由各自的 [isFromConsumer]
     * / `containsKey(SCST_BIND_NAME)` 守卫做了路由——重复挂监听是为了让 producer
     * 异常也能进入持久化路径，否则只会 print stack 然后丢失。
     *
     * @param errorMessage spring-integration 派发过来的 [ErrorMessage]
     * @author K
     * @since 1.0.0
     */
    @ServiceActivator(inputChannel = IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME)
    fun handleProducerError(errorMessage: ErrorMessage) {
        doRealChannelErrorHandle(errorMessage)
    }

    /**
     * 同步发送失败的 producer 异常分支：[IStreamFailHandler.CHANNEL_BEN_NAME]
     * （即 `mqProducerChannel`）专门给 `StreamProducerHelper.doRealSend` 同步发送失败时
     * 主动 push 的错误消息使用。
     *
     * @param errorMessage 主动塞过来的 [ErrorMessage]
     * @author K
     * @since 1.0.0
     */
    @ServiceActivator(inputChannel = IStreamFailHandler.CHANNEL_BEN_NAME)
    fun handSyncChannelError(errorMessage: ErrorMessage) {
        doRealChannelErrorHandle(errorMessage)
    }

    /**
     * 通过 header 前缀判定该消息是否来自 consumer 端。
     *
     * Spring Cloud Stream 各 binder 都会把内置元数据写到 `kafka_*` / `amqp_*` / `rocket_*` 头里，
     * 只要消息含其一就视为消费侧异常。比对前显式 `Locale.ROOT` 是为了避免 Turkish locale
     * `i → İ` 大小写映射把 `"kafka_*"` 误判为不匹配。
     *
     * @param headers 待检查的消息头
     * @return true 表示来自 consumer
     * @author K
     * @since 1.0.0
     */
    private fun isFromConsumer(headers: MessageHeaders): Boolean =
        headers.keys.any { key ->
            val k = key.lowercase(Locale.ROOT)
            k.startsWith("kafka_") || k.startsWith("amqp_") || k.startsWith("rocket_")
        }

    /**
     * 实际处理 producer 异常：尽量取出失败前的原始 Message 再交给 [processProducerError]。
     * 优先取 [MessageHandlingException.failedMessage]；它为空时退回 `errorMessage.originalMessage`，
     * 兼容不同 binder 在异常路径上传递消息的差异。
     *
     * @param errorMessage 进入 error channel 的错误消息
     * @author K
     * @since 1.0.0
     */
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

    /**
     * 真正处理 producer 端失败：
     * 1. 再次 `isFromConsumer` 防御，避免兼容历史路由时把消费端异常误进 producer 分支；
     * 2. 没有 `SCST_BIND_NAME` 头就忽略——没绑定信息无法定位到具体 fail handler；
     * 3. 反序列化 payload 为 [StreamMessageVo] 后构造 [StreamProducerMsgVo]，
     *    交给 [StreamFailHandlerItem] 注册的对应 handler 持久化。
     *
     * @param message 失败消息
     * @author K
     * @since 1.0.0
     */
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
            msgBodyClassName = body.data?.javaClass?.name
        }
        StreamFailHandlerItem.get(bindName)?.persistFailedData(producerMsgVo)
    }

    /** 日志器 */
    private val LOG = LogFactory.getLog(this::class)

}
