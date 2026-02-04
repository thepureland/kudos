package io.kudos.ability.cache.remote.redis.hash

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary] 注解测试用例（远程 Redis）。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnableKudosTest
@Import(HashTestCacheConfigProvider::class, HashCacheableTestService::class)
@EnabledIfDockerInstalled
internal class HashBatchCacheableTest {

    @Autowired
    private lateinit var hashCacheableTestService: HashCacheableTestService

    private val cacheName = "testHash"

    companion object {
        @DynamicPropertySource
        @JvmStatic
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.cache.enabled") { "true" }
            registry.add("kudos.cache.config.strategy") { CacheStrategy.REMOTE.name }
            RedisTestContainer.startIfNeeded(registry)
        }
    }

    @BeforeEach
    fun clearCache() {
        HashCacheKit.getHashCache(cacheName)?.refreshAll(cacheName, emptyList<TestRow>(), emptySet(), emptySet())
    }

    @Test
    fun hashBatchCacheableMissThenHit() {
        hashCacheableTestService.putTestData("a", TestRow(id = "a", name = "Alice", type = 1))
        hashCacheableTestService.putTestData("b", TestRow(id = "b", name = "Bob", type = 2))
        hashCacheableTestService.putTestData("c", TestRow(id = "c", name = "Carol", type = 3))
        val first = hashCacheableTestService.getTestRowsByIds(listOf("a", "b", "c"))
        assertEquals(3, first.size)
        assertEquals("Alice", first["a"]?.name)
        assertEquals("Bob", first["b"]?.name)
        assertEquals("Carol", first["c"]?.name)
        hashCacheableTestService.removeTestData("a")
        hashCacheableTestService.removeTestData("b")
        hashCacheableTestService.removeTestData("c")
        val fromCache = hashCacheableTestService.getTestRowsByIds(listOf("a", "b", "c"))
        assertEquals(3, fromCache.size)
        assertEquals("Alice", fromCache["a"]?.name)
        assertEquals("Bob", fromCache["b"]?.name)
        assertEquals("Carol", fromCache["c"]?.name)
    }

    @Test
    fun hashBatchCacheablePartialHit() {
        hashCacheableTestService.putTestData("p1", TestRow(id = "p1", name = "P1", type = 1))
        hashCacheableTestService.putTestData("p2", TestRow(id = "p2", name = "P2", type = 2))
        val first = hashCacheableTestService.getTestRowsByIds(listOf("p1", "p2", "p3"))
        assertEquals(2, first.size)
        assertEquals("P1", first["p1"]?.name)
        assertEquals("P2", first["p2"]?.name)
        hashCacheableTestService.removeTestData("p1")
        hashCacheableTestService.removeTestData("p2")
        val fromCache = hashCacheableTestService.getTestRowsByIds(listOf("p1", "p2", "p3"))
        assertEquals(2, fromCache.size)
        assertEquals("P1", fromCache["p1"]?.name)
        assertEquals("P2", fromCache["p2"]?.name)
    }

    @Test
    fun hashBatchCacheableEmptyIds() {
        val result = hashCacheableTestService.getTestRowsByIds(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun hashBatchCacheableWritesSetIndex() {
        hashCacheableTestService.putTestData("s1", TestRow(id = "s1", name = "S1", type = 1))
        hashCacheableTestService.putTestData("s2", TestRow(id = "s2", name = "S2", type = 1))
        hashCacheableTestService.putTestData("s3", TestRow(id = "s3", name = "S3", type = 2))
        hashCacheableTestService.getTestRowsByIds(listOf("s1", "s2", "s3"))
        val cache = HashCacheKit.getHashCache(cacheName)!!
        val byType1 = cache.listBySetIndex(cacheName, TestRow::class, "type", 1)
        assertEquals(2, byType1.size)
        assertTrue(byType1.any { it.id == "s1" && it.name == "S1" })
        assertTrue(byType1.any { it.id == "s2" && it.name == "S2" })
        val byType2 = cache.listBySetIndex(cacheName, TestRow::class, "type", 2)
        assertEquals(1, byType2.size)
        assertEquals("s3", byType2.first().id)
    }
}
