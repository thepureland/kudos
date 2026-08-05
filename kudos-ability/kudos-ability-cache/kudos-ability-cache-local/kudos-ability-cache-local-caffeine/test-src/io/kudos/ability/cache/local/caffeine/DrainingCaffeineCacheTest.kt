package io.kudos.ability.cache.local.caffeine

import com.github.benmanes.caffeine.cache.Caffeine
import kotlin.test.*

/**
 * [DrainingCaffeineCache] 纯单元测试。
 *
 * 覆盖 evict / clear 后同步 cleanUp 的语义：失效立即可见（包括 nativeCache.asMap 视图），
 * 以及名称、底层缓存的基本透传行为。
 *
 * @author K
 * @since 1.0.0
 */
internal class DrainingCaffeineCacheTest {

    private fun newCache(name: String = "draining") =
        DrainingCaffeineCache(name, Caffeine.newBuilder().maximumSize(100).build())

    @Test
    fun nameAndPutGet() {
        val cache = newCache("myCache")
        assertEquals("myCache", cache.name)
        cache.put("k", "v")
        assertEquals("v", cache.get("k")?.get())
    }

    @Test
    fun evict_removesEntryImmediately() {
        val cache = newCache()
        cache.put("k1", "v1")
        cache.put("k2", "v2")
        cache.evict("k1")
        assertNull(cache.get("k1"))
        assertFalse(cache.nativeCache.asMap().containsKey("k1"))
        assertEquals("v2", cache.get("k2")?.get())
    }

    @Test
    fun evict_absentKeyIsNoop() {
        val cache = newCache()
        cache.evict("absent")
        assertNull(cache.get("absent"))
    }

    @Test
    fun clear_removesAllEntriesImmediately() {
        val cache = newCache()
        repeat(10) { cache.put("k$it", "v$it") }
        cache.clear()
        assertTrue(cache.nativeCache.asMap().isEmpty(), "clear 后同步 cleanUp，asMap 视图应立即为空")
        assertNull(cache.get("k0"))
    }

    @Test
    fun clear_onEmptyCacheIsNoop() {
        val cache = newCache()
        cache.clear()
        assertTrue(cache.nativeCache.asMap().isEmpty())
    }
}
