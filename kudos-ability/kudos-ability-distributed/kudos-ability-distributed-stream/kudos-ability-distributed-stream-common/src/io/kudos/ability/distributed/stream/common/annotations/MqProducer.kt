package io.kudos.ability.distributed.stream.common.annotations

/**
 * MQ producer annotation.
 *
 * @author paul
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class MqProducer(

    /**
     * Name of the bound stream producer configuration, e.g. producer-out-0.
     */
    val bindingName: String,

    /**
     * Optional; provided only to make lookup easier.
     */
    val topic: String = "",

    /**
     * Index of the method parameter to send as the payload; defaults to the first parameter.
     *
     * Multi-parameter producer methods should set this explicitly to avoid silently ignoring
     * business arguments.
     */
    val payloadParameterIndex: Int = 0,

    /**
     * Whether to cancel sending when the method returns Boolean false; default preserves historical behavior.
     */
    val cancelOnFalse: Boolean = true

)
