package io.kudos.ability.cache.common.kit

import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.core.hash.IHashCache
import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.context.kit.SpringKit
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.context.support.GenericApplicationContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Spring-backed tests for [HashCacheKit] handler-scan paths NOT reachable through overrideForTesting:
 *  - doEvict -> handlersFor(...).evict(id) actually invoking a handler.
 *  - reload -> handlersFor(...).reload(id).
 *  - reloadAll writeOnBoot=true -> handlersFor(...).reloadAll(true).
 *  - getValue(cacheName, id) untyped -> first handler's exposedEntityClass + getHashCache.getById.
 *
 * Dependencies come from a real ApplicationContext registered into [SpringKit]; the MixHashCacheManager's
 * internal hashCaches map is populated via reflection with a recording fake.
 *
 * @author K
 * @since 1.0.0
 */
internal class HashCacheKitSpringTest {

    private lateinit var ctx: GenericApplicationContext
    private lateinit var manager: MixHashCacheManager
    private lateinit var fake: RecordingHashCache
    private lateinit var provider: TestProvider
    private lateinit var handler: RecordingHandler

    @BeforeTest
    fun setup() {
        HashCacheKit.resetForTesting()
        manager = MixHashCacheManager()
        fake = RecordingHashCache()
        provider = TestProvider()
        handler = RecordingHandler("C")
        registerHashCache(manager, "C", fake)

        ctx = GenericApplicationContext(DefaultListableBeanFactory())
        ctx.beanFactory.registerSingleton("mixHashCacheManager", manager)
        ctx.beanFactory.registerSingleton("cacheConfigProvider", provider)
        ctx.beanFactory.registerSingleton("recordingHashHandler", handler)
        ctx.refresh()
        SpringKit.applicationContext = ctx
    }

    @AfterTest
    fun teardown() {
        HashCacheKit.resetForTesting()
        ctx.close()
        SpringKit.applicationContext = null
    }

    @Test
    fun doEvict_active_invokesHandlerEvict() {
        provider.register("C", true, CacheStrategy.REMOTE)
        HashCacheKit.doEvict("C", "7")
        assertEquals(listOf<Any>("7"), handler.evicted)
    }

    @Test
    fun evict_remote_invokesHandlerEvict() {
        provider.register("C", true, CacheStrategy.REMOTE)
        HashCacheKit.evict("C", "9")
        assertEquals(listOf<Any>("9"), handler.evicted)
    }

    @Test
    fun reload_active_invokesHandlerReload() {
        provider.register("C", true, CacheStrategy.REMOTE)
        HashCacheKit.reload("C", "5")
        assertEquals(listOf<Any>("5"), handler.reloaded)
    }

    @Test
    fun reloadAll_writeOnBootTrue_invokesHandlerReloadAll() {
        provider.register("C", true, CacheStrategy.REMOTE, writeOnBoot = true)
        HashCacheKit.reloadAll("C")
        assertEquals(listOf(true), handler.reloadAllClears)
    }

    @Test
    fun getValueUntyped_active_usesHandlerEntityClass() {
        provider.register("C", true, CacheStrategy.REMOTE)
        val entity = Entity("3")
        fake.byId["3"] = entity
        assertEquals(entity, HashCacheKit.getValue("C", "3"))
    }

    @Test
    fun getValueUntyped_active_noEntry_null() {
        provider.register("C", true, CacheStrategy.REMOTE)
        assertNull(HashCacheKit.getValue("C", "absent"))
    }

    // ---- helpers ------------------------------------------------------------

    private fun registerHashCache(mgr: MixHashCacheManager, name: String, cache: IHashCache) {
        val field = MixHashCacheManager::class.java.getDeclaredField("hashCaches")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(mgr) as MutableMap<String, IHashCache>)[name] = cache
    }

    data class Entity(override val id: String) : IIdEntity<String>

    private class RecordingHandler(private val name: String) : AbstractHashCacheHandler<Entity>() {
        val evicted = mutableListOf<Any>()
        val reloaded = mutableListOf<Any>()
        val reloadAllClears = mutableListOf<Boolean>()
        override fun cacheName(): String = name
        override fun entityClass(): KClass<Entity> = Entity::class
        override fun evict(id: Any) { evicted += id }
        override fun reload(id: Any) { reloaded += id }
        override fun reloadAll(clear: Boolean) { reloadAllClears += clear }
    }

    private class RecordingHashCache : IHashCache {
        val byId = mutableMapOf<Any?, Any?>()
        @Suppress("UNCHECKED_CAST")
        override fun <PK, E : IIdEntity<PK>> getById(cacheName: String, id: PK, entityClass: KClass<E>): E? = byId[id] as E?
        override fun existsById(cacheName: String, id: Any): Boolean = byId.containsKey(id)
        override fun <PK, E : IIdEntity<PK>> save(cacheName: String, entity: E, filterableProperties: Set<String>, sortableProperties: Set<String>) {}
        override fun <PK, E : IIdEntity<PK>> saveBatch(cacheName: String, entities: List<E>, filterableProperties: Set<String>, sortableProperties: Set<String>) {}
        override fun <PK, E : IIdEntity<PK>> deleteById(cacheName: String, id: PK, entityClass: KClass<E>, filterableProperties: Set<String>, sortableProperties: Set<String>) {}
        override fun <E : IIdEntity<*>> findByIds(cacheName: String, ids: Collection<*>, entityClass: KClass<E>): List<E> = emptyList()
        override fun <PK, E : IIdEntity<PK>> listAll(cacheName: String, entityClass: KClass<E>): List<E> = emptyList()
        override fun <PK, E : IIdEntity<PK>> listBySetIndex(cacheName: String, entityClass: KClass<E>, property: String, value: Any): List<E> = emptyList()
        override fun <PK, E : IIdEntity<PK>> listPageByZSetIndex(cacheName: String, entityClass: KClass<E>, zsetIndexName: String, offset: Long, limit: Long, desc: Boolean): List<E> = emptyList()
        override fun <PK, E : IIdEntity<PK>> list(cacheName: String, entityClass: KClass<E>, criteria: Criteria?, pageNo: Int, pageSize: Int, vararg orders: Order): List<E> = emptyList()
        override fun <PK, E : IIdEntity<PK>> refreshAll(cacheName: String, entities: List<E>, filterableProperties: Set<String>, sortableProperties: Set<String>) {}
        override fun clear(cacheName: String) {}
    }

    private class TestProvider : ICacheConfigProvider {
        private val hashConfigs = ConcurrentHashMap<String, CacheConfig>()
        fun register(name: String, active: Boolean, strategy: CacheStrategy, writeOnBoot: Boolean = false) {
            hashConfigs[name] = CacheConfig().apply {
                this.name = name; this.active = active; this.strategy = strategy.name
                this.hash = true; this.writeOnBoot = writeOnBoot
            }
        }
        override fun getCacheConfig(name: String): CacheConfig? = hashConfigs[name]
        override fun getAllCacheConfigs(): Map<String, CacheConfig> = hashConfigs
        override fun getLocalCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getHashCacheConfigs(): Map<String, CacheConfig> = hashConfigs
    }
}
