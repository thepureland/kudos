package io.kudos.ability.distributed.notify.common.support

import io.kudos.ability.distributed.notify.common.api.INotifyListener
import java.util.concurrent.ConcurrentHashMap


/**
 * Registry of [INotifyListener] beans—indexed by `(namespace, type)`.
 *
 * Concurrency contract: writes happen mainly during Spring's `BeanPostProcessor` phase (single-threaded);
 * reads happen during MQ dispatch (multi-threaded). Switching to [ConcurrentHashMap] turns that constraint
 * into explicit safety—even if callers register listeners dynamically at runtime, no
 * ConcurrentModificationException or visibility issue occurs. `getOrPut` on CHM is still not atomic, but the
 * "writes only at wiring time" constraint of this registry means the non-atomic getOrPut introduces no new risk.
 *
 * @author Younger
 * @author K
 * @since 1.0.0
 */
object NotifyListenerItem {

    /** Fallback value used when a listener does not specify a namespace. */
    const val DEFAULT_NAMESPACE: String = "default"

    /** Two-level namespace -> (key -> listener) index; both levels use [ConcurrentHashMap], the read path is fully lock-free. */
    private val notifyListenerMap = ConcurrentHashMap<String, ConcurrentHashMap<String, INotifyListener>>()

    /**
     * Registers a listener under the given namespace; blank namespaces fall back to [DEFAULT_NAMESPACE].
     *
     * @param namespace namespace (typically corresponds to a tenant / subsystem in business terms)
     * @param key the listener's business key
     * @param listener the listener instance
     * @author K
     * @since 1.0.0
     */
    fun put(namespace: String, key: String, listener: INotifyListener) {
        val actualNamespace = namespace.ifBlank { DEFAULT_NAMESPACE }
        notifyListenerMap.getOrPut(actualNamespace) { ConcurrentHashMap() }[key] = listener
    }

    /**
     * Shortcut overload that registers a listener under [DEFAULT_NAMESPACE].
     *
     * @param key the listener's business key
     * @param listener the listener instance
     * @author K
     * @since 1.0.0
     */
    fun put(key: String, listener: INotifyListener) {
        put(DEFAULT_NAMESPACE, key, listener)
    }

    /**
     * Returns the listener for the given namespace + key; returns null if the namespace exists but the key is missing.
     *
     * @param namespace the namespace; blank values fall back to [DEFAULT_NAMESPACE]
     * @param key the listener's business key
     * @return the listener instance, or null when not registered
     * @author K
     * @since 1.0.0
     */
    fun get(namespace: String, key: String): INotifyListener? {
        val actualNamespace = namespace.ifBlank { DEFAULT_NAMESPACE }
        return notifyListenerMap[actualNamespace]?.get(key)
    }

    /**
     * Shortcut overload that looks up a listener under [DEFAULT_NAMESPACE].
     *
     * @param key the listener's business key
     * @return the listener instance, or null when not registered
     * @author K
     * @since 1.0.0
     */
    fun get(key: String): INotifyListener? = get(DEFAULT_NAMESPACE, key)

}
