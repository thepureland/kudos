package io.kudos.ability.distributed.stream.rocketmq.init.properties

import io.kudos.context.kit.SpringKit
import io.kudos.ability.distributed.stream.common.support.StreamMessageSerialization
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * RocketMQ configuration properties class.
 * Wraps RocketMQ-related configuration, including the NameServer address and the exception-save switch.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
class RocketMqProperties {

    @Value($$"${spring.cloud.stream.rocketmq.binder.name-server}")
    var nameSrvAddr: String? = null

    @Value($$"${kudos.ability.distributed.stream.save-exception}")
    var saveException: Boolean = true

    /**
     * JDK deserialization allowlist for RocketMqBatchConsumer.
     *
     * Optional additional restriction using [java.io.ObjectInputFilter.Config.createFilter] syntax.
     * The shared class allowlist and resource limits always apply, even when this string is empty.
     * This filter cannot expand the shared allowlist.
     */
    @Value($$"${kudos.ability.distributed.stream.rocketmq.batch-consumer.deserialization-filter:}")
    var batchConsumerDeserializationFilter: String = ""

    @Autowired(required = false)
    var messageSerialization: StreamMessageSerialization = StreamMessageSerialization()

    companion object {
        val instance: RocketMqProperties
            get() = SpringKit.getBean<RocketMqProperties>()
    }

}
