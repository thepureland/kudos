package io.kudos.ability.distributed.stream.kafka.producer

import io.kudos.ability.distributed.stream.common.annotations.MqProducer
import io.kudos.ability.distributed.stream.kafka.data.KafkaSimpleMsg
import io.kudos.base.logger.LogFactory
import org.springframework.stereotype.Service

/**
 * Kafka producer service.
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
@Service
open class KafkaProducerService : IKafkaProducerService {

    private val log = LogFactory.getLog(this::class)

//    @Bean
//    fun contextParam(): KudosContext {
//        val context = KudosContext()
//        // The data type is currently Integer; strings like db1/db2 defined in config files cause problems.
//        // The jdbc module has the same issue; consider unifying it to Object and auto-converting common
//        // types (String, Integer, Long, etc.) later.
//        context.username = "kafkaTestUser"
//        CommonContext.set(context)
//        return context
//    }

    @MqProducer(topic = "KAFKA_TEST_TOPIC", bindingName = "producer-out-0")
    override fun producer(msg: KafkaSimpleMsg) {
        log.info("send new messages: ${msg.msg}")
    }

}
