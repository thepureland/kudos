package io.kudos.ability.distributed.stream.rocketmq.main

import io.kudos.ability.distributed.stream.common.biz.ISysMqFailMsgService
import io.kudos.base.logger.LogFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.annotation.Resource

/**
 * RocketMQ测试服务
 * 
 * @author shane
 * @author K
 * @since 1.0.0
 */
@Service
class RocketMqMainService : IRocketMqMainService {
    
    private val log = LogFactory.getLog(this)
    
    private val topicName = "ROCKETMQ_TEST_TOPIC"

    @Resource
    private lateinit var producerClient: IRocketMqProducerClient

    @Resource
    private lateinit var streamExceptionBiz: ISysMqFailMsgService

    @Resource
    private lateinit var consumerHandler: RocketMqConsumerHandler
    
    private val result = "SUCCESS"

    override fun sendAndReceiveMessage(): String {
        log.info("ready to send message")
        producerClient.send(consumerHandler.defaultMsg)
        var flag = true
        while (flag) {
            Thread.sleep(1000)
            log.info("${consumerHandler.flag}")
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
        val now = LocalDateTime.now()
        producerClient.send("error test")
        var stop = true
        while (stop) {
            try {
                Thread.sleep(1000)
            } catch (_: Exception) {
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
