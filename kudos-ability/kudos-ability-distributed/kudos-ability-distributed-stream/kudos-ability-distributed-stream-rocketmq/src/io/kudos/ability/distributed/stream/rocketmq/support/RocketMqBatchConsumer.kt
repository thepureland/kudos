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
import java.io.ObjectInputStream
import java.time.LocalDateTime
import java.util.function.Consumer

/**
 * RocketMQ批量消息消费者
 * 
 * 支持批量拉取和消费RocketMQ消息，提供失败消息持久化和重试机制。
 * 
 * 核心特性：
 * 1. 批量拉取：使用DefaultLitePullConsumer批量拉取消息，提高消费效率
 * 2. 批量处理：支持配置批量处理大小（batchProcessSize），达到阈值或超时后统一处理
 * 3. 定时提交：即使没有拉取到新消息，也会按照pullTime间隔提交一次，确保消息及时处理
 * 4. 异常处理：支持开启异常数据保存功能，将消费失败的消息持久化到数据库，避免MQ堵塞
 * 5. 优雅关闭：注册JVM关闭钩子，确保应用关闭时正确停止消费者
 * 
 * 工作流程：
 * - 启动后创建守护线程持续拉取消息
 * - 当消息数量达到batchProcessSize或距离上次提交超过pullTime时，触发批量处理
 * - 批量处理时反序列化消息体，调用业务处理方法
 * - 处理成功后提交消费位点，失败时根据saveException配置决定是否保存异常数据
 * 
 * 注意事项：
 * - 消费位点提交采用手动提交模式（autoCommit=false），确保消息处理成功后才提交
 * - 如果开启异常保存，即使处理失败也会提交位点，避免重复消费导致堵塞
 * - 消息体使用JDK序列化，需要确保消息体类实现Serializable接口
 *
 * @param T 消息体类型
 */
