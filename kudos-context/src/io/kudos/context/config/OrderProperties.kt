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

    override val keys: MutableSet<Any> = LinkedHashSet<Any>(keyList)

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
