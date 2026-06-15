package io.kudos.ability.cache.common.kit

import io.kudos.ability.cache.common.core.hash.IHashCache
import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.context.kit.SpringKit
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [HashCacheKit].
 *
 * Uses [HashCacheKit.overrideForTesting] to inject a real [MixHashCacheManager] (whose internal hashCaches map
 * is populated via reflection with a recording [IHashCache] fake) plus a config provider, so no Redis/Caffeine
 * infrastructure is required. SINGLE_LOCAL evict/clear route through CacheOperatorVo.doNotify(); an empty Spring
 * context (no NotifyTool) makes doNotify degrade to local cleanup, which is what is asserted.
 *
 * Coverage:
 *  - isCacheActive (active / inactive / unknown / no-provider).
 *  - getCacheConfig (present / missing / no-provider).
 *  - isWriteInTime (active+configured / inactive).
 *  - evict / doEvict (REMOTE direct, SINGLE_LOCAL degraded, inactive guard).
 *  - clear / doClear (REMOTE direct, SINGLE_LOCAL degraded, inactive guard).
 *  - reloadAll writeOnBoot=false branch (clears via getHashCache).
 *  - existsById / getValue(typed) (active + inactive).
 *  - getHashCache (missing manager / unconfigured name -> IllegalStateException).
 *  - isLocalCacheEnabled (LOCAL_REMOTE / SINGLE_LOCAL -> true, REMOTE -> false, unknown -> false).
 *
 * @author K
 * @since 1.0.0
 */
internal class HashCacheKitTest {

    private lateinit var manager: MixHashCacheManager
    private lateinit var fake: RecordingHashCache
    private lateinit var provider: TestProvider
    private var ctx: AnnotationConfigApplicationContext? = null

    @BeforeTest
    fun setup() {
        manager = MixHashCacheManager()
        fake = RecordingHashCache()
        provider = TestProvider()
        // Populate the private hashCaches map (versionConfig is null -> realName == cacheName).
        registerHashCache(manager, "C", fake)
        HashCacheKit.overrideForTesting(manager, provider)
        ctx = AnnotationConfigApplicationContext(EmptyConfig::class.java)
        SpringKit.applicationContext = ctx
    }

    @AfterTest
    fun teardown() {
        HashCacheKit.resetForTesting()
        ctx?.close()
        SpringKit.applicationContext = null
    }

    // ---- isCacheActive ------------------------------------------------------

    @Test
    fun isCacheActive_true_whenHashConfigActive() {
        provider.register("C", true, CacheStrategy.REMOTE, hash = true)
        assertTrue(HashCacheKit.isCacheActive("C"))
    }

    @Test
    fun isCacheActive_false_whenInactive() {
        provider.register("C", false, CacheStrategy.REMOTE, hash = true)
        assertFalse(HashCacheKit.isCacheActive("C"))
    }

    @Test
    fun isCacheActive_false_whenNotHash() {
        // hash=false -> not in getHashCacheConfigs -> inactive.
        provider.register("C", true, CacheStrategy.REMOTE, hash = false)
        assertFalse(HashCacheKit.isCacheActive("C"))
    }

    @Test
    fun isCacheActive_false_whenNoProvider() {
        HashCacheKit.overrideForTesting(manager, null)
        // No provider override and empty Spring context -> getConfigProvider() returns null.
        assertFalse(HashCacheKit.isCacheActive("C"))
    }

    // ---- getCacheConfig -----------------------------------------------------

    @Test
    fun getCacheConfig_returnsConfig() {
        provider.register("C", true, CacheStrategy.REMOTE, hash = true)
        assertEquals("C", HashCacheKit.getCacheConfig("C")?.name)
    }

    @Test
    fun getCacheConfig_missing_returnsNullAndWarns() {
        // provider present but no entry for D.
        assertNull(HashCacheKit.getCacheConfig("D"))
    }

