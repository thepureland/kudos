package io.kudos.ability.cache.remote.redis.keyvalue

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.cache.get
import org.springframework.cache.interceptor.SimpleKey
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Regression test for [io.kudos.ability.cache.remote.redis.support.RedisCacheKeyConversionService].
 *
 * Guards against a Spring-Data-Redis default-`ConversionService` issue where single-arg
 * [SimpleKey] and the equivalent raw [String] would land on different Redis entries: Spring
 * Data Redis 4.x's `RedisCacheConfiguration#registerDefaultConverters` registers `SimpleKey ->
 * String` as `SimpleKey::toString`, producing `"SimpleKey [k]"` instead of the bare `"k"` that
 * raw-String access produces. Writing through one and reading through the other would then
 * silently miss.
 *
 * [io.kudos.ability.cache.remote.redis.support.RedisCacheKeyConversionService] (wired in
 * `RedisCacheAutoConfiguration` and propagated by `RedisKeyValueCacheManager.createCache`)
 * normalizes single-param `SimpleKey` to `String.valueOf(param)`, so both access paths land on
 * the same Redis key. These tests fail if that wiring is dropped or the conversion service is
 * accidentally replaced with the Spring default — both have happened in adjacent codebases.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest
@Import(TestCacheConfigProvider::class)
@EnabledIfDockerInstalled
internal class SimpleKeyKeyConversionBugTest {

    companion object {
        private const val CACHE_NAME = "test"

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.cache.enabled") { "true" }
            registry.add("kudos.cache.config.strategy") { CacheStrategy.REMOTE.name }
            RedisTestContainer.startIfNeeded(registry)
        }
    }

    @Autowired
    @Qualifier("remoteCacheManager")
    private lateinit var remoteCacheManager: CacheManager

    /**
     * Round-trip 1: write via SimpleKey, read via raw String.
     *
     * The raw String key `"k"` and `SimpleKey("k")` describe the *same logical key*. With Spring
     * Data Redis's default `SimpleKey::toString` mapping the two paths would diverge into
     * `"k"` and `"SimpleKey [k]"`; with our conversion service in place they collapse to `"k"`.
     */
    @Test
    fun simpleKeyWrite_rawStringRead_shouldHit() {
        val cache = remoteCacheManager.getCache(CACHE_NAME)!!
        val key = "k"
        val value = "written-via-SimpleKey"

        // Write via SimpleKey(key). With the conversion service in place the Redis key is `test::k`.
        cache.put(SimpleKey(key), value)

        // Read via raw String — also resolves to `test::k`, so the value is reachable.
        val viaRawString = cache.get<String>(key)

        assertEquals(
            value, viaRawString,
            "raw String \"$key\" lookup should hit what SimpleKey(\"$key\") wrote; if it returns " +
                "null, Spring's default `SimpleKey::toString` mapping has crept back in and the " +
                "SimpleKey write landed at \"SimpleKey [$key]\" instead.",
        )
    }

    /**
     * Round-trip 2: write via raw String, read via SimpleKey.
     *
     * Symmetric to round-trip 1 — guards against a partial regression that fixes only one
     * direction (e.g. SimpleKey side) while silently leaving the other broken.
     */
    @Test
    fun rawStringWrite_simpleKeyRead_shouldHit() {
        val cache = remoteCacheManager.getCache(CACHE_NAME)!!
        val key = "k2"
        val value = "written-via-raw-string"

        // Write via raw String — Redis key `test::k2`.
        cache.put(key, value)

        // Sanity: raw-String reader sees its own write (no encoding involved).
        assertEquals(value, cache.get<String>(key), "control: raw-String round-trip works")

        // Read via SimpleKey — also resolves to `test::k2`, so the value is reachable.
        val viaSimpleKey = cache.get<String>(SimpleKey(key))
        assertEquals(
            value, viaSimpleKey,
            "SimpleKey(\"$key\") lookup should hit the entry the raw-String writer just stored; " +
                "if it returns null, the SimpleKey path resolved to \"SimpleKey [$key]\" instead.",
        )

        // Diagnostic: no value is reachable under the literal stringified-SimpleKey form,
        // confirming the conversion service collapses both paths onto the same Redis key
        // rather than just adding a second copy.
        assertNull(
            cache.get<String>("SimpleKey [$key]"),
            "diagnostic: with the conversion service in place, nothing should land at the literal " +
                "\"SimpleKey [...]\" key; finding a value here would mean Spring's default mapping " +
                "is still in effect and writes are landing under two distinct keys.",
        )
    }
}
