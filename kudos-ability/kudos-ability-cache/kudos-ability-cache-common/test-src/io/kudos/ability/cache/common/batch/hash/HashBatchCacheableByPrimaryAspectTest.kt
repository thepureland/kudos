package io.kudos.ability.cache.common.batch.hash

import io.kudos.ability.cache.common.aop.FakeProceedingJoinPoint
import io.kudos.ability.cache.common.aop.methodOf
import io.kudos.ability.cache.common.core.hash.IHashCache
import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.context.kit.SpringKit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.cache.annotation.CacheConfig as SpringCacheConfig
import org.springframework.context.support.StaticApplicationContext
import kotlin.reflect.KClass
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for [HashBatchCacheableByPrimaryAspect.around], exercising every branch via a hand-built
 * [FakeProceedingJoinPoint]:
 *  - synthetic method (kotlinFunction null) / no annotation / no cacheName -> proceed as Map,
 *  - non-Map return type -> error,
 *  - empty keys -> emptyMap,
 *  - partial cache hit -> only missing keys queried, results merged + saved (writeInTime),
 *  - all hit -> no proceed,
 *  - inactive cache / writeInTime=false -> no findByIds / no saveBatch,
 *  - validatedMap rejects non-Map and non-String keys,
 *  - class-level @CacheConfig fallback.
 *
 * @author K
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HashBatchCacheableByPrimaryAspectTest {

    private val aspect = HashBatchCacheableByPrimaryAspect()
    private lateinit var hashCache: MemHashCache
    private lateinit var provider: BatchConfigProvider
    private lateinit var ctx: StaticApplicationContext

    @BeforeAll
    fun classSetup() {
        ctx = StaticApplicationContext().apply { refresh() }
        ctx.beanFactory.registerSingleton("defaultHashBatchKeysGenerator", DefaultHashBatchKeysGenerator())
        SpringKit.applicationContext = ctx

        hashCache = MemHashCache()
        provider = BatchConfigProvider()
        provider.register(CACHE, active = true, writeInTime = true)
        provider.register(CLASS_CACHE, active = true, writeInTime = true)
        provider.register(INACTIVE, active = false)
        provider.register(NO_WRITE, active = true, writeInTime = false)
        HashCacheKit.overrideForTesting(
            manager = buildManager(CACHE to hashCache, CLASS_CACHE to hashCache, INACTIVE to hashCache, NO_WRITE to hashCache),
            configProvider = provider
        )
        KeyValueCacheKit.overrideForTesting(configProvider = provider)
    }

    @AfterAll
    fun classTeardown() {
        HashCacheKit.resetForTesting()
        KeyValueCacheKit.resetForTesting()
        ctx.close()
    }

    @BeforeTest
    fun reset() {
        hashCache.reset()
    }

    @Test
    fun pointcut_isInvocable() {
        aspect.cut()
    }

    @Test
    fun syntheticMethod_proceedsAsMap() {
        val synthetic = BatchOps::class.java.declaredMethods.first { it.name == "withDefault\$default" }
        val pjp = FakeProceedingJoinPoint(synthetic, arrayOf(), BatchOps()) { mapOf("1" to E("1")) }
        val result = aspect.around(pjp)
        assertEquals(setOf("1"), result.keys)
        assertEquals(1, pjp.proceedCount)
    }

    @Test
    fun methodWithoutAnnotation_proceedsAsMap() {
        val pjp = pjpOf("notAnnotated", listOf("1")) { mapOf("1" to E("1")) }
        val result = aspect.around(pjp)
        assertEquals(setOf("1"), result.keys)
        assertEquals(1, pjp.proceedCount)
    }

    @Test
    fun noCacheNameAnywhere_proceedsAsMap() {
        val pjp = pjpOf("noCacheName", listOf("1")) { mapOf("1" to E("1")) }
        val result = aspect.around(pjp)
        assertEquals(setOf("1"), result.keys)
        assertEquals(1, pjp.proceedCount)
    }

    @Test
    fun nonMapReturnType_throws() {
        val pjp = pjpOf("wrongReturn", listOf("1")) { "x" }
        assertFailsWith<IllegalStateException> { aspect.around(pjp) }
    }

    @Test
    fun emptyKeys_returnsEmptyMap_withoutProceed() {
        val pjp = pjpOf("byIds", emptyList()) { error("should not proceed") }
        assertTrue(aspect.around(pjp).isEmpty())
        assertEquals(0, pjp.proceedCount)
    }

    @Test
    fun allCached_returnsWithoutProceed() {
        hashCache.put(E("1")); hashCache.put(E("2"))
        val pjp = pjpOf("byIds", listOf("1", "2")) { error("should not proceed") }
        val result = aspect.around(pjp)
        assertEquals(setOf("1", "2"), result.keys)
        assertEquals(0, pjp.proceedCount)
    }

    @Test
    fun partialHit_queriesOnlyMissing_mergesAndSaves() {
        hashCache.put(E("1", "cached"))
        val pjp = pjpOf("byIds", listOf("1", "2", "3")) {
            // business method returns the missing entries
            mapOf("2" to E("2", "fresh"), "3" to E("3", "fresh"))
        }
        val result = aspect.around(pjp)
        assertEquals(setOf("1", "2", "3"), result.keys)
        assertEquals("cached", (result["1"] as E).type)
        assertEquals("fresh", (result["2"] as E).type)
        assertEquals(1, pjp.proceedCount)
        // writeInTime -> the 2 fresh entities saved as a batch
        assertEquals(1, hashCache.saveBatchCalls.size)
        assertEquals(setOf("2", "3"), hashCache.saveBatchCalls.single().map { it.id }.toSet())
    }

    @Test
    fun partialHit_writeInTimeFalse_doesNotSave() {
        val pjp = FakeProceedingJoinPoint(
            methodOf(BatchOps::class.java, "noWrite"), arrayOf(listOf("1")), BatchOps()
        ) { mapOf("1" to E("1")) }
        val result = aspect.around(pjp)
        assertEquals(setOf("1"), result.keys)
        assertTrue(hashCache.saveBatchCalls.isEmpty(), "writeInTime=false must not save")
    }

    @Test
    fun inactiveCache_skipsFindByIds_butStillProceeds() {
        val pjp = FakeProceedingJoinPoint(
            methodOf(BatchOps::class.java, "inactive"), arrayOf(listOf("1")), BatchOps()
        ) { mapOf("1" to E("1")) }
        val result = aspect.around(pjp)
        assertEquals(setOf("1"), result.keys)
        assertEquals(0, hashCache.findByIdsCount, "inactive cache must not query")
        assertTrue(hashCache.saveBatchCalls.isEmpty())
    }

    @Test
    fun uncachedReadReturnsNull_returnsOnlyCached() {
        hashCache.put(E("1", "cached"))
        // proceed returns a Map missing the requested key -> uncachedMap has key 2 with null -> still merged null filtered out
        val pjp = pjpOf("byIds", listOf("1", "2")) { mapOf<String, Any?>("2" to null) }
        val result = aspect.around(pjp)
        // key 2's value is null -> filtered out; only cached 1 remains
        assertEquals(setOf("1"), result.keys)
    }

    @Test
    fun nonStringKeyInReturn_throws() {
        // validatedMap rejects non-String keys via require(...) -> IllegalArgumentException
        val pjp = pjpOf("byIds", listOf("1")) { mapOf(1 to E("1")) }
        assertFailsWith<IllegalArgumentException> { aspect.around(pjp) }
    }

    @Test
    fun classLevelCacheConfig_isUsedAsFallback() {
        hashCache.put(E("c1"))
        val pjp = FakeProceedingJoinPoint(
            methodOf(ClassLevelBatchOps::class.java, "byIds"), arrayOf(listOf("c1")), ClassLevelBatchOps()
        ) { error("should not proceed") }
        val result = aspect.around(pjp)
        assertEquals(setOf("c1"), result.keys)
        assertEquals(0, pjp.proceedCount)
    }

    @Test
    fun arrayIntParam_uncachedTypeRestored() {
        // all Array<X>::class erase to kotlin.Array; this exercises the Array branch of readUncachedData.
        val pjp = FakeProceedingJoinPoint(
            methodOf(BatchOps::class.java, "byIntArray"), arrayOf(arrayOf(1, 2)), BatchOps()
        ) { mapOf("1" to E("1"), "2" to E("2")) }
        val result = aspect.around(pjp)
        assertEquals(setOf("1", "2"), result.keys)
    }

    @Test
    fun missingGeneratorBean_fallsBackToDefault() {
        // keysGenerator name resolves to no bean -> getKeysGenerator falls back to DefaultHashBatchKeysGenerator
        hashCache.put(E("1"))
        val pjp = FakeProceedingJoinPoint(
            methodOf(BatchOps::class.java, "customGen"), arrayOf(listOf("1")), BatchOps()
        ) { error("should not proceed") }
        assertEquals(setOf("1"), aspect.around(pjp).keys)
    }

    @Test
    fun proceedAsStringAnyMap_nonMap_throws() {
        // no cacheName -> proceedAsStringAnyMap; proceed returns non-Map -> validatedMap error
        val pjp = pjpOf("noCacheName", listOf("1")) { "not-a-map" }
        assertFailsWith<IllegalStateException> { aspect.around(pjp) }
    }

    @Test
    fun setParam_uncachedTypeRestored() {
        val pjp = FakeProceedingJoinPoint(
            methodOf(BatchOps::class.java, "bySet"), arrayOf(setOf("a", "b")), BatchOps()
        ) { mapOf("a" to E("a"), "b" to E("b")) }
        assertEquals(setOf("a", "b"), aspect.around(pjp).keys)
    }

    @Test
    fun classLevelEmptyCacheConfig_fallsBackToProceed() {
        val pjp = FakeProceedingJoinPoint(
            methodOf(EmptyClassConfigBatchOps::class.java, "byIds"), arrayOf(listOf("1")), EmptyClassConfigBatchOps()
        ) { mapOf("1" to E("1")) }
        val result = aspect.around(pjp)
        assertEquals(setOf("1"), result.keys)
        assertEquals(1, pjp.proceedCount)
    }

    // ---- helpers ----

    private fun pjpOf(method: String, ids: List<String>, proceedFn: () -> Any?): FakeProceedingJoinPoint =
        FakeProceedingJoinPoint(methodOf(BatchOps::class.java, method), arrayOf(ids), BatchOps(), proceedFn)

    data class E(override val id: String, val type: String? = null) : IIdEntity<String>

    @Suppress("unused", "UNUSED_PARAMETER")
    internal class BatchOps {
        @HashBatchCacheableByPrimary(cacheNames = [CACHE], entityClass = E::class,
            filterableProperties = ["type"], sortableProperties = ["score"])
        fun byIds(ids: List<String>): Map<String, E?> = emptyMap()

        fun notAnnotated(ids: List<String>): Map<String, E?> = emptyMap()

        @HashBatchCacheableByPrimary(entityClass = E::class)
        fun noCacheName(ids: List<String>): Map<String, E?> = emptyMap()

        @HashBatchCacheableByPrimary(cacheNames = [CACHE], entityClass = E::class)
        fun wrongReturn(ids: List<String>): String = ""

        @HashBatchCacheableByPrimary(cacheNames = [NO_WRITE], entityClass = E::class)
        fun noWrite(ids: List<String>): Map<String, E?> = emptyMap()

        @HashBatchCacheableByPrimary(cacheNames = [INACTIVE], entityClass = E::class)
        fun inactive(ids: List<String>): Map<String, E?> = emptyMap()

        @HashBatchCacheableByPrimary(cacheNames = [CACHE], entityClass = E::class)
        fun withDefault(ids: List<String> = listOf("d")): Map<String, E?> = emptyMap()

        @HashBatchCacheableByPrimary(cacheNames = [CACHE], entityClass = E::class)
        fun byIntArray(ids: Array<Int>): Map<String, E?> = emptyMap()

        @HashBatchCacheableByPrimary(cacheNames = [CACHE], entityClass = E::class)
        fun bySet(ids: Set<String>): Map<String, E?> = emptyMap()

        @HashBatchCacheableByPrimary(cacheNames = [CACHE], keysGenerator = "noSuchGeneratorBean", entityClass = E::class)
        fun customGen(ids: List<String>): Map<String, E?> = emptyMap()
    }

    @SpringCacheConfig(cacheNames = [CLASS_CACHE])
    @Suppress("unused", "UNUSED_PARAMETER")
    internal class ClassLevelBatchOps {
        @HashBatchCacheableByPrimary(entityClass = E::class)
        fun byIds(ids: List<String>): Map<String, E?> = emptyMap()
    }

    @SpringCacheConfig
    @Suppress("unused", "UNUSED_PARAMETER")
    internal class EmptyClassConfigBatchOps {
        @HashBatchCacheableByPrimary(entityClass = E::class)
        fun byIds(ids: List<String>): Map<String, E?> = emptyMap()
    }

    /** In-memory hash cache supporting findByIds + saveBatch (the two methods the aspect uses). */
    private class MemHashCache : IHashCache {
        private val entities = linkedMapOf<Any?, IIdEntity<*>>()
        val saveBatchCalls = mutableListOf<List<IIdEntity<*>>>()
        var findByIdsCount = 0
            private set

        fun put(e: IIdEntity<*>) { entities[e.id] = e }
        fun reset() { entities.clear(); saveBatchCalls.clear(); findByIdsCount = 0 }

        @Suppress("UNCHECKED_CAST")
        override fun <E : IIdEntity<*>> findByIds(cacheName: String, ids: Collection<*>, entityClass: KClass<E>): List<E> {
            findByIdsCount++
            return ids.mapNotNull { entities[it] } as List<E>
        }
        override fun <PK, E : IIdEntity<PK>> saveBatch(cacheName: String, entities: List<E>, filterableProperties: Set<String>, sortableProperties: Set<String>) {
            saveBatchCalls.add(entities)
            entities.forEach { this.entities[it.id] = it }
        }
        // unused
        override fun <PK, E : IIdEntity<PK>> getById(cacheName: String, id: PK, entityClass: KClass<E>): E? = error("unused")
        override fun existsById(cacheName: String, id: Any): Boolean = error("unused")
        override fun <PK, E : IIdEntity<PK>> save(cacheName: String, entity: E, filterableProperties: Set<String>, sortableProperties: Set<String>) = error("unused")
        override fun <PK, E : IIdEntity<PK>> deleteById(cacheName: String, id: PK, entityClass: KClass<E>, filterableProperties: Set<String>, sortableProperties: Set<String>) = error("unused")
        override fun <PK, E : IIdEntity<PK>> listAll(cacheName: String, entityClass: KClass<E>): List<E> = error("unused")
        override fun <PK, E : IIdEntity<PK>> listBySetIndex(cacheName: String, entityClass: KClass<E>, property: String, value: Any): List<E> = error("unused")
        override fun <PK, E : IIdEntity<PK>> listPageByZSetIndex(cacheName: String, entityClass: KClass<E>, zsetIndexName: String, offset: Long, limit: Long, desc: Boolean): List<E> = error("unused")
        override fun <PK, E : IIdEntity<PK>> list(cacheName: String, entityClass: KClass<E>, criteria: Criteria?, pageNo: Int, pageSize: Int, vararg orders: Order): List<E> = error("unused")
        override fun <PK, E : IIdEntity<PK>> refreshAll(cacheName: String, entities: List<E>, filterableProperties: Set<String>, sortableProperties: Set<String>) = error("unused")
        override fun clear(cacheName: String) = error("unused")
    }

    private fun buildManager(vararg caches: Pair<String, IHashCache>): MixHashCacheManager {
        val manager = MixHashCacheManager()
        val field = MixHashCacheManager::class.java.getDeclaredField("hashCaches")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val map = field.get(manager) as MutableMap<String, IHashCache>
        caches.forEach { (name, cache) -> map[name] = cache }
        return manager
    }

    private class BatchConfigProvider : ICacheConfigProvider {
        private val configs = mutableMapOf<String, CacheConfig>()
        fun register(name: String, active: Boolean, writeInTime: Boolean = false) {
            configs[name] = CacheConfig().apply {
                this.name = name; this.active = active; this.writeInTime = writeInTime; this.hash = true
            }
        }
        override fun getCacheConfig(name: String): CacheConfig? = configs[name]
        override fun getAllCacheConfigs(): Map<String, CacheConfig> = configs
        override fun getLocalCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
    }

    companion object {
        private const val CACHE = "HASH_BATCH_TEST"
        private const val CLASS_CACHE = "HASH_BATCH_CLASS_TEST"
        private const val INACTIVE = "HASH_BATCH_INACTIVE_TEST"
        private const val NO_WRITE = "HASH_BATCH_NO_WRITE_TEST"
    }
}
