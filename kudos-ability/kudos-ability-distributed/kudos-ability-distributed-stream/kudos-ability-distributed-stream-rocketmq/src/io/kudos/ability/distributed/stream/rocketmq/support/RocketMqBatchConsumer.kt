package io.kudos.ability.distributed.stream.rocketmq.support

import com.alibaba.fastjson2.JSONObject
import io.kudos.ability.distributed.stream.common.biz.IStreamExceptionBiz
import io.kudos.ability.distributed.stream.common.model.po.StreamExceptionMsg
import io.kudos.ability.distributed.stream.rocketmq.init.properties.RocketMqProperties
import io.kudos.base.logger.LogFactory
import io.kudos.context.kit.SpringKit
import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer
import org.apache.rocketmq.client.exception.MQClientException
import org.apache.rocketmq.common.message.MessageExt
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.time.LocalDateTime
import java.util.*
import java.util.function.Consumer

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
     * 启动消费，提交业务处理回调，报错本批消费失败。处理失败后还会继续重新处理
     *
     * @param bizBachProcess 业务处理方法
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

    private fun toProcessBizData(batchData: MutableList<MessageExt?>) {
        val list = batchData.stream().map { s: MessageExt? ->
            try {
                val properties = s!!.properties
                val ois = ObjectInputStream(ByteArrayInputStream(s.getBody()))
                val data = ois.readObject() as T?
                return@map BatchConsumerItem<T?>(data, properties)
            } catch (e: Exception) {
                //一般编码过程才会出现此问题，直接报错
                throw RuntimeException(e)
            }
        }.toList()
        try {
            bizBachProcess!!.accept(list)
            consumer.commitSync()
        } catch (e: Exception) {
            if (saveException) {
                log.error(e, "业务数据消费失败！" + e.message)
                saveErrorData(batchData)
                //出现异常，但是保存了日志，所以确认消费
                consumer.commitSync()
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
        val exceptionMsg = StreamExceptionMsg()
        exceptionMsg.topic = topic!!
        exceptionMsg.msgBodyJson = JSONObject.toJSONString(data)
        exceptionMsg.createTime = LocalDateTime.now()
        SpringKit.getBean(IStreamExceptionBiz::class).save(exceptionMsg)
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
