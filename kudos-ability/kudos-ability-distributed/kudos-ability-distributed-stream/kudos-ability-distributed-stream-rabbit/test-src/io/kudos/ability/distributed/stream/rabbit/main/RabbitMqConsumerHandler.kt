package io.kudos.ability.distributed.stream.rabbit.main

import io.kudos.ability.distributed.stream.common.annotations.MqConsumer
import io.kudos.ability.distributed.stream.rabbit.data.RabbitMqSimpleMsg
import io.kudos.base.logger.LogFactory
import org.soul.ability.distributed.stream.common.model.model.StreamMessageVo
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import java.util.function.Consumer

/**
 * RabbitMq测试消费者
 *
 * @author shane
 * @author  K
 * @since 1.0.0
 */
@Component
open class RabbitMqConsumerHandler {
    
    private val log = LogFactory.getLog(this)
    val defaultMsg: String = "Hello simple msg"
    var flag = false
    var errorFlag = false

    @MqConsumer(bindingName = "consumer-in-0")
    fun consumer(): Consumer<Message<StreamMessageVo<RabbitMqSimpleMsg>>?> {
        return Consumer { msg: Message<StreamMessageVo<RabbitMqSimpleMsg>>? ->
            //获取消息体
            val streamMsgVo: StreamMessageVo<RabbitMqSimpleMsg> = msg!!.getPayload()
            val simpleMsg = streamMsgVo.getData()
            log.info("receive message: ${simpleMsg.msg}")
            //记录日志
            if (this.defaultMsg == simpleMsg.msg) {
                flag = true
                log.info("is Test Message: true")
            }
            log.info("before error: $errorFlag")
            //模拟消费异常
            if (errorFlag) {
                log.info("模拟消费异常: start up, true")
                throw RuntimeException("mock consumer exception")
            }
        }
    }

}
