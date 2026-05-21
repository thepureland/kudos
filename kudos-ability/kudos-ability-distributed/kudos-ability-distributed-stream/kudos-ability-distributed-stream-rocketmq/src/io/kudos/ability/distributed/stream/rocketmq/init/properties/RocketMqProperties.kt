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

    /**
     * RocketMqBatchConsumer JDK 反序列化 allowlist。
     *
     * 语法同 [java.io.ObjectInputFilter.Config.createFilter]，为空时保持历史无限制行为。
     * 示例可参考 JDK ObjectInputFilter 模式，配置 Java 基础类型、本项目消息类型，最后拒绝其他类型。
     */
    @Value($$"${kudos.ability.distributed.stream.rocketmq.batch-consumer.deserialization-filter:}")
    var batchConsumerDeserializationFilter: String = ""

    companion object {
        val instance: RocketMqProperties
            get() = SpringKit.getBean<RocketMqProperties>()
    }

}
