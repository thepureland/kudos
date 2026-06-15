package io.kudos.ability.cache.local.caffeine

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheManager
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.support.CacheConfig
import org.springframework.boot.cache.autoconfigure.CacheProperties
import kotlin.test.*

/**
 * [CaffeineKeyValueCacheManager] 纯单元测试。
 *
 * 不依赖 Spring 容器，通过反射注入 [CacheProperties] 与 [CacheVersionConfig]，覆盖：
 * - createCache：spec / ttl / name 缺失的异常分支；ignoreVersion 为 null / false / true 时的
 *   版本前缀拼接；返回 [DrainingCaffeineCache] 包装
 * - ensureSizeBound：spec 已含 maximumSize（不追加）、空白 spec（整体替换）、非空白无上界（追加）
 * - evictByPattern：cache 不存在提前返回；'*' 通配符；键中含正则元字符时按字面量匹配；
 *   带版本前缀的 pattern 先剥再加不重复
 * - existsKey：cache 不存在返回 false；键存在 / 不存在
 *
 * @author K
 * @since 1.0.0
 */
internal class CaffeineKeyValueCacheManagerTest {

    private fun newManager(spec: String?, version: String = ""): CaffeineKeyValueCacheManager {
        val manager = CaffeineKeyValueCacheManager()
        val props = CacheProperties()
        props.caffeine.spec = spec
        val versionConfig = CacheVersionConfig().apply { cacheVersion = version }
        val abstractClass = AbstractKeyValueCacheManager::class.java
        abstractClass.getDeclaredField("properties").apply { isAccessible = true }.set(manager, props)
        abstractClass.getDeclaredField("versionConfig").apply { isAccessible = true }.set(manager, versionConfig)
        return manager
    }

    private fun cacheConfig(name: String? = "test", ttl: Int? = 3600, ignoreVersion: Boolean? = null) =
        CacheConfig().apply {
            this.name = name
            this.ttl = ttl
            this.ignoreVersion = ignoreVersion
        }

    // ---------- createCache 异常分支 ----------

    @Test
    fun createCache_requiresSpec() {
        val manager = newManager(spec = null)
        val ex = assertFailsWith<IllegalArgumentException> { manager.createCache(cacheConfig()) }
        assertEquals("caffeine spec is required", ex.message)
    }

    @Test
    fun createCache_requiresTtl() {
        val manager = newManager(spec = "maximumSize=100")
        val ex = assertFailsWith<IllegalArgumentException> { manager.createCache(cacheConfig(ttl = null)) }
        assertEquals("cache ttl is required", ex.message)
    }

    @Test
    fun createCache_requiresName() {
        val manager = newManager(spec = "maximumSize=100")
        val ex = assertFailsWith<IllegalArgumentException> { manager.createCache(cacheConfig(name = null)) }
        assertEquals("cache name is required", ex.message)
    }

    // ---------- createCache 版本前缀分支 ----------

    @Test
    fun createCache_appliesVersionPrefixWhenIgnoreVersionNullOrFalse() {
        val manager = newManager(spec = "maximumSize=100", version = "v9")
        val byNull = manager.createCache(cacheConfig(ignoreVersion = null))
        assertEquals("v9::test", byNull.name)
        val byFalse = manager.createCache(cacheConfig(ignoreVersion = false))
        assertEquals("v9::test", byFalse.name)
        assertIs<DrainingCaffeineCache>(byNull)
    }

    @Test
    fun createCache_skipsVersionPrefixWhenIgnoreVersionTrue() {
        val manager = newManager(spec = "maximumSize=100", version = "v9")
        val cache = manager.createCache(cacheConfig(ignoreVersion = true))
        assertEquals("test", cache.name)
    }

    @Test
    fun createCache_blankVersionLeavesNameUntouched() {
        val manager = newManager(spec = "maximumSize=100", version = "")
        val cache = manager.createCache(cacheConfig())
        assertEquals("test", cache.name)
    }

    // ---------- ensureSizeBound 分支（经 createCache 间接驱动） ----------

