package io.kudos.test.rdb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure unit tests for [CacheTestResetSupport].
 *
 * These tests exercise the reflection-based helper logic directly (visibility was relaxed
 * to `internal` for testability) without requiring a Spring container, Redis, or the
 * business cache modules — none of which are on this module's classpath. The behaviors of
 * the public entry point and the redis-flush path are also verified in their "no such bean /
 * class not present" branches, which is exactly what happens in this module's test runtime
 * (the cache/redis classes referenced by name are not resolvable here).
 *
 * Coverage:
 * - [CacheTestResetSupport.resetRedisAndApplicationCaches]: early-return when no cache config provider bean
 * - [CacheTestResetSupport.flushRedis]: early-return when RedisTemplates bean is absent
 * - [CacheTestResetSupport.resetCaches] / clearCaches / reloadCaches: split + iterate with missing kit classes
 * - filterCacheNames: hash vs non-hash partitioning, null entries, non-string keys
 * - readHashFlag: null config, missing getter, true/false flags
 * - getBeanByClassName: unknown class, no application context
 * - invoke: success, missing method, arg-count mismatch, assignable params, null arg matching
 * - invokeGetter: getter naming convention (capitalisation), missing getter
 * - invokeStatic: Kotlin object singleton dispatch, unknown class no-op
 *
 * @author K
 * @since 1.0.0
 */
internal class CacheTestResetSupportTest {

    // region fakes ---------------------------------------------------------------------------

    /** Mimics a sys_cache config row exposing a JavaBean `getHash()` getter. */
    class FakeCacheConfig(private val hash: Boolean) {
        fun getHash(): Boolean = hash
    }

    /** Config object that has no `getHash` getter at all. */
    class NoGetterConfig

    /** Generic invoke target with a variety of method signatures. */
    @Suppress("unused")
    class FakeTarget {
        var lastArgs: List<Any?>? = null

        fun noArgs(): String = "noArgs"

        fun getName(): String = "fakeName"

        fun echo(value: String): String {
            lastArgs = listOf(value)
            return "echo:$value"
        }

        fun acceptsNumber(value: Number): String {
            lastArgs = listOf(value)
            return "num:$value"
        }

        fun acceptsAny(value: Any?): String {
            lastArgs = listOf(value)
            return "any:$value"
        }
    }

    /** A Kotlin `object` reachable via Class.forName + INSTANCE field, like the cache kits. */
    object FakeStaticSingleton {
        @JvmStatic
        var calls: MutableList<String> = mutableListOf()

        fun record(name: String) {
            calls.add(name)
        }
    }

    // endregion

    // region invoke --------------------------------------------------------------------------

    @Test
    fun invokeNoArgMethodReturnsValue() {
        val result = CacheTestResetSupport.invoke(FakeTarget(), "noArgs")
        assertEquals("noArgs", result, "no-arg method should be invoked and its value returned")
    }

    @Test
    fun invokeWithExactArgMatch() {
        val target = FakeTarget()
        val result = CacheTestResetSupport.invoke(target, "echo", "hi")
        assertEquals("echo:hi", result, "exact-type arg should match the method signature")
        assertEquals(listOf<Any?>("hi"), target.lastArgs)
    }

    @Test
    fun invokeWithAssignableArg() {
        // Integer is assignable to Number -> should match acceptsNumber(Number)
        val target = FakeTarget()
        val result = CacheTestResetSupport.invoke(target, "acceptsNumber", 42)
        assertEquals("num:42", result, "an assignable subtype arg should satisfy a supertype parameter")
    }

    @Test
    fun invokeWithNullArgMatchesAnyParameter() {
        // A null arg is typed as Any -> the zip rule `actual == Any::class.java` accepts any param type
        val target = FakeTarget()
        val result = CacheTestResetSupport.invoke(target, "acceptsAny", null)
        assertEquals("any:null", result, "null arg should match because actual type collapses to Any")
        assertEquals(listOf<Any?>(null), target.lastArgs)
    }

    @Test
    fun invokeReturnsNullWhenMethodNameMissing() {
        val result = CacheTestResetSupport.invoke(FakeTarget(), "doesNotExist")
        assertNull(result, "unknown method name should yield null, not throw")
    }

    @Test
    fun invokeReturnsNullWhenArgCountMismatch() {
        // echo expects exactly one arg; calling with zero args must not match
        val result = CacheTestResetSupport.invoke(FakeTarget(), "echo")
        assertNull(result, "arg-count mismatch should yield null")
    }

    @Test
    fun invokeReturnsNullWhenArgTypeIncompatible() {
        // acceptsNumber(Number) cannot accept a String arg
        val result = CacheTestResetSupport.invoke(FakeTarget(), "acceptsNumber", "not-a-number")
        assertNull(result, "incompatible (non-assignable, non-null) arg type should yield null")
    }

    // endregion

    // region invokeGetter --------------------------------------------------------------------

    @Test
    fun invokeGetterCapitalisesFirstLetter() {
        val result = CacheTestResetSupport.invokeGetter(FakeTarget(), "name")
        assertEquals("fakeName", result, "invokeGetter should derive getName() from property 'name'")
    }

    @Test
    fun invokeGetterReturnsNullWhenGetterMissing() {
        val result = CacheTestResetSupport.invokeGetter(FakeTarget(), "missing")
        assertNull(result, "missing getter should yield null")
    }

    // endregion

    // region invokeStatic --------------------------------------------------------------------

