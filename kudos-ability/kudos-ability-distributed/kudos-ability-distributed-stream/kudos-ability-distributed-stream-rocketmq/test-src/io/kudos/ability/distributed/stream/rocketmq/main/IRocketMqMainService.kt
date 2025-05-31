package io.kudos.ability.distributed.stream.rocketmq.main


/**
 * RocketMQ测试服务接口
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
interface IRocketMqMainService {

    fun sendAndReceiveMessage(): String?

    fun errorMessage(): String?

}
