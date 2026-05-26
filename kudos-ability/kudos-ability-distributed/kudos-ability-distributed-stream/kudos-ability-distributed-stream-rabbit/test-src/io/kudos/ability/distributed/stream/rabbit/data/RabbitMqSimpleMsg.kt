package io.kudos.ability.distributed.stream.rabbit.data

import java.io.Serializable

/**
 * RabbitMQ test simple message wrapper.
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
class RabbitMqSimpleMsg(var msg: String?) : Serializable
