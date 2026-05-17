package io.kudos.ability.cache.local.caffeine

import com.github.benmanes.caffeine.cache.Cache as NativeCaffeineCache
import org.springframework.cache.caffeine.CaffeineCache

/**
 * Spring [CaffeineCache] 的薄包装，在 [evict] / [clear] 后**同步**调用 Caffeine 的 `cleanUp()`，
 * 立即处理待生效的 invalidate/invalidateAll 队列。
 *
 * Caffeine 设计中 `invalidate(key)` 会立即 `asMap().remove(key)`（同步），但 `invalidateAll()`
 * 仅将清理标记入队列、依赖后续 maintenance 周期执行（异步）。这导致 `clear()` 后的紧随读取仍可能
 * 命中刚被标记失效的副本——尤其是 Spring `@Cacheable.get` 路径走 `nativeCache.asMap()` 时表现明显
 * （参见 0f645e8e 对 `ResourceIdsByTenantIdAndGroupCodeCacheTest.syncOnRoleResourceChange` 的诊断）。
 *
 * 显式 `cleanUp()` 强制 Caffeine 立即处理待 pending 维护工作，让 invalidate 立即生效。
 *
 * @author K
 * @author AI: Cursor
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

    override fun clear() {
        super.clear()
        native.cleanUp()
    }
}
