package io.kudos.ability.distributed.stream.common.support

import io.kudos.ability.distributed.stream.common.handler.StreamFailHandlerItem
import io.kudos.ability.distributed.stream.common.init.properties.StreamProducerLimitProperties
import io.kudos.ability.distributed.stream.common.model.vo.StreamHeader
import io.kudos.ability.distributed.stream.common.model.vo.StreamMessageVo
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.config.BindingServiceProperties
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHandlingException
import org.springframework.messaging.support.ErrorMessage
import org.springframework.messaging.support.GenericMessage
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * Stream message producer helper.
 *
 * Provides a unified API for sending messages, supporting both synchronous and
 * asynchronous sends as well as failure handling and retry.
 *
 * Core features:
 * 1. Synchronous send: call sendMessage to send and immediately return the result.
 * 2. Asynchronous send: call asyncSendMessage to execute asynchronously via a thread
 *    pool without blocking the calling thread.
 * 3. Failure handling: when a send fails and a failure handler is configured, the
 *    failed message is pushed to the error channel for follow-up processing.
 * 4. Message wrapping: automatically creates the StreamMessageVo and adds required
 *    headers (binding name, destination, etc.).
 *
 * Notes:
 * - MQ sends are asynchronous; a true return from send does not guarantee the
 *   message reached the MQ server.
 * - The real send error is only observed on flush, which then triggers the failure
 *   handling mechanism.
 * - Custom failure handlers can be registered via StreamFailHandlerItem.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class StreamProducerHelper {

    @Resource
    private lateinit var streamBridge: StreamBridge

    @Resource
    private lateinit var properties: BindingServiceProperties

    @Resource
    private lateinit var streamAsyncSendExecutor: ThreadPoolTaskExecutor

    @Resource(name = "mqProducerChannel")
    private lateinit var mqProducerChannel: MessageChannel

    @Autowired(required = false)
    private var producerLimitProperties: StreamProducerLimitProperties? = null

    @Volatile
    private var producerLimiter: Semaphore? = null

    @Volatile
    private var producerLimiterSize: Int = 0

    /**
     * Sends a message.
     *
     * @param bindingName the stream binding name
     * @param data        the message body object
     * @param <T>         the type of the message body
    </T> */
    fun <T> sendMessage(bindingName: String, data: T): Boolean {
        if (!properties.bindings.containsKey(bindingName)) {
            LOG.error("Stream configuration item not found: {0}", bindingName)
            return false
        }
        if (!acquireProducerPermit(bindingName)) {
            return false
        }
        val msg = createMessage(bindingName, data)
        try {
            @Suppress("UNCHECKED_CAST")
            val success = doRealSend(bindingName, msg as Message<StreamMessageVo<Any?>>, false)
            if (!success) {
                LOG.warn("Stream send message result: false, bindingName: ${bindingName}, msgId: ${msg.headers.id}")
            }
            return success
        } finally {
            releaseProducerPermit()
        }
    }

    /**
     * Sends a message.
     *
     * @param bindingName the stream binding name
     * @param data        the message body object
     * @param <T>         the type of the message body
    </T> */
    fun <T> asyncSendMessage(bindingName: String, data: T) {
        if (!properties.getBindings().containsKey(bindingName)) {
            LOG.error("Stream configuration item not found: {0}", bindingName)
            return
        }
        if (!acquireProducerPermit(bindingName)) {
            return
        }
        val msg = createMessage(bindingName, data)
        try {
            streamAsyncSendExecutor.execute {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val success = doRealSend(bindingName, msg as Message<StreamMessageVo<Any?>>, false)
                    if (!success) {
                        LOG.warn("Stream send message result: false, bindingName: ${bindingName}, msgId: ${msg.headers.id}")
                    }
                } finally {
                    releaseProducerPermit()
                }
            }
        } catch (e: RuntimeException) {
            releaseProducerPermit()
            throw e
        }
    }

    /**
     * Actually sends the MQ message.
     *
     * Performs the real send operation and handles send failures.
     *
     * Workflow:
     * 1. Call StreamBridge to send the message to the given binding.
     * 2. On success, return true (note: MQ send is asynchronous; true does not mean
     *    the message reached the server).
     * 3. On failure (exception thrown):
     *    - Log the error.
     *    - If a failure handler is registered and this is not a retry, push the
     *      failed message to the error channel.
     *    - Return false.
     *
     * Failure handling:
     * - When the send fails and the binding has a failure handler registered (via
     *   StreamFailHandlerItem) and this is not a retry (isResend=false), the failed
     *   message is wrapped as an ErrorMessage and pushed to the error channel.
     * - The error channel triggers the failure handler, which typically persists the
     *   message to a local file.
     * - If this is a retry (isResend=true), the failure handler will not be
     *   re-triggered, avoiding duplicate persistence.
     *
     * Notes:
     * - MQ sends are asynchronous; a true return from send does not guarantee the
     *   message reached the MQ server.
     * - The real send error is only observed on flush, which then triggers the
     *   failure handling mechanism.
     * - On retry, isResend should be true to avoid re-triggering failure handling.
     *
     * @param bindingName the Stream binding name
     * @param msg the message to send
     * @param isResend true if this is a retry send, false if it is the first send
     * @return true if the send operation succeeded (asynchronously), false on failure
     * @param T the message body type
     */
    fun <T> doRealSend(bindingName: String, msg: Message<StreamMessageVo<T>>, isResend: Boolean): Boolean {
        try {
            // MQ sends here are asynchronous, so this almost always returns true; the actual error is only observed on flush.
            return streamBridge.send(bindingName, msg, StreamMessageConverter.MESSAGE_TYPE)
        } catch (e: Throwable) {
            // Best-effort fallback to avoid arbitrary errors.
            LOG.error(e, "Failed to send MQ message!")
            // Compatibility for the case where exception handling is not enabled. A failed resend returns false and does not need deletion.
            //properties.getBindings().get(bindingName).getProducer().isErrorChannelEnabled();
            if (StreamFailHandlerItem.hasFailedHandler(bindingName) && !isResend) {
                val exception = MessageHandlingException(msg)
                val data = ErrorMessage(exception)
                mqProducerChannel.send(data)
            }
            return false
        }
    }

    /**
     * 把业务数据包装成 Spring [Message]：headers 来自 [StreamHeader.initHeader] 默认值
     * + [StreamHeader.SCST_BIND_NAME] 标记当前 binding 名。
     *
     * SCST_BIND_NAME 是给全局异常处理器（`StreamGlobalExceptionHandler`）定位故障 binding 用的——
     * 缺失这个 header 异常持久化会找不到对应的 fail handler。
     *
     * @param T payload 类型
     * @param bindingName Stream binding 名（与 yml 中 bindings.{name}.destination 配套）
     * @param data 业务数据
     * @return 包装好的 Message
     * @throws IllegalArgumentException bindingName 未在 properties 中配置
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> createMessage(bindingName: String, data: T): Message<StreamMessageVo<T>> {
        val destination = requireNotNull(properties.bindings[bindingName]) { "Stream binding not configured: $bindingName" }.destination
        val headerMap = BeanKit.extract(StreamHeader.initHeader(destination))
        val map = mutableMapOf<String, Any>().apply {
            putAll(headerMap as Map<out String, Any>)
            put(StreamHeader.SCST_BIND_NAME, bindingName)
        }
        return GenericMessage(StreamMessageVo(data), org.springframework.messaging.MessageHeaders(map))
    }

    private fun acquireProducerPermit(bindingName: String): Boolean {
        val limitProperties = producerLimitProperties ?: return true
        if (!limitProperties.enabled) return true
        val limiter = getProducerLimiter(limitProperties.maxInFlight)
        val acquired = try {
            if (limitProperties.acquireTimeoutMillis <= 0) {
                limiter.tryAcquire()
            } else {
                limiter.tryAcquire(limitProperties.acquireTimeoutMillis, TimeUnit.MILLISECONDS)
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
        if (!acquired) {
            LOG.warn("Stream producer local rate limit triggered, skipping send. bindingName={0}", bindingName)
        }
        return acquired
    }

    private fun releaseProducerPermit() {
        if (producerLimitProperties?.enabled == true) {
            producerLimiter?.release()
        }
    }

    private fun getProducerLimiter(maxInFlight: Int): Semaphore {
        val size = maxInFlight.coerceAtLeast(1)
        val existing = producerLimiter
        if (existing != null && producerLimiterSize == size) {
            return existing
        }
        synchronized(this) {
            val current = producerLimiter
            if (current != null && producerLimiterSize == size) {
                return current
            }
            val created = Semaphore(size)
            producerLimiter = created
            producerLimiterSize = size
            return created
        }
    }

    private val LOG = LogFactory.getLog(this::class)

}
