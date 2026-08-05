package io.kudos.ability.cache.common.batch.hash

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
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.Signature
import org.aspectj.lang.reflect.MethodSignature
import org.aspectj.lang.reflect.SourceLocation
import org.aspectj.runtime.internal.AroundClosure
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.context.support.StaticApplicationContext
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for [HashBatchCacheableByPrimaryAspect] array element-type dispatch (bug C2b).
 *
 * Same erasure problem as bug C2: dispatching on `when (clazz) { Array<String>::class -> ...; Array<Int>::class -> ... }`
 * collapses all Array branches to the first because every `Array<X>::class == kotlin.Array`. An `Array<Int>` parameter
 * was therefore rebuilt as `String[]`, which fails (or silently misbehaves) when the target method is invoked
 * reflectively expecting `Integer[]`.
 *
 * These tests assert the rebuilt argument's runtime component type matches the original element type.
 *
 * @author K
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HashBatchCacheableArrayDispatchTest {

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
        provider = BatchConfigProvider().apply { register(CACHE, active = true, writeInTime = true) }
        HashCacheKit.overrideForTesting(
            manager = buildManager(CACHE to hashCache),
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

    /** Array<Int> param: the rebuilt arg passed to proceed must be Integer[], not String[]. */
    @Test
    fun arrayIntParam_rebuiltAsIntegerArray() {
        hashCache.reset()
        val target = BatchOps()
        val pjp = CapturingJoinPoint(methodOf("byIntArray"), arrayOf(arrayOf(1, 2)), target)
        val result = aspect.around(pjp)

        val passed = pjp.capturedArgs!![0]
        assertTrue(passed is Array<*>, "rebuilt arg must be an array")
        assertEquals(Integer::class.java, passed.javaClass.componentType,
            "Array<Int> must be rebuilt as Integer[] (not String[])")
        assertEquals(setOf("1", "2"), result.keys)
        assertEquals(setOf(1, 2), target.lastInts!!.toSet())
    }

    /** Array<Long> param: the rebuilt arg must be Long[]. */
    @Test
    fun arrayLongParam_rebuiltAsLongArray() {
        hashCache.reset()
        val target = BatchOps()
        val pjp = CapturingJoinPoint(methodOf("byLongArray"), arrayOf(arrayOf(7L, 8L)), target)
        val result = aspect.around(pjp)

        val passed = pjp.capturedArgs!![0]
        assertEquals(java.lang.Long::class.java, passed!!.javaClass.componentType,
            "Array<Long> must be rebuilt as Long[] (not String[])")
        assertEquals(setOf("7", "8"), result.keys)
        assertEquals(setOf(7L, 8L), target.lastLongs!!.toSet())
    }

    /** Array<String> param keeps working (no regression). */
    @Test
    fun arrayStringParam_stillWorks() {
        hashCache.reset()
        val target = BatchOps()
        val pjp = CapturingJoinPoint(methodOf("byStringArray"), arrayOf(arrayOf("a", "b")), target)
        val result = aspect.around(pjp)

        val passed = pjp.capturedArgs!![0]
        assertEquals(String::class.java, passed!!.javaClass.componentType)
        assertEquals(setOf("a", "b"), result.keys)
    }

    // ---- helpers ----

    private fun methodOf(name: String): Method = BatchOps::class.java.declaredMethods.first { it.name == name }

    data class E(override val id: String) : IIdEntity<String>

    @Suppress("unused", "UNUSED_PARAMETER")
    internal class BatchOps {
        var lastInts: List<Int>? = null
        var lastLongs: List<Long>? = null

        @HashBatchCacheableByPrimary(cacheNames = [CACHE], entityClass = E::class)
        fun byIntArray(ids: Array<Int>): Map<String, E?> {
            lastInts = ids.toList()
            return ids.associate { it.toString() to E(it.toString()) }
        }

        @HashBatchCacheableByPrimary(cacheNames = [CACHE], entityClass = E::class)
        fun byLongArray(ids: Array<Long>): Map<String, E?> {
            lastLongs = ids.toList()
            return ids.associate { it.toString() to E(it.toString()) }
        }

        @HashBatchCacheableByPrimary(cacheNames = [CACHE], entityClass = E::class)
        fun byStringArray(ids: Array<String>): Map<String, E?> =
            ids.associate { it to E(it) }
    }

    /**
     * A [ProceedingJoinPoint] that captures the args passed to `proceed(args)` and actually invokes the real target
     * method reflectively (reflective invoke surfaces a wrong array component type as `argument type mismatch`).
     */
    private class CapturingJoinPoint(
        private val method: Method,
        private val argArray: Array<Any?>,
        private val targetObj: Any
    ) : ProceedingJoinPoint {
        var capturedArgs: Array<out Any?>? = null
            private set

        override fun proceed(): Any? = invoke(argArray)
        override fun proceed(args: Array<out Any?>?): Any? {
            capturedArgs = args
            return invoke(args ?: argArray)
        }

        private fun invoke(args: Array<out Any?>): Any? {
            method.isAccessible = true
            return method.invoke(targetObj, *args)
        }

        override fun `set$AroundClosure`(arc: AroundClosure?) {}
        override fun getSignature(): Signature = Sig(method)
        override fun getArgs(): Array<Any?> = argArray
        override fun getTarget(): Any = targetObj
        override fun getThis(): Any = targetObj
        override fun getKind(): String = "method-execution"
        override fun getSourceLocation(): SourceLocation = throw UnsupportedOperationException()
        override fun getStaticPart(): JoinPoint.StaticPart = throw UnsupportedOperationException()
        override fun toShortString(): String = method.name
        override fun toLongString(): String = method.toGenericString()
    }

    private class Sig(private val method: Method) : MethodSignature {
        override fun getMethod(): Method = method
        override fun getReturnType(): Class<*> = method.returnType
        override fun getName(): String = method.name
        override fun getModifiers(): Int = method.modifiers
        override fun getDeclaringType(): Class<*> = method.declaringClass
        override fun getDeclaringTypeName(): String = method.declaringClass.name
        override fun getParameterTypes(): Array<Class<*>> = method.parameterTypes
        override fun getParameterNames(): Array<String> = emptyArray()
        override fun getExceptionTypes(): Array<Class<*>> = method.exceptionTypes
        override fun toShortString(): String = method.name
        override fun toLongString(): String = method.toGenericString()
        override fun toString(): String = method.toString()
    }

    /** In-memory hash cache (empty -> all keys miss -> the method is invoked with the rebuilt args). */
    private class MemHashCache : IHashCache {
        private val entities = linkedMapOf<Any?, IIdEntity<*>>()
        fun reset() { entities.clear() }

        @Suppress("UNCHECKED_CAST")
        override fun <E : IIdEntity<*>> findByIds(cacheName: String, ids: Collection<*>, entityClass: KClass<E>): List<E> =
            ids.mapNotNull { entities[it] } as List<E>
        override fun <PK, E : IIdEntity<PK>> saveBatch(cacheName: String, entities: List<E>, filterableProperties: Set<String>, sortableProperties: Set<String>) {
            entities.forEach { this.entities[it.id] = it }
        }
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
        private const val CACHE = "HASH_BATCH_ARRAY_DISPATCH_TEST"
    }
}
