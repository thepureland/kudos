package io.kudos.ability.cache.common.support

import io.kudos.ability.cache.common.enums.CacheStrategy
import java.io.Serial
import java.io.Serializable

/**
 * Cache configuration.
 *
 * **Field semantics**:
 * - [strategyDictCode]: sourced from a DB dictionary code (e.g. sys_cache.strategyDictCode).
 * - [strategy]: sourced from code / yml (e.g. after parsing `kudos.cache.items`).
 *
 * Historically these two fields are written via two paths ("DB persistence" vs "code configuration"), and callers
 * have to write `config.strategy ?: config.strategyDictCode` everywhere, which is easy to miss. Use [resolvedStrategy]
 * uniformly for reads to centralize the fallback. Both raw fields are retained so DB deserialization and yml binding still work.
 *
 * @author K
 * @since 1.0.0
 */
class CacheConfig : Serializable {

    /**
     * Name.
     */
    var name: String? = ""

    /**
     * Cache strategy code (from DB dictionary code). Reads should prefer [resolvedStrategy]; only read this field directly when writing to DB / deserializing.
     */
    var strategyDictCode: String? = null

    /**
     * Whether to populate the cache on startup.
     */
    var writeOnBoot: Boolean? = null

    /**
     * Whether to write back to the cache in real time.
     */
    var writeInTime: Boolean? = null

    /**
     * Cache time-to-live (seconds).
     */
    var ttl: Int? = null

    /**
     * Whether enabled.
     */
    var active: Boolean? = true

    /**
     * Cache strategy code (from code / yml configuration). @Transient means it is not persisted to DB. Reads should prefer [resolvedStrategy].
     */
    @Transient
    var strategy: String? = null

    @Transient
    var ignoreVersion: Boolean? = null

    /**
     * Whether this is a Hash cache (a collection of id-keyed objects). When true, the entry participates in MixHashCacheManager initialization; strategy still comes from [resolvedStrategy].
     */
    var hash: Boolean = false

    /**
     * Derived strategy string: prefers [strategy] (code / yml), falling back to [strategyDictCode] (DB dictionary code).
     * Legacy code wrote `config.strategy ?: config.strategyDictCode` extensively; this property centralizes the fallback.
     */
    val resolvedStrategyCode: String?
        get() = strategy ?: strategyDictCode

    /**
     * Derived strongly-typed strategy. Parses [resolvedStrategyCode] into a [CacheStrategy]; returns null on parse failure or missing value.
     * Callers should typically use this instead of calling [CacheStrategy.valueOf] themselves.
     */
    val resolvedStrategy: CacheStrategy?
        get() = resolvedStrategyCode?.let {
            try {
                CacheStrategy.valueOf(it)
            } catch (_: IllegalArgumentException) {
                null
            }
        }

    /** Enabled flag: null is treated as true (legacy behavior: the initial default is true, but deserialization scenarios may yield null). */
    val isActive: Boolean get() = active != false

    /** Whether to write on boot: null is treated as false. Centralized to avoid scattered `== true` checks. */
    val isWriteOnBoot: Boolean get() = writeOnBoot == true

    /** Whether to write back in real time: null is treated as false. */
    val isWriteInTime: Boolean get() = writeInTime == true

    constructor()

    constructor(
        name: String?,
        strategyDictCode: String?,
        writeOnBoot: Boolean?,
        writeInTime: Boolean?,
        ttl: Int?,
        active: Boolean?
    ) {
        this.name = name
        this.strategyDictCode = strategyDictCode
        this.writeOnBoot = writeOnBoot
        this.writeInTime = writeInTime
        this.ttl = ttl
        this.active = active
    }

    companion object {
        @Serial
        private val serialVersionUID = -3447772148273247925L
    }

}
