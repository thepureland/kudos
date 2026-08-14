package io.kudos.ability.cache.local.caffeine

import com.github.benmanes.caffeine.cache.Cache as NativeCaffeineCache
import org.springframework.cache.caffeine.CaffeineCache

/**
 * Thin wrapper over Spring [CaffeineCache] that **synchronously** invokes Caffeine's `cleanUp()`
 * after [evict] / [clear], processing the pending invalidate/invalidateAll queue immediately.
 *
 * In Caffeine's design `invalidate(key)` performs `asMap().remove(key)` immediately (synchronous),
 * but `invalidateAll()` only enqueues a cleanup marker and relies on a later maintenance cycle
 * (asynchronous). This means a read shortly after `clear()` may still hit a copy that has just
 * been marked invalid — especially visible on Spring's `@Cacheable.get` path that goes through
 * `nativeCache.asMap()` (see 0f645e8e for the diagnosis of
 * `ResourceIdsByTenantIdAndGroupCodeCacheTest.syncOnRoleResourceChange`).
 *
 * An explicit `cleanUp()` forces Caffeine to process pending maintenance work immediately so
 * invalidates take effect right away.
 *
 * All four mutating entry points on Spring's `Cache` are covered. [evictIfPresent] and [invalidate] are
 * separate methods that reach the same asynchronous Caffeine paths as [evict] / [clear]; overriding only the
 * latter pair left the staleness window open for any caller using the boolean-returning variants — including
 * parts of Spring itself.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class DrainingCaffeineCache(
    name: String,
    cache: NativeCaffeineCache<Any, Any>,
) : CaffeineCache(name, cache) {

    private val native: NativeCaffeineCache<Any, Any>
        get() = nativeCache

    override fun evict(key: Any) {
        super.evict(key)
        native.cleanUp()
    }

    override fun evictIfPresent(key: Any): Boolean {
        val evicted = super.evictIfPresent(key)
        native.cleanUp()
        return evicted
    }

    override fun clear() {
        super.clear()
        native.cleanUp()
    }

    override fun invalidate(): Boolean {
        val hadEntries = super.invalidate()
        native.cleanUp()
        return hadEntries
    }
}
