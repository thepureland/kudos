package io.kudos.ability.distributed.stream.kafka.producer

import org.soul.ability.distributed.stream.kafka.data.KafkaSimpleMsg

/**
 * kafka 生產者服务
 *
 * @author shane
 * @since 5.1.1
 */
interface IKafkaProducerService {
    fun producer(msg: KafkaSimpleMsg?)
}
