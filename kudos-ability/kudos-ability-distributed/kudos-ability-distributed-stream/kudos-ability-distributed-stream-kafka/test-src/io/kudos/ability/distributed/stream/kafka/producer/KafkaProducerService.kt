package io.kudos.ability.distributed.stream.kafka.producer

import org.soul.ability.distributed.stream.kafka.data.KafkaSimpleMsg
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service

/**
 * kafka 生產者服务
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
@Service
class KafkaProducerService : IKafkaProducerService {
    private val log: Log = LogFactory.getLog(KafkaProducerService::class.java)

    @Bean
    fun contextParam(): ContextParam {
        val context: ContextParam = ContextParam()
        // 这个数据类型目前是Integer类型, 像配置文件定义的db1,db2这种字符串会有问题
        // jdbc模块也有相同问题, 后续建议统一调整为Object类型,并自动对String,Integer,Long等常用类型进行转换
        context.setUsername("kafkaTestUser")
        CommonContext.set(context)
        return context
    }

    @MqProducer(topic = "KAFKA_TEST_TOPIC", bindingName = "producer-out-0")
    fun producer(msg: KafkaSimpleMsg) {
        log.info("send new messages: {0}", msg.getMsg())
    }
}
