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
import java.util.concurrent.CountDownLatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 混合 Hash 缓存（两级：本地 Caffeine + 远程 Redis）测试用例。
 * 通过 [HashCacheKit.getHashCache] 获取 "testHash" 缓存（LOCAL_REMOTE 策略），验证读写经 mix 时本地与远程一致。
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@Import(LocalRemoteHashTestCacheConfigProvider::class, HashCacheableTestService::class)
@EnabledIfDockerInstalled
internal class LocalRemoteHashCacheTest {

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
    fun testLocalRemoteHashCache() {
        val mixCache = HashCacheKit.getHashCache(cacheName)
        assertNotNull(mixCache)

        val latch = CountDownLatch(1)
        Thread {
            val cache = HashCacheKit.getHashCache(cacheName)!!
            val key = "local_remote_hash_key"
            cache.save(cacheName, TestRow(id = key, name = "LocalRemote", type = 1))

            val found = cache.getById(cacheName, key, TestRow::class)
            assertNotNull(found)
            assertEquals(key, found?.id)
            assertEquals("LocalRemote", found?.name)
            assertEquals(1, found?.type)

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
    }

}
