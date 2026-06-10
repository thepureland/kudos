package io.kudos.ability.distributed.stream.rocketmq.support

import com.alibaba.fastjson2.JSONObject
import io.kudos.ability.distributed.stream.common.biz.ISysMqFailMsgService
import io.kudos.ability.distributed.stream.common.model.po.SysMqFailMsg
import io.kudos.ability.distributed.stream.rocketmq.init.properties.RocketMqProperties
import io.kudos.base.logger.LogFactory
import io.kudos.context.kit.SpringKit
import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer
import org.apache.rocketmq.client.exception.MQClientException
import org.apache.rocketmq.common.message.MessageExt
import java.io.ByteArrayInputStream
import java.io.ObjectInputFilter
import java.io.ObjectInputStream
import java.time.LocalDateTime
import kotlin.concurrent.thread

/**
 * RocketMQ batch message consumer.
 *
 * Supports batch pulling and consumption of RocketMQ messages, with failed-message persistence and a retry mechanism.
 *
 * Core features:
 * 1. Batch pulling: uses DefaultLitePullConsumer to pull messages in batches, improving consumption efficiency
 * 2. Batch processing: supports a configurable batch size (batchProcessSize); processing is triggered when the
 *    threshold or timeout is reached
 * 3. Periodic commit: even when no new messages are pulled, commits at the pullTime interval to ensure timely processing
 * 4. Exception handling: supports persisting failed messages to the database, preventing MQ backlog
 * 5. Graceful shutdown: registers a JVM shutdown hook to stop the consumer correctly on application exit
 *
 * Workflow:
 * - On startup, creates a daemon thread to continuously pull messages
 * - Batch processing is triggered when the message count reaches batchProcessSize or pullTime elapses since the last commit
 * - During batch processing, deserializes the message bodies and invokes the business handler
 * - On success, commits the consumption offset; on failure, the saveException configuration decides whether to persist exception data
 *
 * Notes:
 * - Offset commit uses manual mode (autoCommit=false) to ensure offsets are committed only after successful processing
 * - When exception persistence is enabled, the offset is committed even on failure to avoid repeated consumption and backlog
 * - Message bodies use JDK serialization; the message body class must implement Serializable
 *
 * @param T Message body type
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class RocketMqBatchConsumer<T> @JvmOverloads constructor(
    groupName: String?,
    topic: String?,
    batchProcessSize: Int,
    pullTime: Long,
    private val saveException: Boolean = false,
    private val rocketMqProperties: RocketMqProperties = RocketMqProperties.instance,
    private val failMsgServiceProvider: () -> ISysMqFailMsgService = { SpringKit.getBean() }
) {

    /**
     * Running flag. Written by the shutdown-hook thread ([destroy]) and read by the daemon
     * polling thread — must be @Volatile, otherwise the loop may never observe the stop signal.
     */
    @Volatile
    private var isRunning = false
    private var daemonThread: Thread? = null
    private val consumer: DefaultLitePullConsumer
    private var pullTime: Long = 5000
    private var batchProcessSize = 1000
    private var bizBatchProcess: ((List<BatchConsumerItem<T>>) -> Unit)? = null
    private val groupName: String?
    private val topic: String?

    /**
     * @param groupName     Group name
     * @param topic         Topic
     * @param pullTime      Commit interval when no batch data is pulled
     * @param saveException Whether to enable exception-data saving; enable for strict business scenarios and provide a
     *   recovery mechanism for the saved exception data to avoid backlog
     */
    init {
        this.pullTime = pullTime
        this.batchProcessSize = batchProcessSize
        this.groupName = groupName
        this.topic = topic
        consumer = DefaultLitePullConsumer(groupName)
        consumer.namesrvAddr = rocketMqProperties.nameSrvAddr
        consumer.pullBatchSize = 32
        consumer.consumerPullTimeoutMillis = 5000
        consumer.isAutoCommit = false
        try {
            consumer.subscribe(topic, "*")
            consumer.start()
        } catch (e: MQClientException) {
            log.error(e, "MQ consumer start failed! topic=$topic")
        }
        // Graceful shutdown
        Runtime.getRuntime().addShutdownHook(Thread { this.destroy() })
    }

    /**
     * Starts consumption with the supplied business-handler callback.
     *
     * Creates a daemon thread that continuously pulls and batch-processes messages.
     *
     * Batch-processing triggers:
     * 1. Message count reaches batchProcessSize (default 1000)
     * 2. Time since the last commit exceeds pullTime (default 5000 ms)
     *
     * Workflow:
     * 1. Save the business-handler callback
     * 2. Set the running flag to true
     * 3. Create a daemon thread running the loop:
     *    - Pull messages from MQ (poll, non-blocking)
     *    - Append pulled messages to the batch buffer
     *    - Check whether batch-processing conditions are met (count or time)
     *    - If met, invoke toProcessBizData to handle the batch
     *    - Reset the buffer and the last-commit timestamp
     * 4. When the thread exits (isRunning=false), process any remaining batch data
     *
     * Batch strategy:
     * - Count threshold: process immediately for higher throughput
     * - Time threshold: process even if the count is small, ensuring timely processing
     * - Either condition triggers processing, balancing throughput and latency
     *
     * Notes:
     * - Uses a daemon thread so the JVM is not held open
     * - Remaining batch data is processed on thread exit to avoid message loss
     * - On batch-processing failure, the saveException setting decides whether to persist exception data
     *
     * @param bizBatchProcess Business handler that processes the batch message list
     */
    fun start(bizBatchProcess: (List<BatchConsumerItem<T>>) -> Unit) {
        this.bizBatchProcess = bizBatchProcess
        this.isRunning = true
        daemonThread = thread(
            name = "RocketMqBatchConsumer-$groupName",
            isDaemon = true
        ) {
            var lastCommitTime = System.currentTimeMillis()
            val batchData = mutableListOf<MessageExt?>()
            while (isRunning) {
                val poll = consumer.poll()
                val polledAny = poll != null && poll.isNotEmpty()
                if (polledAny) {
                    log.debug("Pulled this round: ${poll.size}")
                    batchData.addAll(poll)
                }
                val nowTime = System.currentTimeMillis()
                if (batchData.size >= batchProcessSize || nowTime - lastCommitTime > pullTime) {
                    log.info("Business data processed this round: ${batchData.size}")
                    toProcessBizData(batchData)
                    lastCommitTime = System.currentTimeMillis()
                    batchData.clear()
                } else if (!polledAny) {
                    // Idle backoff to avoid a CPU busy-loop. consumer.poll() has its own blocking timeout
                    // (consumerPullTimeoutMillis = 5s), but under low load the broker may return an empty
                    // collection immediately instead of blocking; this fallback yields the CPU.
                    try {
                        Thread.sleep(IDLE_POLL_SLEEP_MS)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
            }
            toProcessBizData(batchData)
        }
    }

    /**
     * Processes a batch of business data.
     *
     * Deserializes RocketMQ messages into business objects, invokes the business handler, and commits the
     * consumption offset based on the processing result.
     *
     * Workflow:
     * 1. Message deserialization:
     *    - Iterate over the batch message list
     *    - Read the byte array from the message body
     *    - Deserialize into a business object via ObjectInputStream
     *    - Extract the message properties
     *    - Wrap as a BatchConsumerItem object
     *
     * 2. Business processing:
     *    - Invoke the business handler with the batch
     *    - On success, commit the consumption offset (commit)
     *
     * 3. Exception handling:
     *    - On failure with saveException=true:
     *      * Log the error
     *      * Save exception data to the database (avoid MQ backlog)
     *      * Commit the offset (avoid repeated consumption)
     *    - On failure with saveException=false:
     *      * Log the error
     *      * Do not commit the offset (will be re-pulled next round)
     *
     * Serialization:
     * - Message bodies use JDK serialization (ObjectInputStream)
     * - The message body class must implement Serializable
     * - A deserialization failure throws RuntimeException and aborts processing
     *
     * Offset commit:
     * - Success: commit immediately to acknowledge processing
     * - Failure with exception persistence: also commit to avoid backlog from repeated consumption
     * - Failure without exception persistence: do not commit so the next pull retries
     *
     * Notes:
     * - A deserialization failure throws directly and does not trigger the exception-save path
     * - Business-handler exceptions follow saveException to decide whether to persist
     * - After persisting exception data the offset is still committed; a recovery mechanism is required for the saved data
     *
     * @param batchData The batch message list pulled from MQ
     */
    @Suppress("UNCHECKED_CAST")
    private fun toProcessBizData(batchData: MutableList<MessageExt?>) {
        if (batchData.isEmpty()) {
            return
        }
        val list = batchData.filterNotNull().map { s ->
            try {
                val data = decodeBody(s.body)
                BatchConsumerItem(data, s.properties)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        val processor = bizBatchProcess
        if (processor == null) {
            log.warn("Business consumer handler is null; skipping this batch. topic=$topic")
            return
        }
        try {
            processor.invoke(list)
            consumer.commit()
        } catch (e: Exception) {
            if (!saveException) {
                log.error(e, "Business consumption failed; will re-pull data...")
                return
            }
            log.error(e, "Business data consumption failed! ${e.message}")
            saveErrorData(batchData)
            // An exception occurred, but the log was saved, so acknowledge the consumption.
            consumer.commit()
        }
    }

    /**
     * Persists a failed batch to `sys_mq_fail_msg` to prevent MQ backlog.
     * Only actually persists when the RocketMQ configuration has `saveException = true`; otherwise just emits a
     * WARN noting that the feature is disabled.
     *
     * @param data Failed batch (typically a [MessageExt] list)
     * @author K
     * @since 1.0.0
     */
    private fun saveErrorData(data: Any) {
        if (!rocketMqProperties.saveException) {
            log.warn("Exception-consumption logging is not enabled...")
            return
        }
        log.warn("Saving exception-consumption log to avoid MQ backlog")
        val exceptionMsg = SysMqFailMsg().apply {
            topic = requireNotNull(this@RocketMqBatchConsumer.topic) { "topic is null" }
            msgBodyJson = JSONObject.toJSONString(data)
            createTime = LocalDateTime.now()
        }
        failMsgServiceProvider().save(exceptionMsg)
    }

    @Suppress("UNCHECKED_CAST")
    private fun decodeBody(body: ByteArray): T {
        return decodeJdkBody(body, rocketMqProperties.batchConsumerDeserializationFilter) as T
    }

    /**
     * Gracefully stops the consumer:
     * 1. Sets `isRunning = false` so the daemon thread's main loop exits;
     * 2. joins the daemon thread;
     * 3. calls `consumer.shutdown()` to release the RocketMQ client's netty connections and offset resources.
     *
     * Step 3 is the key fix versus the prior implementation, which only flipped the flag and waited for JVM exit
     * — otherwise the consumer's internal thread pool / netty selector keeps holding resources until the process
     * terminates. Registered with [Runtime.addShutdownHook] and invoked by the JVM.
     *
     * @author K
     * @since 1.0.0
     */
    fun destroy() {
        this.isRunning = false
        try {
            daemonThread?.join()
        } catch (e: InterruptedException) {
            log.error(e, "Failed to stop MQ consumption")
            Thread.currentThread().interrupt()
        }
        // Shut down the RocketMQ client to release netty / offset resources. The prior implementation only set
        // isRunning=false to exit the loop, leaving the consumer's connections and thread pool hanging until JVM exit.
        try {
            consumer.shutdown()
        } catch (e: Exception) {
            log.warn("RocketMQ consumer shutdown exception: {0}", e.message)
        }
    }


    /**
     * A batch consumption item: business message body + the attributes (attribute headers) of the original
     * [MessageExt]. Passing both to the business callback removes the need for the business side to look up the
     * original MessageExt.
     *
     * @param T Message body type
     * @property data Deserialized message body
     * @property properties Property map from the original MessageExt
     * @author K
 * @author AI: Codex
     * @since 1.0.0
     */
    class BatchConsumerItem<T>(var data: T, var properties: MutableMap<String?, String?>?)

    companion object {
        /** Logger */
        private val log = LogFactory.getLog(this::class)

        /** Daemon-thread sleep duration (ms) on idle polls to avoid 100% CPU spinning. */
        private const val IDLE_POLL_SLEEP_MS = 100L

        internal fun decodeJdkBody(body: ByteArray, deserializationFilter: String): Any? =
            ObjectInputStream(ByteArrayInputStream(body)).use { input ->
                val filter = deserializationFilter.takeIf { it.isNotBlank() }
                if (filter != null) {
                    input.setObjectInputFilter(ObjectInputFilter.Config.createFilter(filter))
                }
                input.readObject()
            }
    }

}
