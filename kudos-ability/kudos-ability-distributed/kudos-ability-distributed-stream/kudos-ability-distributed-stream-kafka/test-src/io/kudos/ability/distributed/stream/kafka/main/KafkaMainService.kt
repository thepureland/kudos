package io.kudos.ability.distributed.stream.kafka.main

import org.soul.ability.distributed.stream.common.iservice.IStreamExceptionService
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class KafkaMainService : IKafkaMainService {
    private val log: Log = LogFactory.getLog(KafkaMainService::class.java)
    private val topicName = "KAFKA_TEST_TOPIC"

    @Autowired
    private val producerClient: IKafkaProducerClient? = null

    @Autowired
    private val streamExceptionService: IStreamExceptionService? = null

    @Autowired
    private val consumerHandler: KafkaConsumerHandler? = null
    private val result = "SUCCESS"

    override fun sendAndReceiveMessage(): String {
        log.info("ready to send message")
        producerClient!!.send(consumerHandler!!.defaultMsg)
        var flag = true
        while (flag) {
            log.info("{0}", consumerHandler.flag)
            // 待消费者接收信息后修改 flag 为 false
            if (consumerHandler.flag) {
                flag = false
            }
        }
        log.info("收到 mq 消息")
        return result
    }

    override fun errorMessage(): String {
        consumerHandler!!.errorFlag = true
        val now = Date()
        producerClient!!.send("error test")
        var stop = true
        while (stop) {
            try {
                Thread.sleep(1000)
            } catch (e: Exception) {
                Thread.currentThread().interrupt()
            }
            val list = streamExceptionService!!.query(topicName, now)
            if (list.size > 0) {
                stop = false
                consumerHandler.errorFlag = false
            }
        }
        return result
    }
}