class RocketMqBatchConsumer<T> @JvmOverloads constructor(
    groupName: String?,
    topic: String?,
    batchProcessSize: Int,
    pullTime: Long,
    saveException: Boolean = false
) {

    private var isRunning = false
    private var daemonThread: Thread? = null
    private val consumer: DefaultLitePullConsumer
    private var pullTime: Long = 5000
    private var batchProcessSize = 1000
    private var bizBachProcess: Consumer<MutableList<BatchConsumerItem<T?>?>?>? = null
    private val groupName: String?
    private val topic: String?
    private val saveException = false

    /**
     * @param groupName     分组名
     * @param topic         主题
     * @param pullTime      批量拉不到数据，多久提交一次
     * @param saveException 是否开启异常数据保存，业务严格下请开启它，并提供异常数据修复机制，避免堵塞
     */
    /**
     * @param groupName 分组名
     * @param topic     主题
     * @param pullTime  批量拉不到数据，多久提交一次
     */
    init {
        this.pullTime = pullTime
        this.batchProcessSize = batchProcessSize
        this.groupName = groupName
        this.topic = topic
        consumer = DefaultLitePullConsumer(groupName)
        consumer.namesrvAddr = RocketMqProperties.instance.nameSrvAddr
        consumer.pullBatchSize = 32
        consumer.consumerPullTimeoutMillis = 5000
        consumer.isAutoCommit = false
        try {
            consumer.subscribe(topic, "*")
            consumer.start()
        } catch (e: MQClientException) {
            log.error(e, "MQ消费启动异常！topic=$topic")
        }
        //优雅关机
        Runtime.getRuntime().addShutdownHook(Thread(Runnable { this.destroy() }))
    }

    /**
     * 启动消费，提交业务处理回调
     * 
     * 创建守护线程持续拉取和批量处理消息。
     * 
     * 批量处理触发条件：
     * 1. 消息数量达到batchProcessSize（默认1000条）
     * 2. 距离上次提交超过pullTime（默认5000毫秒）
     * 
     * 工作流程：
     * 1. 保存业务处理方法回调
     * 2. 设置运行标志为true
     * 3. 创建守护线程，执行以下循环：
     *    - 从MQ拉取消息（poll方法，非阻塞）
     *    - 将拉取到的消息添加到批量数据列表
     *    - 检查是否达到批量处理条件（数量或时间）
     *    - 如果达到条件，调用toProcessBizData处理批量数据
     *    - 重置批量数据列表和上次提交时间
     * 4. 线程退出时（isRunning=false），处理剩余的批量数据
     * 
     * 批量处理策略：
     * - 达到数量阈值：立即处理，提高吞吐量
     * - 达到时间阈值：即使数量不足也处理，确保消息及时处理
     * - 两者任一满足即触发处理，平衡吞吐量和延迟
     * 
     * 注意事项：
     * - 使用守护线程，不会阻止JVM关闭
     * - 线程退出时会处理剩余的批量数据，避免消息丢失
     * - 批量处理失败时，根据saveException配置决定是否保存异常数据
     * 
     * @param bizBachProcess 业务处理方法，接收批量消息列表进行处理
     */
    fun start(bizBachProcess: Consumer<MutableList<BatchConsumerItem<T?>?>?>) {
        this.bizBachProcess = bizBachProcess
        this.isRunning = true
        daemonThread = Thread {
            var lastCommitTime = System.currentTimeMillis()
            var batchData: MutableList<MessageExt?> = ArrayList<MessageExt?>()
            while (isRunning) {
                val poll = consumer.poll()
                if (poll != null && poll.size > 0) {
                    log.debug("本次拉到数据：" + poll.size)
                    batchData.addAll(poll)
                }
                val nowTime = System.currentTimeMillis()
                //超过1000条或者超过5秒
                if (batchData.size >= batchProcessSize || nowTime - lastCommitTime > pullTime) {
                    log.info("本次处理业务数据量：" + batchData.size)
                    toProcessBizData(batchData)
                    lastCommitTime = System.currentTimeMillis()
                    //清理内存数据
                    batchData = ArrayList<MessageExt?>()
                }
            }
            toProcessBizData(batchData)
        }
        daemonThread!!.setName("RocketMqBatchConsumer-$groupName")
        daemonThread!!.setDaemon(true)
        daemonThread!!.start()
    }

    /**
     * 处理批量业务数据
     * 
     * 将RocketMQ消息反序列化为业务对象，调用业务处理方法，并根据处理结果提交消费位点。
     * 
     * 工作流程：
     * 1. 消息反序列化：
     *    - 遍历批量消息列表
     *    - 从消息体中读取字节数组
     *    - 使用ObjectInputStream反序列化为业务对象
     *    - 提取消息属性（properties）
     *    - 封装为BatchConsumerItem对象
     * 
     * 2. 业务处理：
     *    - 调用业务处理方法处理批量数据
     *    - 如果处理成功，提交消费位点（commit）
     * 
     * 3. 异常处理：
     *    - 如果处理失败且saveException=true：
     *      * 记录错误日志
     *      * 保存异常数据到数据库（避免MQ堵塞）
     *      * 提交消费位点（避免重复消费）
     *    - 如果处理失败且saveException=false：
     *      * 记录错误日志
     *      * 不提交消费位点（下次会重新拉取）
     * 
     * 序列化说明：
     * - 消息体使用JDK序列化（ObjectInputStream）
     * - 需要确保消息体类实现Serializable接口
     * - 反序列化失败会抛出RuntimeException，中断处理
     * 
     * 消费位点提交：
     * - 处理成功：立即提交，确认消息已处理
     * - 处理失败且保存异常：也提交，避免重复消费导致堵塞
     * - 处理失败且不保存异常：不提交，下次重新拉取重试
     * 
     * 注意事项：
     * - 反序列化失败会直接抛出异常，不会触发异常保存逻辑
     * - 业务处理异常会根据saveException配置决定是否保存
     * - 保存异常数据后仍会提交位点，需要提供异常数据修复机制
     * 
     * @param batchData 批量消息列表，包含从MQ拉取的消息
     */
    private fun toProcessBizData(batchData: MutableList<MessageExt?>) {
        val list = batchData.stream().map { s: MessageExt? ->
            try {
                val properties = s!!.properties
                val ois = ObjectInputStream(ByteArrayInputStream(s.body))
                ois.use {
                    @Suppress("UNCHECKED_CAST")
                    val data = ois.readObject() as T?
                    return@map BatchConsumerItem<T?>(data, properties)
                }
            } catch (e: Exception) {
                //一般编码过程才会出现此问题，直接报错
                throw RuntimeException(e)
            }
        }.toList()
        try {
            bizBachProcess!!.accept(list)
            consumer.commit()
        } catch (e: Exception) {
            if (saveException) {
                log.error(e, "业务数据消费失败！" + e.message)
                saveErrorData(batchData)
                //出现异常，但是保存了日志，所以确认消费
                consumer.commit()
            } else {
                log.error(e, "业务消费失败，重新拉数据...")
            }
        }
    }

    private fun saveErrorData(data: Any) {
        if (!RocketMqProperties.instance.saveException) {
            log.warn("未开启异常消费记录功能...")
            return
        }
        log.warn("保存异常消费日志，避免mq堵塞")
        val exceptionMsg = SysMqFailMsg()
        exceptionMsg.topic = topic!!
        exceptionMsg.msgBodyJson = JSONObject.toJSONString(data)
        exceptionMsg.createTime = LocalDateTime.now()
        SpringKit.getBean(ISysMqFailMsgService::class).save(exceptionMsg)
    }

    fun destroy() {
        this.isRunning = false
        try {
            daemonThread!!.join()
        } catch (e: InterruptedException) {
            log.error("停止mq消费失败" + e.message)
        }
    }


    class BatchConsumerItem<T>(var data: T?, var properties: MutableMap<String?, String?>?)

    companion object {
        private val log = LogFactory.getLog(this)
    }

}
