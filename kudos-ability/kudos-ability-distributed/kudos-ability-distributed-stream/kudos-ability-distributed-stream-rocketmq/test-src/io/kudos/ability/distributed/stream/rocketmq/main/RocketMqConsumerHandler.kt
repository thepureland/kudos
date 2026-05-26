package io.kudos.ability.distributed.stream.rocketmq.main

import com.alibaba.fastjson.JSONObject
import io.kudos.ability.distributed.stream.common.annotations.MqConsumer
import io.kudos.ability.distributed.stream.common.model.vo.StreamMessageVo
import io.kudos.ability.distributed.stream.rocketmq.data.RocketMqSimpleMsg
import io.kudos.base.logger.LogFactory
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import java.util.function.Consumer

/**
 * RocketMQ test consumer.
 *
 * @author shane
 * @author  K
 * @since 1.0.0
 */
@Component
open class RocketMqConsumerHandler {
    
    private val log = LogFactory.getLog(this::class)
    val defaultMsg: String = "Hello simple msg"
    var flag = false
    var errorFlag = false

    @MqConsumer(bindingName = "consumer-in-0")
    fun consumer(): Consumer<Message<StreamMessageVo<JSONObject>>?> {
        return Consumer { msg: Message<StreamMessageVo<JSONObject>>? ->
            // Get the message body
            val streamMsgVo = msg?.payload ?: return@Consumer
            val simpleMsgJson = streamMsgVo.data ?: return@Consumer
            val simpleMsg = simpleMsgJson.toJavaObject(RocketMqSimpleMsg::class.java)
            log.info("receive message: ${simpleMsg.msg}")
            // Logging
            if (this.defaultMsg == simpleMsg.msg) {
                flag = true
                log.info("is Test Message: true")
            }
            log.info("before error: $errorFlag")
            // Simulate a consumption exception
            if (errorFlag) {
                log.info("Simulated consumption exception: start up, true")
                throw RuntimeException("mock consumer exception")
            }
        }
    }

}