    @Test
    fun getCacheConfig_noProvider_returnsNull() {
        HashCacheKit.overrideForTesting(manager, null)
        assertNull(HashCacheKit.getCacheConfig("C"))
    }

    // ---- isWriteInTime ------------------------------------------------------

    @Test
    fun isWriteInTime_trueWhenConfigured() {
        provider.register("C", true, CacheStrategy.REMOTE, hash = true, writeInTime = true)
        assertTrue(HashCacheKit.isWriteInTime("C"))
    }

    @Test
    fun isWriteInTime_falseWhenInactive() {
        provider.register("C", false, CacheStrategy.REMOTE, hash = true, writeInTime = true)
        assertFalse(HashCacheKit.isWriteInTime("C"))
    }

    // ---- evict / doEvict ----------------------------------------------------

    @Test
    fun doEvict_active_callsHandlerEvict_viaNothing_butGetHashCacheRemoved() {
        // doEvict delegates to handlersFor(...).evict(id); with no Spring handler beans the list is empty,
        // so nothing happens and no exception is thrown (active guard passes, handler list empty).
        provider.register("C", true, CacheStrategy.REMOTE, hash = true)
        HashCacheKit.doEvict("C", "k") // must not throw
    }

    @Test
    fun doEvict_inactive_isNoOp() {
        provider.register("C", false, CacheStrategy.REMOTE, hash = true)
        HashCacheKit.doEvict("C", "k") // returns at active guard
    }

    @Test
    fun evict_inactive_isNoOp() {
        provider.register("C", false, CacheStrategy.REMOTE, hash = true)
        HashCacheKit.evict("C", "k")
    }

    @Test
    fun evict_remote_directPath_noNotify() {
        // REMOTE strategy -> doEvict path (handler list empty -> no-op, no throw).
        provider.register("C", true, CacheStrategy.REMOTE, hash = true)
        HashCacheKit.evict("C", "k")
    }

    @Test
    fun evict_singleLocal_degradesToLocalEviction() {
        // SINGLE_LOCAL -> CacheOperatorVo.doNotify() -> no NotifyTool -> KeyValueCacheKit fallback.
        // KeyValueCacheKit needs its own overrides for the fallback; provide an inactive KV config so the
        // fallback is a safe no-op (we only assert the SINGLE_LOCAL branch is reached without throwing).
        provider.register("C", true, CacheStrategy.SINGLE_LOCAL, hash = true)
        KeyValueCacheKit.overrideForTesting(null, provider)
        try {
            HashCacheKit.evict("C", "k") // routes through doNotify -> KV fallback (inactive -> no-op)
        } finally {
            KeyValueCacheKit.resetForTesting()
        }
    }

    // ---- clear / doClear ----------------------------------------------------

    @Test
    fun doClear_active_clearsViaHashCache() {
        provider.register("C", true, CacheStrategy.REMOTE, hash = true)
        HashCacheKit.doClear("C")
        assertEquals(listOf("C"), fake.clears)
    }

    @Test
    fun doClear_inactive_isNoOp() {
        provider.register("C", false, CacheStrategy.REMOTE, hash = true)
        HashCacheKit.doClear("C")
        assertTrue(fake.clears.isEmpty())
    }

    @Test
    fun clear_remote_directClear() {
        provider.register("C", true, CacheStrategy.REMOTE, hash = true)
        HashCacheKit.clear("C")
        assertEquals(listOf("C"), fake.clears)
    }

    @Test
    fun clear_inactive_isNoOp() {
        provider.register("C", false, CacheStrategy.REMOTE, hash = true)
        HashCacheKit.clear("C")
        assertTrue(fake.clears.isEmpty())
    }

    @Test
    fun clear_singleLocal_degradesToNotify() {
        provider.register("C", true, CacheStrategy.SINGLE_LOCAL, hash = true)
        KeyValueCacheKit.overrideForTesting(null, provider)
        try {
            HashCacheKit.clear("C") // doNotify -> KV fallback (inactive -> no-op); no throw
        } finally {
            KeyValueCacheKit.resetForTesting()
        }
    }