    @Test
    fun invokeStaticDispatchesToObjectSingleton() {
        FakeStaticSingleton.calls = mutableListOf()
        CacheTestResetSupport.invokeStatic(
            "io.kudos.test.rdb.CacheTestResetSupportTest\$FakeStaticSingleton",
            "record",
            "alpha"
        )
        assertEquals(listOf("alpha"), FakeStaticSingleton.calls, "invokeStatic should reach the object's INSTANCE method")
    }

    @Test
    fun invokeStaticIsNoOpForUnknownClass() {
        // Must not throw when the class cannot be resolved (the real cache kits are absent here)
        CacheTestResetSupport.invokeStatic("io.kudos.does.not.Exist", "doClear", "someCache")
    }

    // endregion

    // region readHashFlag --------------------------------------------------------------------

    @Test
    fun readHashFlagNullConfigIsFalse() {
        assertEquals(false, CacheTestResetSupport.readHashFlag(null), "null config should be treated as non-hash")
    }

    @Test
    fun readHashFlagTrue() {
        assertEquals(true, CacheTestResetSupport.readHashFlag(FakeCacheConfig(true)))
    }

    @Test
    fun readHashFlagFalse() {
        assertEquals(false, CacheTestResetSupport.readHashFlag(FakeCacheConfig(false)))
    }

    @Test
    fun readHashFlagMissingGetterDefaultsToFalse() {
        assertEquals(
            false,
            CacheTestResetSupport.readHashFlag(NoGetterConfig()),
            "config without getHash should default to false"
        )
    }

    // endregion

    // region filterCacheNames ----------------------------------------------------------------

    @Test
    fun filterCacheNamesPartitionsHashAndNonHash() = with(CacheTestResetSupport) {
        val configs = linkedMapOf<Any?, Any?>(
            "kv1" to FakeCacheConfig(false),
            "hash1" to FakeCacheConfig(true),
            "kv2" to FakeCacheConfig(false),
            "hash2" to FakeCacheConfig(true)
        )
        val kv = configs.filterCacheNames(expectHash = false)
        val hash = configs.filterCacheNames(expectHash = true)
        assertEquals(listOf("kv1", "kv2"), kv, "non-hash cache names")
        assertEquals(listOf("hash1", "hash2"), hash, "hash cache names")
    }

    @Test
    fun filterCacheNamesNullConfigCountsAsNonHash() = with(CacheTestResetSupport) {
        val configs = linkedMapOf<Any?, Any?>(
            "kv" to null
        )
        assertEquals(listOf("kv"), configs.filterCacheNames(expectHash = false))
        assertTrue(configs.filterCacheNames(expectHash = true).isEmpty())
    }

    @Test
    fun filterCacheNamesDropsNonStringKeys() = with(CacheTestResetSupport) {
        val configs = linkedMapOf<Any?, Any?>(
            123 to FakeCacheConfig(false),
            "validKv" to FakeCacheConfig(false)
        )
        // Both are non-hash, but the Int key must be dropped by mapNotNull { name as? String }
        assertEquals(listOf("validKv"), configs.filterCacheNames(expectHash = false))
    }

    // endregion

    // region resetCaches / clearCaches / reloadCaches ----------------------------------------

    @Test
    fun resetCachesSplitsAndIteratesWithoutThrowing() {
        // The real cache kit classes are not on this module's classpath, so each invokeStatic
        // becomes a no-op; the value here is exercising the split + iteration branches.
        val configs = linkedMapOf<Any?, Any?>(
            "kv1" to FakeCacheConfig(false),
            "hash1" to FakeCacheConfig(true)
        )
        CacheTestResetSupport.resetCaches(configs)
    }

    @Test
    fun resetCachesHandlesEmptyConfig() {
        CacheTestResetSupport.resetCaches(emptyMap<Any?, Any?>())
    }

    @Test
    fun clearCachesIteratesAllNames() {
        // Unknown kit class -> no-op per name, but the forEach over names must run.
        CacheTestResetSupport.clearCaches("io.kudos.does.not.Exist", listOf("a", "b", "c"))
    }

    @Test
    fun reloadCachesIteratesAllNames() {
        CacheTestResetSupport.reloadCaches("io.kudos.does.not.Exist", listOf("a", "b"))
    }

    @Test
    fun clearAndReloadHandleEmptyNameList() {
        CacheTestResetSupport.clearCaches("io.kudos.does.not.Exist", emptyList())
        CacheTestResetSupport.reloadCaches("io.kudos.does.not.Exist", emptyList())
    }

    // endregion

    // region getBeanByClassName --------------------------------------------------------------

    @Test
    fun getBeanByClassNameReturnsNullForUnknownClass() {
        // Class.forName fails -> early null, never touches the application context.
        assertNull(CacheTestResetSupport.getBeanByClassName("io.kudos.totally.Missing"))
    }

    @Test
    fun getBeanByClassNameReturnsNullWhenNoApplicationContext() {
        // String is resolvable, but SpringKit.applicationContext is not initialised in this
        // pure-unit test JVM -> its throwing getter is swallowed by runCatching -> null.
        assertNull(CacheTestResetSupport.getBeanByClassName("java.lang.String"))
    }

    // endregion

    // region flushRedis / resetRedisAndApplicationCaches -------------------------------------

    @Test
    fun flushRedisReturnsEarlyWhenRedisTemplatesAbsent() {
        // RedisTemplates class is not on this module's classpath -> getBeanByClassName null -> early return.
        CacheTestResetSupport.flushRedis()
    }

    @Test
    fun resetRedisAndApplicationCachesReturnsEarlyWithoutCacheProvider() {
        // No Spring context + cache provider class absent -> public entry point must be a safe no-op.
        CacheTestResetSupport.resetRedisAndApplicationCaches()
    }

    // endregion
}
