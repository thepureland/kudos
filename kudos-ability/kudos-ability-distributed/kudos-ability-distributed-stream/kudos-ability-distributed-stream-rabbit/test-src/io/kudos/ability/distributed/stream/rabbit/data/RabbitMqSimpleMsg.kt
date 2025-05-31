package io.kudos.ability.distributed.stream.rabbit.data

import java.io.Serializable

/**
 * RabbitMQ测试简单消息封装
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
class RabbitMqSimpleMsg(var msg: String?) : Serializable