    @Test
    fun createCache_keepsSpecWithExplicitSizeBound() {
        val manager = newManager(spec = "maximumSize=2")
        val cache = manager.createCache(cacheConfig())
        // maximumSize=2 生效：放 3 个键，cleanUp 后最多剩 2 个
        cache.put("k1", "v1")
        cache.put("k2", "v2")
        cache.put("k3", "v3")
        cache.nativeCache.cleanUp()
        assertTrue(cache.nativeCache.asMap().size <= 2)
    }

    @Test
    fun createCache_appendsSizeBoundForBlankSpec() {
        val manager = newManager(spec = "")
        val cache = manager.createCache(cacheConfig())
        cache.put("k", "v")
        assertEquals("v", cache.get("k")?.get())
    }

    @Test
    fun createCache_appendsSizeBoundForSpecWithoutBound() {
        val manager = newManager(spec = "expireAfterAccess=300s")
        val cache = manager.createCache(cacheConfig())
        cache.put("k", "v")
        assertEquals("v", cache.get("k")?.get())
    }

    // ---------- evictByPattern ----------

    private fun managerWithCache(version: String = ""): CaffeineKeyValueCacheManager {
        val manager = newManager(spec = "maximumSize=100", version = version)
        manager.initCacheAfterSystemInit(mapOf("test" to cacheConfig()))
        return manager
    }

    @Test
    fun evictByPattern_returnsSilentlyWhenCacheAbsent() {
        val manager = managerWithCache()
        manager.evictByPattern("noSuchCache", "user:*")
    }

    @Test
    fun evictByPattern_evictsOnlyMatchingKeys() {
        val manager = managerWithCache()
        val cache = manager.getCache("test")!!
        cache.put("user:1", "a")
        cache.put("user:2", "b")
        cache.put("order:1", "c")
        manager.evictByPattern("test", "user:*")
        assertFalse(manager.existsKey("test", "user:1"))
        assertFalse(manager.existsKey("test", "user:2"))
        assertTrue(manager.existsKey("test", "order:1"))
    }

    @Test
    fun evictByPattern_quotesRegexMetacharactersLiterally() {
        val manager = managerWithCache()
        val cache = manager.getCache("test")!!
        cache.put("user.x:1", "a")
        cache.put("userQx:1", "b") // 若 '.' 未被 quote，会被误删
        cache.put("a[1](2)+:k", "c") // 非法正则片段，未 quote 会导致 PatternSyntaxException
        manager.evictByPattern("test", "user.x:*")
        assertFalse(manager.existsKey("test", "user.x:1"))
        assertTrue(manager.existsKey("test", "userQx:1"))
        manager.evictByPattern("test", "a[1](2)+:*")
        assertFalse(manager.existsKey("test", "a[1](2)+:k"))
    }

    @Test
    fun evictByPattern_stripsAndReappliesVersionPrefix() {
        val manager = managerWithCache(version = "v2")
        val cacheName = "v2::test"
        val cache = manager.getCache(cacheName)!!
        cache.put("v2::user:1", "a")
        cache.put("v2::order:1", "b")
        // pattern 已带版本前缀：先剥离再统一加回，不会出现 v2::v2:: 双前缀
        manager.evictByPattern(cacheName, "v2::user:*")
        assertFalse(manager.existsKey(cacheName, "v2::user:1"))
        assertTrue(manager.existsKey(cacheName, "v2::order:1"))
        // 逻辑 pattern（不带前缀）同样可用
        manager.evictByPattern(cacheName, "order:*")
        assertFalse(manager.existsKey(cacheName, "v2::order:1"))
    }

    @Test
    fun evictByPattern_evictTakesEffectImmediately() {
        // DrainingCaffeineCache 的同步 cleanUp：evict 后立即读不到
        val manager = managerWithCache()
        val cache = manager.getCache("test")!!
        cache.put("user:1", "a")
        manager.evictByPattern("test", "*")
        assertNull(cache.get("user:1"))
    }

    // ---------- existsKey ----------

    @Test
    fun existsKey_falseWhenCacheAbsent() {
        val manager = managerWithCache()
        assertFalse(manager.existsKey("noSuchCache", "k"))
    }

    @Test
    fun existsKey_distinguishesPresentAndAbsentKeys() {
        val manager = managerWithCache()
        val cache = manager.getCache("test")!!
        cache.put("k1", "v1")
        assertTrue(manager.existsKey("test", "k1"))
        assertFalse(manager.existsKey("test", "k2"))
    }
}
