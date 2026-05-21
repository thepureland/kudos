package io.kudos.ability.distributed.stream.common.init.properties

/**
 * producer 侧本地 in-flight 限流配置。
 *
 * 绑定前缀：`kudos.ability.distributed.stream.producer-limit`
 */
open class StreamProducerLimitProperties {
    var enabled: Boolean = false
    var maxInFlight: Int = 1024
    var acquireTimeoutMillis: Long = 0
}
