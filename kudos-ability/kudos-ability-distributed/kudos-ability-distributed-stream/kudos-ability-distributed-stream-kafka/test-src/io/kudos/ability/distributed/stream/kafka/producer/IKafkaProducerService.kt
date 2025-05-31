package io.kudos.ability.distributed.stream.kafka.producer

import io.kudos.ability.distributed.stream.kafka.data.KafkaSimpleMsg

/**
 * kafka测试生產者服务接口
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
interface IKafkaProducerService {

    fun producer(msg: KafkaSimpleMsg)

}
