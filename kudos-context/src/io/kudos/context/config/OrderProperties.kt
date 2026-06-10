package io.kudos.context.config

import java.util.AbstractMap
import java.util.Collections
import java.util.Enumeration
import java.util.LinkedHashSet
import java.util.Properties

/**
 * An order-preserving Properties extension adapter.
 * Overrides entrySet when retrieving all properties.
 *
 * @author hanson
 * @since 1.0.0
 */
class OrderProperties : Properties {

    private val keyList: MutableList<Any> = mutableListOf()

    constructor() : super()

    constructor(defaults: Properties?) : super(defaults)

    @Synchronized
    override fun put(key: Any?, value: Any?): Any? {
        if (!super.containsKey(key)) {
            key?.let { keyList.add(it) }
        }
        return super.put(key, value)
    }

    @Synchronized
    override fun remove(key: Any?): Any? {
        keyList.remove(key)
        return super.remove(key)
    }

    /**
     * Keeps [keyList] in sync. [java.util.Hashtable.putIfAbsent] inserts directly without delegating to [put],
     * so without this override a key inserted via putIfAbsent would be missing from the ordered views
     * ([keys] / [stringPropertyNames] / [entries]).
     */
    @Synchronized
    override fun putIfAbsent(key: Any?, value: Any?): Any? {
        val old = super.putIfAbsent(key, value)
        if (old == null) {
            key?.let { keyList.add(it) }
        }
        return old
    }

    /**
     * Keeps [keyList] in sync. [java.util.Hashtable.remove] (key, value) removes directly without delegating to
     * the single-arg [remove], so without this override the removed key would linger in the ordered views.
     */
    @Synchronized
    override fun remove(key: Any?, value: Any?): Boolean {
        val removed = super.remove(key, value)
        if (removed) {
            keyList.remove(key)
        }
        return removed
    }

    @Synchronized
    override fun clear() {
        keyList.clear()
        super.clear()
    }

    override fun propertyNames(): Enumeration<*> {
        // Return enumeration of stringPropertyNames, preserving order
        return Collections.enumeration<String?>(stringPropertyNames())
    }

    @Synchronized
    override fun keys(): Enumeration<Any?> {
        // Return enumeration of keys in insertion order
        return Collections.enumeration<Any?>(ArrayList(keyList))
    }

    /**
     * Keys in insertion order.
     *
     * Note: this must be a getter computed from the live [keyList]. The previous implementation initialized it
     * once at construction time (`= LinkedHashSet(keyList)`), when [keyList] was still empty — so the `keys`
     * property (Java `keySet()`) permanently returned an empty snapshot no matter what was put later.
     */
    override val keys: MutableSet<Any>
        @Synchronized get() = LinkedHashSet(keyList)

    override fun stringPropertyNames(): MutableSet<String?> {
        val set = LinkedHashSet<String?>()
        keyList.mapTo(set) { it.toString() }
        defaults?.stringPropertyNames()?.let(set::addAll)
        return set
    }

    override val entries: MutableSet<MutableMap.MutableEntry<Any, Any?>>
        get() {
            val entries = LinkedHashSet<MutableMap.MutableEntry<Any, Any?>>()
            keyList.mapTo(entries) { AbstractMap.SimpleEntry<Any, Any?>(it, super.get(it)) }
            defaults?.entries?.forEach { e ->
                if (!containsKey(e.key)) {
                    entries.add(AbstractMap.SimpleEntry<Any, Any?>(e.key, e.value))
                }
            }
            return entries
        }

}
