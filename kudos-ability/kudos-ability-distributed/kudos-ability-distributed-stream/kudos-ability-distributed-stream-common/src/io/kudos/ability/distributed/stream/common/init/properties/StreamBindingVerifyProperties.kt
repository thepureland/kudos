package io.kudos.ability.distributed.stream.common.init.properties

/**
 * Stream binding self-check configuration.
 *
 * Binding prefix: `kudos.ability.distributed.stream.binding-verify`
 */
open class StreamBindingVerifyProperties {

    /**
     * Whether the producer binding self-check is enabled.
     */
    var enabled: Boolean = false

    /**
     * Whether startup should fail (fail-fast) when required bindings are missing.
     */
    var failOnMissing: Boolean = false

    /**
     * List of required producer binding names.
     */
    var requiredProducerBindings: List<String> = emptyList()
}

