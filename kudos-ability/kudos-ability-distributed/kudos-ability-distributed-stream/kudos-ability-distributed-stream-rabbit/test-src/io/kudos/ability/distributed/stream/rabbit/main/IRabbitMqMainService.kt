package io.kudos.ability.distributed.stream.rabbit.main


/**
 * RabbitMq测试服务接口
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
interface IRabbitMqMainService {

    fun sendAndReceiveMessage(): String?

    fun errorMessage(): String?

}
