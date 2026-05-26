package io.kudos.ability.cache.common.support

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Process-level registry for cache clean listeners.
 *
 * Historical issue: the old implementation added `@Synchronized` to [register] but not to [getCleanListener];
 * the underlying structures were `mutableMapOf` / `ArrayList`, so readers could observe a growing list or dirty state.
 * Switched to [ConcurrentHashMap] + [CopyOnWriteArrayList]:
 * - writers no longer need coarse-grained synchronization;
 * - readers receive a snapshot that is unaffected by structural modifications, making iteration safe;
 * - in read-heavy / write-rare scenarios (typical: registered once at startup, read-only thereafter), the COW write cost is acceptable.
 */
object CacheCleanRegister {

    private val registerMap = ConcurrentHashMap<String, CopyOnWriteArrayList<ICacheCleanListener>>()

    fun register(cacheKey: String, cleanListener: ICacheCleanListener) {
        registerMap.computeIfAbsent(cacheKey) { CopyOnWriteArrayList() }.add(cleanListener)
    }

    fun getCleanListener(cacheKey: String): List<ICacheCleanListener>? = registerMap[cacheKey]
}
