package io.kudos.ability.distributed.stream.common.handler

import java.util.concurrent.ConcurrentHashMap

/**
 * Stream message failure handler registry.
 * Manages and looks up failure handlers by binding name.
 */
object StreamFailHandlerItem {
    /** bindName -> IStreamFailHandler registry; written during Spring configuration, read during message handling. */
    private val STREAM_HANDLER = ConcurrentHashMap<String, IStreamFailHandler>()

    /**
     * Register a failure handler for the given bindName.
     *
     * @param bindName Spring Cloud Stream binding name (e.g. `outboundOrder-out-0`)
     * @param listener failure handler instance
     * @author K
     * @since 1.0.0
     */
    fun put(bindName: String, listener: IStreamFailHandler) {
        STREAM_HANDLER[bindName] = listener
    }

    /**
     * Check whether a failure handler is registered for the given bindName.
     *
     * @param bindName binding name
     * @return true if registered
     * @author K
     * @since 1.0.0
     */
    fun hasFailedHandler(bindName: String?): Boolean = STREAM_HANDLER.containsKey(bindName)

    /**
     * Get the failure handler for bindName; falls back to the default implementation under
     * [IStreamFailHandler.DEFAULT_BIND_NAME] when none is registered. Returns null when neither
     * exists, leaving the caller to decide how to degrade.
     *
     * @param bindName binding name
     * @return registered handler or default implementation, or null if neither exists
     * @author K
     * @since 1.0.0
     */
    fun get(bindName: String): IStreamFailHandler? =
        // No specific handler -> use the default implementation
        STREAM_HANDLER[bindName] ?: STREAM_HANDLER[IStreamFailHandler.DEFAULT_BIND_NAME]
}
