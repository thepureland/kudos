package io.kudos.ability.distributed.stream.kafka.main

import io.kudos.base.logger.LogFactory
import org.soul.ability.distributed.stream.common.iservice.IStreamExceptionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

/**
 * kafka测试服务
 * 
 * @author shane
 * @author K
 * @since 1.0.0
 */
@Service
open class KafkaMainService : IKafkaMainService {
    
    private val log = LogFactory.getLog(this)
    
    private val topicName = "KAFKA_TEST_TOPIC"

    @Autowired
    private lateinit var producerClient: IKafkaProducerClient

    @Autowired
    private lateinit var streamExceptionService: IStreamExceptionService

    @Autowired
    private lateinit var consumerHandler: KafkaConsumerHandler
    
    private val result = "SUCCESS"

    override fun sendAndReceiveMessage(): String {
        log.info("ready to send message")
        producerClient.send(consumerHandler.defaultMsg)
        var flag = true
        while (flag) {
            Thread.sleep(1000)
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
        consumerHandler.errorFlag = true
        val now = Date()
        producerClient.send("error test")
        var stop = true
        while (stop) {
            try {
                Thread.sleep(1000)
            } catch (e: Exception) {
                Thread.currentThread().interrupt()
            }
            val list = streamExceptionService.query(topicName, now)
            if (list.isNotEmpty()) {
                stop = false
                consumerHandler.errorFlag = false
            }
        }
        return result
    }
}
