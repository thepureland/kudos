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
 * 混合 Hash 缓存（两级：本地 Caffeine + 远程 Redis）测试用例。
 * 通过 [HashCacheKit.getHashCache] 获取 "testHash" 缓存（LOCAL_REMOTE 策略），验证读写经 mix 时本地与远程一致。
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
    /** 专用于 TestRowWithTime，与 testHash 分离避免与 TestRow 混存导致 ClassCastException */
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
            assertSame(found, foundAgain, "LOCAL_REMOTE 下同一 id 再次从缓存获取应返回同一对象引用")

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
        assertSame(fromCache, fromCacheAgain, "LOCAL_REMOTE 下同一 id 再次从缓存获取应返回同一对象引用")
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
        assertSame(byType2.first(), byType2Again.first(), "LOCAL_REMOTE 下同一维度再次从缓存获取应返回同一对象引用")
    }

    // ---------- 通过 Service 模拟二级索引/排序/分页，与缓存结果对比 ----------

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
     * 验证从远端回填本地时副属性索引不丢失。
     * 旧逻辑：所有读路径回填都传 emptySet()，本地永远没有 Set/ZSet 索引，按副属性查询永远 miss 本地、反复打远端。
     */
    @Test
    fun backfillFromRemoteRebuildsSecondaryIndex() {
        val cache = HashCacheKit.getHashCache(cacheName)
        // 1. 通过 mixCache 写入带索引的数据；MixHashCache 会记录 filterable=["type"]
        cache.save(cacheName, TestRow(id = "br1", name = "BR1", type = 7), setOf("type"), emptySet())
        cache.save(cacheName, TestRow(id = "br2", name = "BR2", type = 7), setOf("type"), emptySet())

        // 2. 直接清空本地 caffeine（模拟本地缓存被驱逐或本节点冷启动），远程仍然有数据与索引
        val realName = versionConfig.getFinalCacheName(cacheName)
        caffeineHashCache.clear(realName)

        // 3. mixCache.getById 触发回填本地（用 indexedFilterable=["type"] 重建索引）
        val backfilled = cache.getById(cacheName, "br1", TestRow::class)
        assertNotNull(backfilled)

        // 4. 直接走本地 caffeine 按副属性查询：旧 bug 下本地没有索引应当返回空；修复后能查到回填的 br1
        val byTypeLocal = caffeineHashCache.listBySetIndex(realName, TestRow::class, "type", 7)
        assertTrue(byTypeLocal.any { it.id == "br1" }, "回填后本地应能按 type 索引找到 br1")
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
