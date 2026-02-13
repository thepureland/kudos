package io.kudos.ability.distributed.stream.rocketmq.init.properties

import io.kudos.context.kit.SpringKit
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * RocketMQ配置属性类
 * 封装RocketMQ相关的配置信息，包括NameServer地址和异常保存开关
 */
@Component
class RocketMqProperties {

    @Value($$"${spring.cloud.stream.rocketmq.binder.name-server}")
    var nameSrvAddr: String? = null

    @Value($$"${kudos.ability.distributed.stream.save-exception}")
    var saveException: Boolean = true

    companion object {
        val instance: RocketMqProperties
            get() = SpringKit.getBean<RocketMqProperties>()
    }

}
