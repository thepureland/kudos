package io.kudos.ability.cache.common.notify

import io.kudos.ability.cache.common.core.hash.IHashCache
import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
import io.kudos.ability.cache.common.core.keyvalue.MixCache
import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.context.kit.SpringKit
import org.springframework.cache.Cache
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [CacheNotifyListener].
 *
 * KeyValueCacheKit / HashCacheKit are stubbed via overrideForTesting with in-memory doubles.
 *
 * Coverage:
 *  - notifyType() returns the CACHE_OPERATOR constant.
 *  - notifyProcess: inactive (both kv & hash) -> early return, no cleanup.
 *  - notifyProcess: SINGLE_LOCAL + TYPE_CLEAR -> kv & hash cleared.
 *  - notifyProcess: SINGLE_LOCAL + TYPE_EVICT (non-null key) -> kv & hash evicted.
 *  - notifyProcess: non-SINGLE_LOCAL strategy -> no cleanup (handled elsewhere).
 *  - doClear (via notify): TYPE_EVICT with null key -> no eviction.
 *
 * @author K
 * @since 1.0.0
 */
internal class CacheNotifyListenerTest {

    private val listener = CacheNotifyListener()
    private lateinit var kvMgr: TestMixCacheManager
    private lateinit var hashMgr: MixHashCacheManager
    private lateinit var hashFake: RecordingHashCache
    private lateinit var provider: TestProvider
    private var ctx: AnnotationConfigApplicationContext? = null

    @BeforeTest
    fun setup() {
        kvMgr = TestMixCacheManager()
        hashMgr = MixHashCacheManager()
        hashFake = RecordingHashCache()
        provider = TestProvider()
        registerHashCache(hashMgr, "C", hashFake)
        KeyValueCacheKit.overrideForTesting(kvMgr, provider)
        HashCacheKit.overrideForTesting(hashMgr, provider)
        // HashCacheKit.doEvict scans Spring beans for handlers; an empty context yields no handlers (no throw).
        ctx = AnnotationConfigApplicationContext(EmptyConfig::class.java)
        SpringKit.applicationContext = ctx
    }

    @AfterTest
    fun teardown() {
        KeyValueCacheKit.resetForTesting()
        HashCacheKit.resetForTesting()
        ctx?.close()
        SpringKit.applicationContext = null
    }

    private fun notify(vo: CacheOperatorVo) {
        val msg = NotifyMessageVo<CacheOperatorVo>(CacheNotifyListener.CACHE_OPERATOR, vo)
        listener.notifyProcess(msg)
    }

    @Test
    fun notifyType_isCacheOperator() {
        assertEquals(CacheNotifyListener.CACHE_OPERATOR, listener.notifyType())
        assertEquals("_CACHE_OPERATOR", CacheNotifyListener.CACHE_OPERATOR)
    }

    @Test
    fun notifyProcess_bothInactive_noCleanup() {
        // No config registered -> kv inactive (isActive=false) and hash inactive -> early return.
        kvMgr.underlying("C").put("k", "v")
        notify(CacheOperatorVo(CacheOperatorVo.TYPE_CLEAR, "C", null))
        assertEquals("v", kvMgr.underlying("C").get("k")?.get())
        assertTrue(hashFake.clears.isEmpty())
    }

    @Test
    fun notifyProcess_singleLocal_clear_clearsKvAndHash() {
        provider.register("C", true, CacheStrategy.SINGLE_LOCAL, hash = true)
        kvMgr.underlying("C").put("k", "v")
        notify(CacheOperatorVo(CacheOperatorVo.TYPE_CLEAR, "C", null))
        assertNull(kvMgr.underlying("C").get("k"), "kv cache should be cleared")
        assertEquals(listOf("C"), hashFake.clears, "hash cache should be cleared")
    }

    @Test
    fun notifyProcess_singleLocal_evict_evictsKvAndHash() {
        provider.register("C", true, CacheStrategy.SINGLE_LOCAL, hash = true)
        kvMgr.underlying("C").put("k", "v")
        notify(CacheOperatorVo(CacheOperatorVo.TYPE_EVICT, "C", "k"))
        assertNull(kvMgr.underlying("C").get("k"), "kv entry should be evicted")
        // hash doEvict delegates to handlers (none in unit context) -> no throw; clears stays empty.
        assertTrue(hashFake.clears.isEmpty())
    }

    @Test
    fun notifyProcess_nonSingleLocal_noCleanup() {
        provider.register("C", true, CacheStrategy.REMOTE, hash = true)
        kvMgr.underlying("C").put("k", "v")
        notify(CacheOperatorVo(CacheOperatorVo.TYPE_CLEAR, "C", null))
        assertEquals("v", kvMgr.underlying("C").get("k")?.get(), "REMOTE strategy must not be cleaned here")
        assertTrue(hashFake.clears.isEmpty())
    }

