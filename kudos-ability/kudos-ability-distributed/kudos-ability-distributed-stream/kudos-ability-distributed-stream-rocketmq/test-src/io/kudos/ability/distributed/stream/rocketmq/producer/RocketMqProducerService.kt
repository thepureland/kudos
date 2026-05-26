package io.kudos.ability.distributed.stream.rocketmq.producer

import io.kudos.ability.distributed.stream.common.annotations.MqProducer
import io.kudos.ability.distributed.stream.rocketmq.data.RocketMqSimpleMsg
import io.kudos.base.logger.LogFactory
import org.springframework.stereotype.Service

/**
 * RocketMQ producer service.
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
@Service
open class RocketMqProducerService : IRocketMqProducerService {

    private val log = LogFactory.getLog(this::class)

//    @Bean
//    fun contextParam(): ContextParam {
//        val context = ContextParam()
//        // This field type is currently Integer; strings like db1/db2 defined in config files cause issues.
//        // The jdbc module has the same problem; recommend unifying to Object type and auto-converting
//        // common types like String, Integer, Long, etc.
//        context.username = "rocketMqTestUser"
//        CommonContext.set(context)
//        return context
//    }

    @MqProducer(topic = "ROCKETMQ_TEST_TOPIC", bindingName = "producer-out-0")
    override fun producer(msg: RocketMqSimpleMsg) {
        log.info("send new messages: ${msg.msg}")
    }

}
