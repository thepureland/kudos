package io.kudos.ability.distributed.stream.kafka.producer

import io.kudos.ability.distributed.stream.kafka.data.KafkaSimpleMsg

/**
 * Producer service interface for Kafka tests.
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
interface IKafkaProducerService {

    fun producer(msg: KafkaSimpleMsg)

}
