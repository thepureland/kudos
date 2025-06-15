package io.kudos.ability.distributed.stream.kafka.main


/**
 * kafka测试服务接口
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
interface IKafkaMainService {

    fun sendAndReceiveMessage(): String?

    fun errorMessage(): String?

}
