package io.kudos.ability.cache.remote.redis

import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.remote.redis.support.RedisCacheKeyConversionService
import io.kudos.context.kit.SpringKit
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.cache.interceptor.SimpleKey
import org.springframework.context.ApplicationContext
import org.springframework.context.support.GenericApplicationContext
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.core.RedisTemplate
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [RedisKeyValueCacheManager].
 *
 * Covers, without a real Redis:
 * - createCache: version-prefixed naming, TTL applied / default TTL when unset, conversion-service
 *   propagation (single-param SimpleKey collapses to the bare string), and the null-name failure
 * - addCache / loadCaches / initCacheAfterSystemInit registration flow
 * - existsKey: false for a key whose underlying entry is absent
 * - evictByPattern: keys+delete when matched, skip delete when nothing matches, and the silent
 *   return when no RedisTemplate bean exists in the Spring context
 *
 * @author K
 * @since 1.0.0
 */
internal class RedisKeyValueCacheManagerTest {

    private lateinit var cacheWriter: RedisCacheWriter
    private lateinit var manager: RedisKeyValueCacheManager
    private lateinit var versionConfig: CacheVersionConfig

    private var previousContext: ApplicationContext? = null
    private val openedContexts = mutableListOf<GenericApplicationContext>()

    @BeforeTest
    fun setUp() {
        previousContext = runCatching { SpringKit.applicationContext }.getOrNull()
        cacheWriter = mock(RedisCacheWriter::class.java)
        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .entryTtl(Duration.ofSeconds(900))
            .withConversionService(RedisCacheKeyConversionService.create())
        manager = RedisKeyValueCacheManager(cacheWriter, defaultConfig)
        versionConfig = CacheVersionConfig() // cacheVersion defaults to "default"
        val field = RedisKeyValueCacheManager::class.java.getDeclaredField("versionConfig")
        field.isAccessible = true
        field.set(manager, versionConfig)
    }

    @AfterTest
    fun restoreContext() {
        SpringKit.applicationContext = previousContext
        openedContexts.forEach { it.close() }
        openedContexts.clear()
    }

    private fun installContext(vararg beans: Pair<String, Any>) {
        val ctx = GenericApplicationContext()
        beans.forEach { (name, bean) -> ctx.beanFactory.registerSingleton(name, bean) }
        ctx.refresh()
        openedContexts += ctx
        SpringKit.applicationContext = ctx
    }

    @Suppress("UNCHECKED_CAST")
    private fun mockTemplate(): RedisTemplate<String, Any> =
        mock(RedisTemplate::class.java) as RedisTemplate<String, Any>

    // ---------- createCache ----------

    @Test
    fun createCache_appliesVersionPrefixAndTtl() {
        val cache = manager.createCache(CacheConfig().apply { name = "user"; ttl = 300 })
        assertIs<ScanClearRedisCache>(cache)
        assertEquals(versionConfig.getFinalCacheName("user"), cache.name)
        assertEquals(
            Duration.ofSeconds(300),
            cache.cacheConfiguration.ttlFunction.getTimeToLive("k", "v"),
            "configured ttl (seconds) must be applied to the cache entry ttl"
        )
        assertFalse(cache.cacheConfiguration.allowCacheNullValues, "null-value caching must stay disabled")
    }

    @Test
    fun createCache_withoutTtl_usesDefaultConfig() {
        val cache = manager.createCache(CacheConfig().apply { name = "noTtl" })
        // entryTtl never applied -> defaultCacheConfig's zero ttl ("no expiry")
        assertEquals(Duration.ZERO, cache.cacheConfiguration.ttlFunction.getTimeToLive("k", "v"))
    }

    @Test
    fun createCache_propagatesSimpleKeyConversionFix() {
        val cache = manager.createCache(CacheConfig().apply { name = "conv" })
        val cs = cache.cacheConfiguration.conversionService
        assertEquals("k", cs.convert(SimpleKey("k"), String::class.java),
            "per-cache instances must inherit the SimpleKey->String fix from the outer configuration")
    }

    @Test
    fun createCache_nullName_throws() {
        val ex = assertFailsWith<IllegalArgumentException> {
            manager.createCache(CacheConfig().apply { name = null })
        }
        assertEquals("cache name required", ex.message)
    }

    // ---------- addCache / loadCaches / initCacheAfterSystemInit ----------

    @Test
    fun addCache_appendsToInternalList() {
        val cache = manager.createCache(CacheConfig().apply { name = "c1" })
        manager.addCache(cache)
        // loadCaches() (protected) returns the same list; exercised via initCacheAfterSystemInit -> afterPropertiesSet
        assertTrue(manager.caches.contains(cache))
        assertEquals(1, manager.caches.size)
    }

    @Test
    fun initCacheAfterSystemInit_registersAllConfiguredCaches() {
        val configs = mapOf(
            "a" to CacheConfig().apply { name = "a"; ttl = 60 },
            "b" to CacheConfig().apply { name = "b" }
        )
        manager.initCacheAfterSystemInit(configs)
        assertEquals(2, manager.caches.size)
        val realNameA = versionConfig.getFinalCacheName("a")
        val realNameB = versionConfig.getFinalCacheName("b")
        assertTrue(manager.cacheNames.containsAll(listOf(realNameA, realNameB)))
        assertNotNull(manager.getCache(realNameA))
        assertNotNull(manager.getCache(realNameB))
    }

    // ---------- existsKey ----------

    @Test
    fun existsKey_returnsFalseWhenEntryAbsent() {
        manager.initCacheAfterSystemInit(mapOf("e" to CacheConfig().apply { name = "e" }))
        // mocked cacheWriter returns null bytes for any get -> Cache.get(key) yields null -> not exists
        assertFalse(manager.existsKey(versionConfig.getFinalCacheName("e"), "missing"))
    }

    // ---------- evictByPattern ----------

    @Test
    fun evictByPattern_deletesMatchedKeys() {
        val template = mockTemplate()
        // keyPrefix for "user" is "user::"; version prefix then applied
        val fullPattern = versionConfig.getFinalCacheName("user::") + "*"
        val matched = setOf("default::user::1", "default::user::2")
        `when`(template.keys(fullPattern)).thenReturn(matched)
        installContext("stringRedisTemplate" to template)

        manager.evictByPattern("user", "*")

        verify(template).delete(matched)
    }

    @Test
    fun evictByPattern_noMatch_skipsDelete() {
        val template = mockTemplate()
        `when`(template.keys(ArgumentMatchers.anyString())).thenReturn(emptySet())
        installContext("stringRedisTemplate" to template)

        manager.evictByPattern("user", "nothing:*")

        verify(template, never()).delete(ArgumentMatchers.anyCollection())
    }

    @Test
    fun evictByPattern_withoutTemplateBean_returnsSilently() {
        installContext() // no RedisTemplate beans
        manager.evictByPattern("user", "*") // must not throw
    }
}
