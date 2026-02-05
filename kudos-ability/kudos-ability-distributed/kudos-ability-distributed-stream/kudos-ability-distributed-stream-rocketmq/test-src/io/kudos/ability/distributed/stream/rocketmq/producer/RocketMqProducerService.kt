package io.kudos.ability.distributed.stream.rocketmq.producer

import io.kudos.ability.distributed.stream.common.annotations.MqProducer
import io.kudos.ability.distributed.stream.rocketmq.data.RocketMqSimpleMsg
import io.kudos.base.logger.LogFactory
import org.springframework.stereotype.Service

/**
 * RocketMQ 生產者服务
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
@Service
open class RocketMqProducerService : IRocketMqProducerService {

    private val log = LogFactory.getLog(this)

//    @Bean
//    fun contextParam(): ContextParam {
//        val context = ContextParam()
//        // 这个数据类型目前是Integer类型, 像配置文件定义的db1,db2这种字符串会有问题
//        // jdbc模块也有相同问题, 后续建议统一调整为Object类型,并自动对String,Integer,Long等常用类型进行转换
//        context.username = "rocketMqTestUser"
//        CommonContext.set(context)
//        return context
//    }

    @MqProducer(topic = "ROCKETMQ_TEST_TOPIC", bindingName = "producer-out-0")
    override fun producer(msg: RocketMqSimpleMsg) {
        log.info("send new messages: ${msg.msg}")
    }

}
