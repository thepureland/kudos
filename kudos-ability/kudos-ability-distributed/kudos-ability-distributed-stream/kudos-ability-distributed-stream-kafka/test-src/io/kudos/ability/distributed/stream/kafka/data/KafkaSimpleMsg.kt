package io.kudos.ability.distributed.stream.kafka.data

import java.io.Serializable

/**
 * kafka测试简单消息封装
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
class KafkaSimpleMsg(var msg: String?) : Serializable
