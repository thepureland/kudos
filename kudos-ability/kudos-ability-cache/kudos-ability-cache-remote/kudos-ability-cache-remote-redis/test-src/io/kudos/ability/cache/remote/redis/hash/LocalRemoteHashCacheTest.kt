package io.kudos.ability.cache.remote.redis.hash

import io.kudos.ability.cache.common.core.hash.IHashCache
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.sort.Order
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.concurrent.CountDownLatch
import kotlin.test.*

/**
 * Mixed hash cache (two-level: local Caffeine + remote Redis) test cases.
 * Looks up the "testHash" cache via [HashCacheKit.getHashCache] (LOCAL_REMOTE strategy) and verifies that
 * reads/writes through mix keep local and remote consistent.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnableKudosTest
@Import(LocalRemoteHashTestCacheConfigProvider::class, HashCacheableTestService::class)
@EnabledIfDockerInstalled
internal class LocalRemoteHashCacheTest {

    @Autowired
    private lateinit var hashCacheableTestService: HashCacheableTestService

    @Autowired
    @Qualifier("caffeineIdEntitiesHashCache")
    private lateinit var caffeineHashCache: IHashCache

    @Autowired
    private lateinit var versionConfig: CacheVersionConfig

    private val cacheName = "testHash"
    /** Dedicated to TestRowWithTime; kept separate from testHash to avoid ClassCastException when mixing with TestRow. */
    private val cacheNameWithTime = "testHashWithTime"
    private val setIdx = setOf("type")
    private val zsetIdx = setOf("type", "sortScore")

    companion object {
        @DynamicPropertySource
        @JvmStatic
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.cache.enabled") { "true" }
            registry.add("kudos.cache.config.strategy") { CacheStrategy.LOCAL_REMOTE.name }
            RedisTestContainer.startIfNeeded(registry)
        }
    }

    @BeforeEach
    fun clearCache() {
        HashCacheKit.getHashCache(cacheName).refreshAll(cacheName, emptyList<TestRow>(), emptySet(), emptySet())
        HashCacheKit.getHashCache(cacheNameWithTime).refreshAll(cacheNameWithTime, emptyList<TestRowWithTime>(), emptySet(), emptySet())
        hashCacheableTestService.clearTestData()
        hashCacheableTestService.clearTestDataWithTime()
    }

    @Test
    fun testLocalRemoteHashCache() {
        val mixCache = HashCacheKit.getHashCache(cacheName)
        assertNotNull(mixCache)

        val latch = CountDownLatch(1)
        Thread {
            val cache = HashCacheKit.getHashCache(cacheName)
            val key = "local_remote_hash_key"
            cache.save(cacheName, TestRow(id = key, name = "LocalRemote", type = 1))

            val found = cache.getById(cacheName, key, TestRow::class)
            assertNotNull(found)
            assertEquals(key, found.id)
            assertEquals("LocalRemote", found.name)
            assertEquals(1, found.type)
            val foundAgain = cache.getById(cacheName, key, TestRow::class)
            assertSame(found, foundAgain, "Under LOCAL_REMOTE, fetching the same id from the cache again should return the same object reference")

            val all = cache.listAll(cacheName, TestRow::class)
            assertTrue(all.any { it.id == key && it.name == "LocalRemote" })

            latch.countDown()
        }.start()
        latch.await()
    }

    @Test
    fun hashCacheableInLocalRemoteMode() {
        hashCacheableTestService.putTestData("lr1", TestRow(id = "lr1", name = "LocalRemote", type = 1))
        val first = hashCacheableTestService.getTestRowById("lr1")
        assertEquals("lr1", first?.id)
        assertEquals("LocalRemote", first?.name)
        hashCacheableTestService.removeTestData("lr1")
        val fromCache = hashCacheableTestService.getTestRowById("lr1")
        assertEquals("LocalRemote", fromCache?.name)
        val fromCacheAgain = hashCacheableTestService.getTestRowById("lr1")
        assertSame(fromCache, fromCacheAgain, "Under LOCAL_REMOTE, fetching the same id from the cache again should return the same object reference")
    }

    @Test
    fun hashCacheableWritesSetIndexInLocalRemoteMode() {
        hashCacheableTestService.putTestData("lr2", TestRow(id = "lr2", name = "SetIdx", type = 2))
        hashCacheableTestService.getTestRowById("lr2")
        val cache = HashCacheKit.getHashCache(cacheName)
        val byType2 = cache.listBySetIndex(cacheName, TestRow::class, "type", 2)
        assertEquals(1, byType2.size)
        assertEquals("lr2", byType2.first().id)
        assertEquals("SetIdx", byType2.first().name)
        val byType2Again = cache.listBySetIndex(cacheName, TestRow::class, "type", 2)
        assertSame(byType2.first(), byType2Again.first(), "Under LOCAL_REMOTE, fetching the same dimension from the cache again should return the same object reference")
    }

    // ---------- Use the Service to mirror secondary index / order / paging and compare with cached results ----------

    @Test
    fun serviceListByTypeMatchesCacheSetIndexInLocalRemoteMode() {
        hashCacheableTestService.putTestData("w1", TestRow(id = "w1", name = "W1", type = 1))
        hashCacheableTestService.putTestData("w2", TestRow(id = "w2", name = "W2", type = 1))
        hashCacheableTestService.putTestData("w3", TestRow(id = "w3", name = "W3", type = 2))
        hashCacheableTestService.getTestRowById("w1")
        hashCacheableTestService.getTestRowById("w2")
        hashCacheableTestService.getTestRowById("w3")
        val cache = HashCacheKit.getHashCache(cacheName)
        val expectedByType1 = hashCacheableTestService.listTestRowsByType(1).map { it.id }.toSet()
        val actualByType1 = cache.listBySetIndex(cacheName, TestRow::class, "type", 1).map { it.id }.toSet()
        assertEquals(expectedByType1, actualByType1)
        assertEquals(2, actualByType1.size)
    }

    @Test
    fun serviceSortScorePageMatchesCacheZSetPageInLocalRemoteMode() {
        hashCacheableTestService.putTestDataWithTime("z1", TestRowWithTime(id = "z1", type = 0, sortScore = 10.0))
        hashCacheableTestService.putTestDataWithTime("z2", TestRowWithTime(id = "z2", type = 0, sortScore = 20.0))
        hashCacheableTestService.putTestDataWithTime("z3", TestRowWithTime(id = "z3", type = 0, sortScore = 30.0))
        val cache = HashCacheKit.getHashCache(cacheNameWithTime)
        cache.save(cacheNameWithTime, TestRowWithTime(id = "z1", type = 0, sortScore = 10.0), setIdx, zsetIdx)
        cache.save(cacheNameWithTime, TestRowWithTime(id = "z2", type = 0, sortScore = 20.0), setIdx, zsetIdx)
        cache.save(cacheNameWithTime, TestRowWithTime(id = "z3", type = 0, sortScore = 30.0), setIdx, zsetIdx)
        val expected = hashCacheableTestService.listTestRowsWithTimeBySortScorePage(0, 2, desc = true).map { it.id }
        val actual = cache.listPageByZSetIndex(cacheNameWithTime, TestRowWithTime::class, "sortScore", 0, 2, desc = true).map { it.id }
        assertEquals(expected, actual)
        assertEquals(listOf("z3", "z2"), actual)
    }

    /**
     * Verifies that secondary-property indexes are not lost when backfilling local from remote.
     * Old behavior: all read-path backfills passed emptySet(), so the local layer never built Set/ZSet
     * indexes, secondary-property queries always missed locally, and remote was hit repeatedly.
     */
    @Test
    fun backfillFromRemoteRebuildsSecondaryIndex() {
        val cache = HashCacheKit.getHashCache(cacheName)
        // 1. Write indexed data via mixCache; MixHashCache will record filterable=["type"].
        cache.save(cacheName, TestRow(id = "br1", name = "BR1", type = 7), setOf("type"), emptySet())
        cache.save(cacheName, TestRow(id = "br2", name = "BR2", type = 7), setOf("type"), emptySet())

        // 2. Clear local caffeine directly (simulating local eviction or a cold start for this node); remote still has data and indexes.
        val realName = versionConfig.getFinalCacheName(cacheName)
        caffeineHashCache.clear(realName)

        // 3. mixCache.getById triggers local backfill (rebuilds the index using indexedFilterable=["type"]).
        val backfilled = cache.getById(cacheName, "br1", TestRow::class)
        assertNotNull(backfilled)

        // 4. Query local caffeine directly by the secondary property: with the old bug the local layer
        //    had no index and would return empty; after the fix it should find the backfilled br1.
        val byTypeLocal = caffeineHashCache.listBySetIndex(realName, TestRow::class, "type", 7)
        assertTrue(byTypeLocal.any { it.id == "br1" }, "After backfill, local should find br1 via the type index")
    }

    @Test
    fun serviceListPageMatchesCacheListInLocalRemoteMode() {
        hashCacheableTestService.putTestDataWithTime("q1", TestRowWithTime(id = "q1", type = 1, sortScore = 100.0))
        hashCacheableTestService.putTestDataWithTime("q2", TestRowWithTime(id = "q2", type = 1, sortScore = 200.0))
        hashCacheableTestService.putTestDataWithTime("q3", TestRowWithTime(id = "q3", type = 2, sortScore = 150.0))
        val cache = HashCacheKit.getHashCache(cacheNameWithTime)
        cache.save(cacheNameWithTime, TestRowWithTime(id = "q1", type = 1, sortScore = 100.0), setIdx, zsetIdx)
        cache.save(cacheNameWithTime, TestRowWithTime(id = "q2", type = 1, sortScore = 200.0), setIdx, zsetIdx)
        cache.save(cacheNameWithTime, TestRowWithTime(id = "q3", type = 2, sortScore = 150.0), setIdx, zsetIdx)
        val criteria = Criteria.of("type", OperatorEnum.EQ, 1)
        val order = Order.desc("sortScore")
        val expected = hashCacheableTestService.listTestRowsWithTimePage(criteria, 1, 10, order).map { it.id }
        val actual = cache.list(cacheNameWithTime, TestRowWithTime::class, criteria, 1, 10, order).map { it.id }
        assertEquals(expected, actual)
        assertEquals(listOf("q2", "q1"), actual)
    }
}
