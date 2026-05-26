package io.kudos.ability.distributed.stream.kafka.main

import io.kudos.ability.distributed.stream.common.biz.ISysMqFailMsgService
import io.kudos.base.logger.LogFactory
import jakarta.annotation.Resource
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Kafka test service.
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
@Service
open class KafkaMainService : IKafkaMainService {
    
    private val log = LogFactory.getLog(this::class)
    
    private val topicName = "KAFKA_TEST_TOPIC"

    @Resource
    private lateinit var producerClient: IKafkaProducerClient

    @Resource
    private lateinit var streamExceptionBiz: ISysMqFailMsgService

    @Resource
    private lateinit var consumerHandler: KafkaConsumerHandler
    
    private val result = "SUCCESS"

    override fun sendAndReceiveMessage(): String {
        log.info("ready to send message")
        producerClient.send(consumerHandler.defaultMsg)
        var flag = true
        while (flag) {
            Thread.sleep(1000)
            log.info("{0}", consumerHandler.flag)
            // Set flag to false once the consumer has received the message
            if (consumerHandler.flag) {
                flag = false
            }
        }
        log.info("received mq message")
        return result
    }

    override fun errorMessage(): String {
        consumerHandler.errorFlag = true
        val now = LocalDateTime.now()
        producerClient.send("error test")
        var stop = true
        while (stop) {
            try {
                Thread.sleep(1000)
            } catch (e: Exception) {
                Thread.currentThread().interrupt()
            }
            val list = streamExceptionBiz.query(topicName, now)
            if (list.isNotEmpty()) {
                stop = false
                consumerHandler.errorFlag = false
            }
        }
        return result
    }
}
