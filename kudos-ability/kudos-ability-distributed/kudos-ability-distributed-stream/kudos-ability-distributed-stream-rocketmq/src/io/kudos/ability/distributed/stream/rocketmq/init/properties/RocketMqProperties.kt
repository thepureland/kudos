package io.kudos.ability.distributed.stream.rocketmq.init.properties

import io.kudos.context.kit.SpringKit
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class RocketMqProperties {

    @Value("\${spring.cloud.stream.rocketmq.binder.name-server}")
    var nameSrvAddr: String? = null

    @Value("\${kudos.ability.distributed.stream.save-exception}")
    var saveException: Boolean = true

    companion object {
        val instance: RocketMqProperties
            get() = SpringKit.getBean(RocketMqProperties::class)
    }

}
