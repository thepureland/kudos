package io.kudos.context.config

import java.util.*

/**
 * 一个记录顺序的properties扩展适配器
 * 得到所有properties的时候重写entrySet
 *
 * @author hanson
 * @since 1.0.0
 */
class OrderProperties : Properties {

    private val keyList: MutableList<Any> = ArrayList<Any>()

    constructor() : super()

    constructor(defaults: Properties?) : super(defaults)

    @Synchronized
    override fun put(key: Any?, value: Any?): Any? {
        if (!super.containsKey(key)) {
            keyList.add(key!!)
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
        for (k in keyList) {
            set.add(k.toString())
        }
        // include defaults if any
        if (defaults != null) {
            for (dk in defaults.stringPropertyNames()) {
                if (!set.contains(dk)) {
                    set.add(dk)
                }
            }
        }
        return set
    }

    override val entries: MutableSet<MutableMap.MutableEntry<Any, Any?>>
        get() {
        val entries = LinkedHashSet<MutableMap.MutableEntry<Any, Any?>>()
        for (key in keyList) {
            entries.add(AbstractMap.SimpleEntry<Any, Any?>(key, super.get(key)))
        }
        // include defaults entries
        if (defaults != null) {
            for (e in defaults.entries) {
                if (!containsKey(e.key)) {
                    entries.add(AbstractMap.SimpleEntry<Any, Any?>(e.key, e.value))
                }
            }
        }
        return entries
    }

}
