package io.kudos.ability.distributed.stream.kafka.data

import java.io.Serializable

/**
 * Simple message wrapper used in Kafka tests.
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
class KafkaSimpleMsg(var msg: String?) : Serializable
