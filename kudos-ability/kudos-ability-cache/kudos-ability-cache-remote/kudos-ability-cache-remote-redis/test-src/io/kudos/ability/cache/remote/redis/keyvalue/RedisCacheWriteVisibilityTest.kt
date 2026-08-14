package io.kudos.ability.cache.remote.redis.keyvalue

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.cache.get
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the remote cache to **synchronous, read-your-writes** semantics.
 *
 * Spring Data Redis 4.1 made cache writes asynchronous by default: `DefaultRedisCacheWriter.put` calls
 * `AsyncCacheWriter.store(...)` and returns without joining the future. `RedisCacheAutoConfiguration`
 * therefore builds the writer with `immediateWrites()`. If that opt-out is dropped — or a future upgrade
 * changes the default again — the stack silently loses two properties it depends on:
 *
 * 1. **Read-your-writes.** `put` followed by `get` may miss. This is not a theoretical concern: with
 *    asynchronous writes the key was still absent from Redis right after `put` returned in roughly 43% of
 *    iterations, which made [LocalRemoteCacheTest], [RemoteCacheTest] and [SimpleKeyKeyConversionBugTest]
 *    fail intermittently and unreproducibly. `@CachePut`'s contract (the value is cached once the annotated
 *    method returns) breaks the same way.
 * 2. **Write-failure propagation.** Nothing joins the future, so a failed remote write is dropped silently
 *    instead of surfacing through `MixCache.writeThrough`.
 *
 * The loop below is deliberately long: a single put/get pair only fails ~1% of the time under the
 * regression, whereas the "is the key in Redis the instant `put` returned" probe catches it almost every
 * iteration. Both signals are asserted so the test fails fast and says which property was lost.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest
@Import(TestCacheConfigProvider::class)
@EnabledIfDockerInstalled
internal class RedisCacheWriteVisibilityTest {

    companion object {
        private const val CACHE_NAME = "test"
        private const val ROUNDS = 200

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

    @Autowired
    private lateinit var redisTemplates: RedisTemplates

    @Autowired
    private lateinit var versionConfig: CacheVersionConfig

    @Test
    fun putIsVisibleAsSoonAsItReturns() {
        val cache = remoteCacheManager.getCache(CACHE_NAME)!!

        @Suppress("UNCHECKED_CAST")
        val template = redisTemplates.defaultRedisTemplate as RedisTemplate<String, Any>
        val keyPrefix = "${versionConfig.getFinalCacheName(CACHE_NAME)}::"

        var absentAfterPut = 0
        var readMisses = 0
        for (i in 0 until ROUNDS) {
            val key = "write_visibility_$i"
            val value = "v_$i"

            cache.put(key, value)

            // Probe Redis before touching the cache again: with asynchronous writes the SET is still in
            // flight here. This is the sensitive signal.
            if (template.hasKey("$keyPrefix$key") != true) absentAfterPut++
            if (cache.get<String>(key) != value) readMisses++
        }

        assertEquals(
            0, absentAfterPut,
            "$absentAfterPut/$ROUNDS writes had not reached Redis when Cache.put returned. The remote cache " +
                "writer is running in asynchronous mode — check that RedisCacheAutoConfiguration still builds " +
                "it with RedisCacheWriter.create(cf) { it.immediateWrites() }.",
        )
        assertEquals(
            0, readMisses,
            "$readMisses/$ROUNDS reads missed the value written immediately before them; the remote cache is " +
                "no longer read-your-writes consistent.",
        )
    }
}
