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

/**
 * [io.kudos.ability.cache.common.batch.hash.HashBatchCacheable] 注解测试用例（本地+远程 LOCAL_REMOTE）。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnableKudosTest
@Import(LocalRemoteHashTestCacheConfigProvider::class, HashCacheableTestService::class)
@EnabledIfDockerInstalled
internal class LocalRemoteHashBatchCacheableTest {

    @Autowired
    private lateinit var hashCacheableTestService: HashCacheableTestService

    private val cacheName = "testHash"

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
        HashCacheKit.getHashCache(cacheName)?.refreshAll(cacheName, emptyList<TestRow>(), emptySet(), emptySet())
    }

    @Test
    fun hashBatchCacheableInLocalRemoteMode() {
        hashCacheableTestService.putTestData("ba1", TestRow(id = "ba1", name = "BatchA", type = 1))
        hashCacheableTestService.putTestData("ba2", TestRow(id = "ba2", name = "BatchB", type = 2))
        val first = hashCacheableTestService.getTestRowsByIds(listOf("ba1", "ba2"))
        assertEquals(2, first.size)
        assertEquals("BatchA", first["ba1"]?.name)
        assertEquals("BatchB", first["ba2"]?.name)
        hashCacheableTestService.removeTestData("ba1")
        hashCacheableTestService.removeTestData("ba2")
        val fromCache = hashCacheableTestService.getTestRowsByIds(listOf("ba1", "ba2"))
        assertEquals(2, fromCache.size)
        assertEquals("BatchA", fromCache["ba1"]?.name)
        assertEquals("BatchB", fromCache["ba2"]?.name)
    }
}