    @Test
    fun notifyProcess_evictWithNullKey_doesNotEvict() {
        provider.register("C", true, CacheStrategy.SINGLE_LOCAL, hash = true)
        kvMgr.underlying("C").put("k", "v")
        // TYPE_EVICT with null key: doClear's evict branch requires non-null key -> skipped.
        notify(CacheOperatorVo(CacheOperatorVo.TYPE_EVICT, "C", null))
        assertEquals("v", kvMgr.underlying("C").get("k")?.get())
    }

    @Test
    fun notifyProcess_onlyHashActive_resolvesStrategyFromHash() {
        // kv config inactive, hash config active SINGLE_LOCAL -> strategy resolved from hash, clears hash.
        provider.register("C", false, CacheStrategy.SINGLE_LOCAL, hash = true)
        // active=false makes kv inactive; but hash uses getHashCacheConfigs filter which still requires isActive.
        // Register a separate active hash config to drive the hash-active branch:
        provider.registerHashActive("C", CacheStrategy.SINGLE_LOCAL)
        notify(CacheOperatorVo(CacheOperatorVo.TYPE_CLEAR, "C", null))
        assertEquals(listOf("C"), hashFake.clears)
    }

    // ---- helpers ------------------------------------------------------------

    private fun registerHashCache(mgr: MixHashCacheManager, name: String, cache: IHashCache) {
        val field = MixHashCacheManager::class.java.getDeclaredField("hashCaches")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(mgr) as MutableMap<String, IHashCache>)[name] = cache
    }

    @Configuration
    open class EmptyConfig

    private class TestMixCacheManager : MixCacheManager() {
        private val under = ConcurrentHashMap<String, Cache>()
        private val wrappers = ConcurrentHashMap<String, MixCache>()
        fun underlying(name: String): Cache = under.computeIfAbsent(name) { ConcurrentMapCache(it, true) }
        override fun getCache(name: String): Cache =
            wrappers.computeIfAbsent(name) { MixCache(CacheStrategy.REMOTE, null, underlying(it)) }
    }

    private class RecordingHashCache : IHashCache {
        val clears = mutableListOf<String>()
        override fun <PK, E : IIdEntity<PK>> getById(cacheName: String, id: PK, entityClass: KClass<E>): E? = null
        override fun existsById(cacheName: String, id: Any): Boolean = false
        override fun <PK, E : IIdEntity<PK>> save(cacheName: String, entity: E, filterableProperties: Set<String>, sortableProperties: Set<String>) {}
        override fun <PK, E : IIdEntity<PK>> saveBatch(cacheName: String, entities: List<E>, filterableProperties: Set<String>, sortableProperties: Set<String>) {}
        override fun <PK, E : IIdEntity<PK>> deleteById(cacheName: String, id: PK, entityClass: KClass<E>, filterableProperties: Set<String>, sortableProperties: Set<String>) {}
        override fun <E : IIdEntity<*>> findByIds(cacheName: String, ids: Collection<*>, entityClass: KClass<E>): List<E> = emptyList()
        override fun <PK, E : IIdEntity<PK>> listAll(cacheName: String, entityClass: KClass<E>): List<E> = emptyList()
        override fun <PK, E : IIdEntity<PK>> listBySetIndex(cacheName: String, entityClass: KClass<E>, property: String, value: Any): List<E> = emptyList()
        override fun <PK, E : IIdEntity<PK>> listPageByZSetIndex(cacheName: String, entityClass: KClass<E>, zsetIndexName: String, offset: Long, limit: Long, desc: Boolean): List<E> = emptyList()
        override fun <PK, E : IIdEntity<PK>> list(cacheName: String, entityClass: KClass<E>, criteria: Criteria?, pageNo: Int, pageSize: Int, vararg orders: Order): List<E> = emptyList()
        override fun <PK, E : IIdEntity<PK>> refreshAll(cacheName: String, entities: List<E>, filterableProperties: Set<String>, sortableProperties: Set<String>) {}
        override fun clear(cacheName: String) { clears += cacheName }
    }

    private class TestProvider : ICacheConfigProvider {
        private val configs = ConcurrentHashMap<String, CacheConfig>()
        private val hashConfigs = ConcurrentHashMap<String, CacheConfig>()
        fun register(name: String, active: Boolean, strategy: CacheStrategy, hash: Boolean) {
            val cfg = CacheConfig().apply {
                this.name = name; this.active = active; this.strategy = strategy.name; this.hash = hash
            }
            configs[name] = cfg
            if (hash) hashConfigs[name] = cfg
        }
        fun registerHashActive(name: String, strategy: CacheStrategy) {
            hashConfigs[name] = CacheConfig().apply {
                this.name = name; this.active = true; this.strategy = strategy.name; this.hash = true
            }
        }
        override fun getCacheConfig(name: String): CacheConfig? = configs[name]
        override fun getAllCacheConfigs(): Map<String, CacheConfig> = configs
        override fun getLocalCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getHashCacheConfigs(): Map<String, CacheConfig> = hashConfigs
    }
}
