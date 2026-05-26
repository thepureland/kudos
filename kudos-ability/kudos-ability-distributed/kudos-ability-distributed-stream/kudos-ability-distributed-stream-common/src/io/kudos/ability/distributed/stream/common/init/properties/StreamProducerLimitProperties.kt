package io.kudos.ability.distributed.stream.common.init.properties

/**
 * Producer-side local in-flight rate limit configuration.
 *
 * Binding prefix: `kudos.ability.distributed.stream.producer-limit`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class StreamProducerLimitProperties {
    var enabled: Boolean = false
    var maxInFlight: Int = 1024
    var acquireTimeoutMillis: Long = 0
}
