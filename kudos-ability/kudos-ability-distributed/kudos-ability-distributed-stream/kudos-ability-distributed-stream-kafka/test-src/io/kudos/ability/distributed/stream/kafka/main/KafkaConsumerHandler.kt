package io.kudos.ability.distributed.stream.kafka.main

import org.soul.ability.distributed.stream.kafka.data.KafkaSimpleMsg
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component
class KafkaConsumerHandler {
    private val log: Log = LogFactory.getLog(KafkaConsumerHandler::class.java)
    val defaultMsg: String = "Hello simple msg"

    @MqConsumer(bindingName = "consumer-in-0")
    fun consumer(): Consumer<Message<StreamMessageVo<KafkaSimpleMsg>>?> {
        return Consumer { msg: Message<StreamMessageVo<KafkaSimpleMsg>>? ->
            //获取消息体
            val streamMsgVo: StreamMessageVo<KafkaSimpleMsg> = msg!!.getPayload()
            val simpleMsg: KafkaSimpleMsg = streamMsgVo.getData()
            log.info("receive message: {0}", simpleMsg.getMsg())
            //记录日志
            if (this.defaultMsg == simpleMsg.getMsg()) {
                Companion.flag = true
                log.info("is Test Message: {0}", Companion.flag)
            }
            log.info("before error: {0}", Companion.errorFlag)
            //模拟消费异常
            if (Companion.errorFlag) {
                log.info("模拟消费异常: {0}, {1}", "start up", Companion.errorFlag)
                throw RuntimeException("mock consumer exception")
            }
        }
    }

    var flag: Boolean
        get() = Companion.flag
        set(flag) {
            Companion.flag = flag
        }

    var errorFlag: Boolean
        get() = Companion.errorFlag
        set(errorFlag) {
            Companion.errorFlag = errorFlag
        }

    companion object {
        private var flag = false
        private var errorFlag = false
    }
}
