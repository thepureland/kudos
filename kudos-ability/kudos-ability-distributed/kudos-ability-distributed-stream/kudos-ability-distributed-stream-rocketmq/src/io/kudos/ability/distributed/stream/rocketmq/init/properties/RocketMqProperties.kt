package io.kudos.ability.distributed.stream.rocketmq.init.properties

import io.kudos.context.kit.SpringKit
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
     * Same syntax as [java.io.ObjectInputFilter.Config.createFilter]; empty preserves the historical unrestricted behavior.
     * Refer to the JDK ObjectInputFilter pattern for examples: allow Java primitive types and this project's message
     * types, then reject everything else.
     */
    @Value($$"${kudos.ability.distributed.stream.rocketmq.batch-consumer.deserialization-filter:}")
    var batchConsumerDeserializationFilter: String = ""

    companion object {
        val instance: RocketMqProperties
            get() = SpringKit.getBean<RocketMqProperties>()
    }

}
