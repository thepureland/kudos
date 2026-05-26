package io.kudos.ability.distributed.stream.rocketmq.data

import java.io.Serializable

/**
 * Simple message wrapper for RocketMQ tests.
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
class RocketMqSimpleMsg(var msg: String?) : Serializable