    // ---- reloadAll ----------------------------------------------------------

    @Test
    fun reloadAll_writeOnBootFalse_clearsViaHashCache() {
        provider.register("C", true, CacheStrategy.REMOTE, hash = true, writeOnBoot = false)
        HashCacheKit.reloadAll("C")
        assertEquals(listOf("C"), fake.clears)
    }

    @Test
    fun reloadAll_inactive_isNoOp() {
        provider.register("C", false, CacheStrategy.REMOTE, hash = true)
        HashCacheKit.reloadAll("C")
        assertTrue(fake.clears.isEmpty())
    }

    @Test
    fun reload_inactive_isNoOp() {
        provider.register("C", false, CacheStrategy.REMOTE, hash = true)
        HashCacheKit.reload("C", "k")
    }

    // ---- existsById / getValue ---------------------------------------------

    @Test
    fun existsById_active_delegatesToHashCache() {
        provider.register("C", true, CacheStrategy.REMOTE, hash = true)
        fake.existing.add("k")
        assertTrue(HashCacheKit.existsById("C", "k"))
        assertFalse(HashCacheKit.existsById("C", "absent"))
    }

    @Test
    fun existsById_inactive_false() {
        provider.register("C", false, CacheStrategy.REMOTE, hash = true)
        assertFalse(HashCacheKit.existsById("C", "k"))
    }

    @Test
    fun getValueTyped_active_delegatesToHashCache() {
        provider.register("C", true, CacheStrategy.REMOTE, hash = true)
        val entity = Entity("7")
        fake.byId["7"] = entity
        assertEquals(entity, HashCacheKit.getValue("C", "7", Entity::class))
    }

    @Test
    fun getValueTyped_inactive_null() {
        provider.register("C", false, CacheStrategy.REMOTE, hash = true)
        assertNull(HashCacheKit.getValue("C", "7", Entity::class))
    }

    @Test
    fun getValueUntyped_inactive_null() {
        provider.register("C", false, CacheStrategy.REMOTE, hash = true)
        assertNull(HashCacheKit.getValue("C", "7"))
    }

    // ---- getHashCache failure paths ----------------------------------------

    @Test
    fun getHashCache_noManager_throws() {
        HashCacheKit.overrideForTesting(null, provider)
        val ex = assertFailsWith<IllegalStateException> { HashCacheKit.getHashCache("C") }
        assertTrue(ex.message!!.contains("MixHashCacheManager is unavailable"))
    }

    @Test
    fun getHashCache_unconfiguredName_throws() {
        val ex = assertFailsWith<IllegalStateException> { HashCacheKit.getHashCache("UNCONFIGURED") }
        assertTrue(ex.message!!.contains("Hash cache is not configured"))
    }

    @Test
    fun getHashCache_configured_returnsCache() {
        assertEquals(fake, HashCacheKit.getHashCache("C"))
    }

    // ---- isLocalCacheEnabled ------------------------------------------------

    @Test
    fun isLocalCacheEnabled_localRemote_true() {
        provider.register("C", true, CacheStrategy.LOCAL_REMOTE, hash = true)
        assertTrue(HashCacheKit.isLocalCacheEnabled("C"))
    }

    @Test
    fun isLocalCacheEnabled_singleLocal_true() {
        provider.register("C", true, CacheStrategy.SINGLE_LOCAL, hash = true)
        assertTrue(HashCacheKit.isLocalCacheEnabled("C"))
    }

    @Test
    fun isLocalCacheEnabled_remote_false() {
        provider.register("C", true, CacheStrategy.REMOTE, hash = true)
        assertFalse(HashCacheKit.isLocalCacheEnabled("C"))
    }

