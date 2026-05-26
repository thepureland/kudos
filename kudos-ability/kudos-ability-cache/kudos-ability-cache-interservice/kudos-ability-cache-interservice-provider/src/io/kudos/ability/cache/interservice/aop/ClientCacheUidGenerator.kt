package io.kudos.ability.cache.interservice.aop

import io.kudos.ability.cache.interservice.common.ClientCacheItem
import org.springframework.util.ConcurrentReferenceHashMap

/**
 * Provider-side response UID generator.
 *
 * By default, the UID is recomputed each time from the response object's JSON. For hot endpoints
 * that repeatedly return the same immutable object instance, the uid cache can be enabled to reuse
 * the computed result; the cache uses weak-reference keys to avoid retaining response objects.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class ClientCacheUidGenerator(
    private val cacheEnabled: Boolean = false
) {

    private val uidCache = ConcurrentReferenceHashMap<Any, String>(
        256,
        ConcurrentReferenceHashMap.ReferenceType.WEAK
    )

    fun generate(result: Any): String {
        if (!cacheEnabled) {
            return ClientCacheItem.genUid(result)
        }
        return uidCache.computeIfAbsent(result) { ClientCacheItem.genUid(it) }
            ?: ClientCacheItem.genUid(result)
    }
}
