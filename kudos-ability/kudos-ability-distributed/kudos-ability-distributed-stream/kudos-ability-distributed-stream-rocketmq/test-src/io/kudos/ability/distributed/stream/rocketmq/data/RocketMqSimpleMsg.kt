package io.kudos.ability.distributed.stream.rocketmq.data

import java.io.Serializable

/**
 * RocketMQ测试简单消息封装
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
class RocketMqSimpleMsg(var msg: String?) : Serializable
