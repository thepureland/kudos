package io.kudos.test.container

import org.springframework.test.context.DynamicPropertyRegistry
import java.util.function.Supplier

/**
 * Lightweight fake [DynamicPropertyRegistry] for unit tests.
 *
 * Collects every `add(name, supplier)` call into [entries] without any Spring context, so the
 * container classes' `registerProperties(...)` logic can be exercised and asserted offline (no Docker).
 *
 * @author K
 * @since 1.0.0
 */
internal class FakeDynamicPropertyRegistry : DynamicPropertyRegistry {

    /** name -> supplier, preserving insertion order. */
    val entries = LinkedHashMap<String, Supplier<Any>>()

    override fun add(name: String, valueSupplier: Supplier<Any>) {
        entries[name] = valueSupplier
    }

    /** Eagerly evaluates the supplier registered under [name], or null if absent. */
    fun value(name: String): Any? = entries[name]?.get()

    /** Whether a property was registered under [name]. */
    fun has(name: String): Boolean = entries.containsKey(name)
}