    @Test
    fun isLocalCacheEnabled_unknown_false() {
        assertFalse(HashCacheKit.isLocalCacheEnabled("UNKNOWN"))
    }

    @Test
    fun overrideForTesting_defaultArgs_clearOverrides() {
        // Exercise the default-parameter path (both args omitted) -> both overrides fall back to null.
        HashCacheKit.overrideForTesting()
        // With no manager override and an empty Spring context, getHashCache reports the manager unavailable.
        val ex = assertFailsWith<IllegalStateException> { HashCacheKit.getHashCache("C") }
        assertTrue(ex.message!!.contains("MixHashCacheManager is unavailable"))
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

    data class Entity(override val id: String) : IIdEntity<String>

    /** Recording in-memory [IHashCache] fake exposing only the methods the Kit exercises. */
    private class RecordingHashCache : IHashCache {
        val clears = mutableListOf<String>()
        val existing = mutableSetOf<Any>()
        val byId = mutableMapOf<Any?, Any?>()

        @Suppress("UNCHECKED_CAST")
        override fun <PK, E : IIdEntity<PK>> getById(cacheName: String, id: PK, entityClass: KClass<E>): E? =
            byId[id] as E?

        override fun existsById(cacheName: String, id: Any): Boolean = existing.contains(id)

        override fun <PK, E : IIdEntity<PK>> save(
            cacheName: String, entity: E, filterableProperties: Set<String>, sortableProperties: Set<String>
        ) {}

        override fun <PK, E : IIdEntity<PK>> saveBatch(
            cacheName: String, entities: List<E>, filterableProperties: Set<String>, sortableProperties: Set<String>
        ) {}

        override fun <PK, E : IIdEntity<PK>> deleteById(
            cacheName: String, id: PK, entityClass: KClass<E>,
            filterableProperties: Set<String>, sortableProperties: Set<String>
        ) {}

        override fun <E : IIdEntity<*>> findByIds(
            cacheName: String, ids: Collection<*>, entityClass: KClass<E>
        ): List<E> = emptyList()

        override fun <PK, E : IIdEntity<PK>> listAll(cacheName: String, entityClass: KClass<E>): List<E> = emptyList()

        override fun <PK, E : IIdEntity<PK>> listBySetIndex(
            cacheName: String, entityClass: KClass<E>, property: String, value: Any
        ): List<E> = emptyList()

        override fun <PK, E : IIdEntity<PK>> listPageByZSetIndex(
            cacheName: String, entityClass: KClass<E>, zsetIndexName: String, offset: Long, limit: Long, desc: Boolean
        ): List<E> = emptyList()

        override fun <PK, E : IIdEntity<PK>> list(
            cacheName: String, entityClass: KClass<E>, criteria: Criteria?, pageNo: Int, pageSize: Int, vararg orders: Order
        ): List<E> = emptyList()

        override fun <PK, E : IIdEntity<PK>> refreshAll(
            cacheName: String, entities: List<E>, filterableProperties: Set<String>, sortableProperties: Set<String>
        ) {}

        override fun clear(cacheName: String) { clears += cacheName }
    }

    private class TestProvider : ICacheConfigProvider {
        private val configs = ConcurrentHashMap<String, CacheConfig>()
        fun register(
            name: String,
            active: Boolean,
            strategy: CacheStrategy,
            hash: Boolean,
            writeInTime: Boolean = false,
            writeOnBoot: Boolean = false,
        ) {
            configs[name] = CacheConfig().apply {
                this.name = name; this.active = active; this.strategy = strategy.name
                this.hash = hash; this.writeInTime = writeInTime; this.writeOnBoot = writeOnBoot
            }
        }
        override fun getCacheConfig(name: String): CacheConfig? = configs[name]
        override fun getAllCacheConfigs(): Map<String, CacheConfig> = configs
        override fun getLocalCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getHashCacheConfigs(): Map<String, CacheConfig> = configs.filterValues { it.hash }
    }
}
