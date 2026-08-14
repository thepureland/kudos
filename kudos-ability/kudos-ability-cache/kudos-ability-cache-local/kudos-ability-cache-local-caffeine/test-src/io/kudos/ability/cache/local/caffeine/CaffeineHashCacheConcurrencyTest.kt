package io.kudos.ability.cache.local.caffeine

import io.kudos.base.model.contract.entity.IIdEntity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Concurrency and TTL behaviour of [CaffeineHashCache].
 *
 * The secondary indexes are plain maps updated alongside the Caffeine main store, so keeping them consistent
 * is this class's own responsibility rather than something Caffeine provides. These tests drive the paths that
 * used to corrupt them.
 *
 * @author K
 * @since 1.0.0
 */
internal class CaffeineHashCacheConcurrencyTest {

    private data class Row(override val id: String, val type: String) : IIdEntity<String>

    private val cacheName = "concurrent"
    private val filterable = setOf("type")

    // ---- 并发写入不得丢失索引项 -----------------------------------------------

    @Test
    fun concurrentSavesOfDistinctIds_allRemainQueryableBySecondaryIndex() {
        // 旧实现里 removeFromIndexes 用 `entries.removeIf { ids.remove(id); ids.isEmpty() }`：
        // 谓词执行期间不持锁，另一线程刚 getOrPut 重建并加入的 id 会随整个桶一起被删掉 ——
        // 实体还在主存里，却从二级索引消失，listBySetIndex 再也查不到它。
        val cache = CaffeineHashCache(maximumSize = 10_000)
        val threads = 8
        val perThread = 60
        val pool = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>(null)

        repeat(threads) { t ->
            pool.submit {
                try {
                    start.await()
                    repeat(perThread) { i ->
                        // 所有实体共用同一个 type，因而落在同一个索引桶上 —— 争用最激烈的形态
                        cache.save(cacheName, Row("id-$t-$i", "shared"), filterable, emptySet())
                    }
                } catch (t: Throwable) {
                    failure.compareAndSet(null, t)
                }
            }
        }
        start.countDown()
        pool.shutdown()
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "并发写入未在预期时间内完成")
        assertNull(failure.get(), "并发写入不应抛异常: ${failure.get()}")

        val found = cache.listBySetIndex(cacheName, Row::class, "type", "shared")
        assertEquals(
            threads * perThread, found.size,
            "每个成功写入的实体都必须能通过二级索引查到；缺失说明并发下索引项被丢弃了"
        )
    }

    @Test
    fun concurrentSavesOfSameId_leaveExactlyOneIndexEntry() {
        // 同一 id 的并发写会交错成 remove(A) / remove(B) / add(B) / add(A)，
        // 让实体同时挂在新旧两个属性值下 —— 按旧值查询仍能查到它。
        val cache = CaffeineHashCache(maximumSize = 10_000)
        val pool = Executors.newFixedThreadPool(4)
        val start = CountDownLatch(1)

        repeat(4) { t ->
            pool.submit {
                start.await()
                repeat(80) { cache.save(cacheName, Row("same", "type-$t"), filterable, emptySet()) }
            }
        }
        start.countDown()
        pool.shutdown()
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS))

        val current = cache.getById(cacheName, "same", Row::class)
        assertNotNull(current)
        val matchingCurrent = cache.listBySetIndex(cacheName, Row::class, "type", current.type)
        assertEquals(listOf("same"), matchingCurrent.map { it.id }, "应能按当前属性值查到")

        val staleHits = (0 until 4)
            .map { "type-$it" }
            .filter { it != current.type }
            .flatMap { cache.listBySetIndex(cacheName, Row::class, "type", it) }
        assertTrue(
            staleHits.isEmpty(),
            "不应残留指向旧属性值的索引项，实际按旧值仍查到: ${staleHits.map { it.id }}"
        )
    }

    // ---- clearLocal 不得让并发写入凭空消失 -------------------------------------

    @Test
    fun clearLocalDoesNotDetachTheStoreFromConcurrentWriters() {
        // 旧实现 `mainData.remove(cacheName)` 会把实例摘掉：此刻已持有旧引用的写线程继续往一个
        // 没人再读的对象里写，随后 main() 重建空实例，这些写入无声消失。
        // 现在原地清空，clear 之后写入的数据必须可见。
        val cache = CaffeineHashCache(maximumSize = 10_000)
        cache.save(cacheName, Row("before", "t"), filterable, emptySet())

        cache.clearLocal(cacheName)
        cache.save(cacheName, Row("after", "t"), filterable, emptySet())

        assertNull(cache.getById(cacheName, "before", Row::class), "clear 应清掉既有数据")
        assertNotNull(cache.getById(cacheName, "after", Row::class), "clear 之后的写入必须可见")
        assertEquals(
            listOf("after"), cache.listBySetIndex(cacheName, Row::class, "type", "t").map { it.id },
            "二级索引应与主存一致"
        )
    }

    // ---- TTL ------------------------------------------------------------------

    @Test
    fun configuredTtlExpiresEntries() {
        // CacheConfig.ttl 此前对 hash=true 的缓存项完全不起作用：只有容量上限，没有过期。
        // 一旦某次失效广播丢失（pub/sub 是 fire-and-forget），脏数据会永久驻留。
        val cache = CaffeineHashCache(maximumSize = 10_000) { 1 } // 1 second
        cache.save(cacheName, Row("ttl", "t"), filterable, emptySet())
        assertNotNull(cache.getById(cacheName, "ttl", Row::class), "刚写入应能读到")

        Thread.sleep(1_200)

        assertNull(cache.getById(cacheName, "ttl", Row::class), "超过 ttl 后条目应已过期")
    }

    @Test
    fun noTtlConfigured_entriesSurvive() {
        val cache = CaffeineHashCache(maximumSize = 10_000) { null }
        cache.save(cacheName, Row("keep", "t"), filterable, emptySet())
        Thread.sleep(200)
        assertNotNull(cache.getById(cacheName, "keep", Row::class), "未配置 ttl 时不应过期")
    }
}
