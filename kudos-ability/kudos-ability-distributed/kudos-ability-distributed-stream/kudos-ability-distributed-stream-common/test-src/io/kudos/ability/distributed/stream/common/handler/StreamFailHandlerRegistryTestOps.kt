package io.kudos.ability.distributed.stream.common.handler

/**
 * Test-only access to the private registry inside [StreamFailHandlerItem], allowing tests to
 * snapshot / clear / restore the global handler map so they stay deterministic and
 * order-independent despite the registry being a process-global singleton.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal object StreamFailHandlerRegistryTestOps {

    @Suppress("UNCHECKED_CAST")
    private val registry: MutableMap<String, IStreamFailHandler> = run {
        val field = StreamFailHandlerItem::class.java.getDeclaredField("STREAM_HANDLER")
        field.isAccessible = true
        field.get(StreamFailHandlerItem) as MutableMap<String, IStreamFailHandler>
    }

    /** Returns a copy of the current registry content. */
    fun snapshot(): Map<String, IStreamFailHandler> = HashMap(registry)

    /** Replaces the registry content with the given snapshot. */
    fun restore(snapshot: Map<String, IStreamFailHandler>) {
        registry.clear()
        registry.putAll(snapshot)
    }

    /** Removes all registered handlers. */
    fun clear() = registry.clear()
}
