package io.kudos.ability.distributed.stream.rabbit.producer

import io.kudos.ability.distributed.stream.common.annotations.MqProducer
import io.kudos.ability.distributed.stream.rabbit.data.RabbitMqSimpleMsg
import io.kudos.base.logger.LogFactory
import org.springframework.stereotype.Service

/**
 * RabbitMQ producer service.
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
@Service
open class RabbitMqProducerService : IRabbitMqProducerService {

    private val log = LogFactory.getLog(this::class)

//    @Bean
//    fun contextParam(): ContextParam {
//        val context = ContextParam()
//        // This field type is currently Integer; strings like db1/db2 defined in config files cause issues.
//        // The jdbc module has the same problem; recommend unifying to Object type and auto-converting
//        // common types like String, Integer, Long, etc.
//        context.username = "rabbitTestUser"
//        CommonContext.set(context)
//        return context
//    }

    @MqProducer(topic = "RABBIT_TEST_TOPIC", bindingName = "producer-out-0")
    override fun producer(msg: RabbitMqSimpleMsg) {
        log.info("send new messages: ${msg.msg}")
    }

}
