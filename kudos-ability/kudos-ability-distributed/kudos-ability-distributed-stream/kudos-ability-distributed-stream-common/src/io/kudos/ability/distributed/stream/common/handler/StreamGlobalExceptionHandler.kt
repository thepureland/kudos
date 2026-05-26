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
 * Stream global exception handler — listens on the spring-integration error channel
 * plus a custom producer error channel.
 *
 * **Multiple @ServiceActivator on the same ERROR_CHANNEL is intentional**:
 *  - [globalHandleError] handles **consumer-side** exceptions (identified via `isFromConsumer`
 *    by headers prefixed with kafka_/amqp_/rocket_) → persists to the `sys_mq_fail_msg` table.
 *  - [handleProducerError] handles **producer-side** exceptions → calls
 *    `IStreamFailHandler.persistFailedData` for file persistence (typical implementation
 *    [StreamProducerExceptionHandler]).
 *  - [handSyncChannelError] listens on the custom [IStreamFailHandler.CHANNEL_BEN_NAME]
 *    (`mqProducerChannel`) — error messages are actively pushed into this channel by
 *    `StreamProducerHelper.doRealSend` when a synchronous send fails.
 *
 * The two listeners on ERROR_CHANNEL are not a bug — each `isFromConsumer` /
 * `containsKey(SCST_BIND_NAME)` guard performs routing.
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
     * Listens for global exception messages.
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
            LOG.warn("Received stream exception message, starting persistence...")
            val body = SerializationKit.deserialize(message.getPayload() as ByteArray)

            val exceptionMsg = SysMqFailMsg().apply {
                topic = headers.get(StreamHeader.TOPIC_KEY).toString()
                msgHeaderJson = JsonKit.toJson(headers).ifEmpty { headers.toString() }
                msgBodyJson = JsonKit.toJson(body)
                createTime = LocalDateTime.now()
            }
            val success = streamExceptionService.save(exceptionMsg)
            LOG.info("Stream exception message persistence result: {0}, id: {1}", success, headers.id)
        } catch (e: Exception) {
            LOG.error(e, "Failed to persist stream exception message")
        }
    }

    /**
     * Producer-side exception branch: consumer-side exceptions are intercepted by
     * [globalHandleError]; remaining producer-side exceptions are caught here and
     * delegated to [doRealChannelErrorHandle].
     *
     * The two listeners bound to ERROR_CHANNEL look redundant, but each
     * [isFromConsumer] / `containsKey(SCST_BIND_NAME)` guard performs routing — the
     * duplicate listener is required so that producer exceptions also enter the
     * persistence path; otherwise they would only print the stack and be lost.
     *
     * @param errorMessage the [ErrorMessage] dispatched by spring-integration
     * @author K
     * @since 1.0.0
     */
    @ServiceActivator(inputChannel = IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME)
    fun handleProducerError(errorMessage: ErrorMessage) {
        doRealChannelErrorHandle(errorMessage)
    }

    /**
     * Producer exception branch for synchronous send failures:
     * [IStreamFailHandler.CHANNEL_BEN_NAME] (i.e. `mqProducerChannel`) is dedicated
     * to error messages actively pushed by `StreamProducerHelper.doRealSend` when a
     * synchronous send fails.
     *
     * @param errorMessage the actively pushed [ErrorMessage]
     * @author K
     * @since 1.0.0
     */
    @ServiceActivator(inputChannel = IStreamFailHandler.CHANNEL_BEN_NAME)
    fun handSyncChannelError(errorMessage: ErrorMessage) {
        doRealChannelErrorHandle(errorMessage)
    }

    /**
     * Determines whether a message originates from the consumer side via header prefixes.
     *
     * Each Spring Cloud Stream binder writes built-in metadata into headers prefixed
     * with `kafka_*` / `amqp_*` / `rocket_*`; presence of any one of them is treated
     * as a consumer-side exception. Explicit `Locale.ROOT` before comparison avoids
     * the Turkish locale `i → İ` case-mapping mistakenly treating `"kafka_*"` as a
     * non-match.
     *
     * @param headers message headers to inspect
     * @return true if the message comes from a consumer
     * @author K
     * @since 1.0.0
     */
    private fun isFromConsumer(headers: MessageHeaders): Boolean =
        headers.keys.any { key ->
            val k = key.lowercase(Locale.ROOT)
            k.startsWith("kafka_") || k.startsWith("amqp_") || k.startsWith("rocket_")
        }

    /**
     * Actual handling of a producer exception: tries to obtain the original Message
     * prior to failure and hands it to [processProducerError]. Prefers
     * [MessageHandlingException.failedMessage]; falls back to
     * `errorMessage.originalMessage` when null, to accommodate differences in how
     * binders propagate messages along the error path.
     *
     * @param errorMessage the error message arriving on the error channel
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
            LOG.error(e, "File persistence failed!")
        }
    }

    /**
     * Actual handling of producer-side failure:
     * 1. Defensive `isFromConsumer` re-check to avoid misrouting consumer-side
     *    exceptions into the producer branch under legacy routing compatibility.
     * 2. Ignore when the `SCST_BIND_NAME` header is missing — without binding info
     *    we cannot locate the specific fail handler.
     * 3. Deserialize the payload as [StreamMessageVo], build a [StreamProducerMsgVo],
     *    and hand it to the handler registered with [StreamFailHandlerItem] for
     *    persistence.
     *
     * @param message the failed message
     * @author K
     * @since 1.0.0
     */
    private fun processProducerError(message: Message<*>) {
        // Awkward but necessary compatibility for the global exception issue.
        if (isFromConsumer(message.headers)) return
        if (!message.headers.containsKey(StreamHeader.SCST_BIND_NAME)) {
            LOG.debug("No binding info found for the exception, ignoring...")
            return
        }
        LOG.warn("Received stream exception message, starting persistence...")
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

    /** Logger */
    private val LOG = LogFactory.getLog(this::class)

}
