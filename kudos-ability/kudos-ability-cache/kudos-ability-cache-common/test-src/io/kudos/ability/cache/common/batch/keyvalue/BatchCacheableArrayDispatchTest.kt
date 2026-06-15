package io.kudos.ability.cache.common.batch.keyvalue

import io.kudos.ability.cache.common.core.keyvalue.MixCache
import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.context.kit.SpringKit
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.Signature
import org.aspectj.lang.reflect.MethodSignature
import org.aspectj.lang.reflect.SourceLocation
import org.aspectj.runtime.internal.AroundClosure
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.cache.Cache
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.context.support.StaticApplicationContext
import java.lang.reflect.Method
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for the batch-cache aspect array element-type dispatch (bug C2).
 *
 * Before the fix, the aspect dispatched on `when (clazz.kotlin) { Array<String>::class -> ...; Array<Int>::class -> ... }`.
 * Because of Kotlin/JVM type erasure, every `Array<X>::class` equals `kotlin.Array`, so only the first Array branch
 * (`Array<String>`) ever matched. A method whose param is `Array<Int>` / `Array<Long>` / ... was therefore rebuilt as a
 * `String[]`, which fails with `argument type mismatch` when the target method is invoked reflectively (or silently
 * misbehaves).
 *
 * These tests assert that the rebuilt argument is an array whose **runtime component type** matches the original element
 * type, for Array<Int>/Array<Long>/Array<BigDecimal> (not just Array<String>).
 *
 * @author K
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BatchCacheableArrayDispatchTest {

    private val aspect = BatchCacheableAspect()
    private lateinit var cacheManager: TestMixCacheManager
    private lateinit var provider: TestProvider
    private lateinit var ctx: StaticApplicationContext

    @BeforeAll
    fun classSetup() {
        ctx = StaticApplicationContext().apply { refresh() }
        ctx.beanFactory.registerSingleton("defaultKeysGenerator", DefaultKeysGenerator())
        SpringKit.applicationContext = ctx

        cacheManager = TestMixCacheManager()
        provider = TestProvider().apply { register(CACHE, active = true) }
        KeyValueCacheKit.overrideForTesting(cacheManager, provider)
    }

    @BeforeEach
    fun perTestSetup() {
        // All test methods share one cache name; the stringified ids (e.g. "1","2") collide across
        // the ints/longs/decimals/strings methods. Clear the cache so each test starts fresh and
        // every element is a genuine miss (otherwise a prior method's value would be served).
        cacheManager.getCache(CACHE).clear()
    }

    @AfterAll
    fun classTeardown() {
        KeyValueCacheKit.resetForTesting()
        ctx.close()
    }

    /** Directly unit-tests the dispatch helper: the produced array must carry the real element component type. */
    @Test
    fun toTypedArray_buildsArrayWithCorrectRuntimeComponentType() {
        val ints = BatchCacheableAspect.toTypedArray(listOf(1, 2, 3), Int::class)
        assertTrue(ints is Array<*>)
        assertEquals(Integer::class.java, ints.javaClass.componentType)
        assertEquals(listOf(1, 2, 3), (ints as Array<*>).toList())

        val longs = BatchCacheableAspect.toTypedArray(listOf(10L, 20L), Long::class)
        assertEquals(java.lang.Long::class.java, longs.javaClass.componentType)

        val decimals = BatchCacheableAspect.toTypedArray(listOf(BigDecimal("1.5")), BigDecimal::class)
        assertEquals(BigDecimal::class.java, decimals.javaClass.componentType)

        val strings = BatchCacheableAspect.toTypedArray(listOf("a", "b"), String::class)
        assertEquals(String::class.java, strings.javaClass.componentType)
    }

    /** Array<Int> param: rebuilt arg must be Integer[], and reflective invoke must succeed (no argument-type-mismatch). */
    @Test
    fun arrayIntParam_rebuiltAsIntegerArray_andInvokesRealMethod() {
        val target = ArrayOps()
        val pjp = CapturingJoinPoint(methodOf("ints"), arrayOf(arrayOf(1, 2, 3)), target)
        val result = aspect.around(pjp)

        val passed = pjp.capturedArgs!![0]
        assertTrue(passed is Array<*>, "rebuilt arg must be an array")
        assertEquals(Integer::class.java, passed.javaClass.componentType,
            "Array<Int> must be rebuilt as Integer[] (not String[])")
        // The real method actually ran (reflective invoke through proceed succeeded) and returned its echo map.
        assertEquals(mapOf("1" to "i1", "2" to "i2", "3" to "i3"), result)
        assertEquals(setOf(1, 2, 3), target.lastInts!!.toSet())
    }

    /** Array<Long> param: rebuilt arg must be Long[]. */
    @Test
    fun arrayLongParam_rebuiltAsLongArray() {
        val target = ArrayOps()
        val pjp = CapturingJoinPoint(methodOf("longs"), arrayOf(arrayOf(5L, 6L)), target)
        val result = aspect.around(pjp)

        val passed = pjp.capturedArgs!![0]
        assertEquals(java.lang.Long::class.java, passed!!.javaClass.componentType,
            "Array<Long> must be rebuilt as Long[] (not String[])")
        assertEquals(mapOf("5" to "l5", "6" to "l6"), result)
        assertEquals(setOf(5L, 6L), target.lastLongs!!.toSet())
    }

    /** Array<BigDecimal> param: rebuilt arg must be BigDecimal[]. */
    @Test
    fun arrayBigDecimalParam_rebuiltAsBigDecimalArray() {
        val target = ArrayOps()
        val pjp = CapturingJoinPoint(methodOf("decimals"), arrayOf(arrayOf(BigDecimal("1"), BigDecimal("2"))), target)
        aspect.around(pjp)

        val passed = pjp.capturedArgs!![0]
        assertEquals(BigDecimal::class.java, passed!!.javaClass.componentType,
            "Array<BigDecimal> must be rebuilt as BigDecimal[]")
    }

    /** Array<String> param keeps working (no regression). */
    @Test
    fun arrayStringParam_stillWorks() {
        val target = ArrayOps()
        val pjp = CapturingJoinPoint(methodOf("strings"), arrayOf(arrayOf("a", "b")), target)
        val result = aspect.around(pjp)

        val passed = pjp.capturedArgs!![0]
        assertEquals(String::class.java, passed!!.javaClass.componentType)
        assertEquals(mapOf("a" to "sa", "b" to "sb"), result)
    }

    // ---- helpers ----

    private fun methodOf(name: String): Method = ArrayOps::class.java.declaredMethods.first { it.name == name }

    @Suppress("unused", "UNUSED_PARAMETER")
    internal class ArrayOps {
        var lastInts: List<Int>? = null
        var lastLongs: List<Long>? = null

        @BatchCacheable(cacheNames = [CACHE], valueClass = String::class)
        fun ints(ids: Array<Int>): Map<String, String> {
            lastInts = ids.toList()
            return ids.associate { it.toString() to "i$it" }
        }

        @BatchCacheable(cacheNames = [CACHE], valueClass = String::class)
        fun longs(ids: Array<Long>): Map<String, String> {
            lastLongs = ids.toList()
            return ids.associate { it.toString() to "l$it" }
        }

        @BatchCacheable(cacheNames = [CACHE], valueClass = String::class)
        fun decimals(ids: Array<BigDecimal>): Map<String, String> =
            ids.associate { it.toPlainString() to "d$it" }

        @BatchCacheable(cacheNames = [CACHE], valueClass = String::class)
        fun strings(ids: Array<String>): Map<String, String> =
            ids.associate { it to "s$it" }
    }

    /**
     * A [ProceedingJoinPoint] that captures the args passed to `proceed(args)` and then **actually** invokes the real
     * target method reflectively. Reflective invoke is what surfaces the bug: a wrong array component type throws
     * `IllegalArgumentException: argument type mismatch`.
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

    class TestMixCacheManager : MixCacheManager() {
        private val under = ConcurrentHashMap<String, Cache>()
        private val wrappers = ConcurrentHashMap<String, MixCache>()
        fun underlying(name: String): Cache = under.computeIfAbsent(name) { ConcurrentMapCache(it, true) }
        override fun getCache(name: String): Cache =
            wrappers.computeIfAbsent(name) { MixCache(CacheStrategy.REMOTE, null, underlying(it)) }
    }

    class TestProvider : ICacheConfigProvider {
        private val configs = ConcurrentHashMap<String, CacheConfig>()
        fun register(name: String, active: Boolean) {
            configs[name] = CacheConfig().apply { this.name = name; this.active = active }
        }
        override fun getCacheConfig(name: String): CacheConfig? = configs[name]
        override fun getAllCacheConfigs(): Map<String, CacheConfig> = configs
        override fun getLocalCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
    }

    companion object {
        private const val CACHE = "BATCH_ARRAY_DISPATCH_TEST"
    }
}
