package io.kudos.ability.distributed.stream.rabbit.main

import io.kudos.ability.distributed.stream.common.annotations.MqConsumer
import io.kudos.ability.distributed.stream.common.model.vo.StreamMessageVo
import io.kudos.ability.distributed.stream.rabbit.data.RabbitMqSimpleMsg
import io.kudos.base.logger.LogFactory
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import java.util.function.Consumer

/**
 * RabbitMQ test consumer.
 *
 * @author shane
 * @author  K
 * @since 1.0.0
 */
@Component
open class RabbitMqConsumerHandler {
    
    private val log = LogFactory.getLog(this::class)
    val defaultMsg: String = "Hello simple msg"
    var flag = false
    var errorFlag = false

    @MqConsumer(bindingName = "consumer-in-0")
    fun consumer(): Consumer<Message<StreamMessageVo<RabbitMqSimpleMsg>>?> {
        return Consumer { msg: Message<StreamMessageVo<RabbitMqSimpleMsg>>? ->
            // Extract payload
            val streamMsgVo = msg?.payload ?: return@Consumer
            val simpleMsg = streamMsgVo.data ?: return@Consumer
            log.info("receive message: ${simpleMsg.msg}")
            // Log
            if (this.defaultMsg == simpleMsg.msg) {
                flag = true
                log.info("is Test Message: true")
            }
            log.info("before error: $errorFlag")
            // Simulate a consumer exception
            if (errorFlag) {
                log.info("simulate consumer exception: start up, true")
                throw RuntimeException("mock consumer exception")
            }
        }
    }

}
