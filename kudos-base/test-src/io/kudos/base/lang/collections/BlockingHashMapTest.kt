package io.kudos.base.lang.collections

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * test for BlockingHashMap
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
internal class BlockingHashMapTest {

    private lateinit var map: BlockingHashMap<String, String>

    @BeforeTest
    fun setUp() {
        map = BlockingHashMap()
    }

    @Test
    fun putThenTakeSingleValue() = runBlocking {
        // 单生产单消费
        launch {
            delay(50) // 确保 put 在 take 之前一小会儿发生
            map.put("key1", "hello")
        }

        val result = map.take("key1")
        assertEquals("hello", result)
    }

    @Test
    fun takeBlocksUntilPut() = runBlocking {
        val job = launch {
            // 先启动一个 take，会挂起，因为当前尚未调用 put
            val value = map.take("blockKey")
            assertEquals("world", value)
        }

        // 确保 take 已经挂起
        delay(100)

        // 然后再 put
        map.put("blockKey", "world")
        job.join()
    }

    @Test
    fun pollTimesOutReturnsNull() = runBlocking {
        val start = System.currentTimeMillis()
        val value = map.poll("neverPut", 200)
        val elapsed = System.currentTimeMillis() - start

        assertNull(value)
        // 大致超时在 200ms 左右
        assertTrue(elapsed >= 180, "Expected to wait ~200ms, but waited only $elapsed ms")
    }

    @Test
    fun pollReturnsValueIfPutBeforeTimeout() = runBlocking {
        launch {
            delay(50)
            map.put("pKey", "pVal")
        }
        val result = map.poll("pKey", 500)
        assertEquals("pVal", result)
    }

    @Test
    fun pollNegativeTimeoutThrows() {
        // 负数超时应抛 IllegalArgumentException
        runBlocking {
            assertFailsWith<IllegalArgumentException> {
                map.poll("any", -10)
            }
        }
    }

    @Test
    fun multipleWaitersEachGetOneValue() = runBlocking {
        // 同一个 key 上有两个协程同时 take()，我们发送两次 put，
        // 应该让这两个 take 分别各自拿到一次“不同的”值。
        val results = mutableListOf<String>()
        val lock = Any()

        val waiter1 = launch {
            val x = map.take("multiKey")
            synchronized(lock) { results.add(x) }
        }
        val waiter2 = launch {
            val x = map.take("multiKey")
            synchronized(lock) { results.add(x) }
        }

        // 等待两者都挂起
        delay(100)

        // 先后两次 put，不同值
        map.put("multiKey", "one")
        map.put("multiKey", "two")

        // 等待两个 take 都返回
        waiter1.join()
        waiter2.join()

        // 结果里应当包含 "one" 和 "two"，且各自只被消费一次
        assertEquals(2, results.size)
        assertTrue(results.containsAll(listOf("one", "two")))
    }

    @Test
    fun putAfterMultiplePollers() = runBlocking {
        // 对 poll 也要支持相同的逻辑：如果超时足够大，第二个 poll 也能拿到第二个 put
        val r = mutableListOf<String>()
        val mtx = Any()

        val p1 = launch {
            val x = map.poll("X", 1000)   // 应当一直等到 put
            synchronized(mtx) { r.add(x!!) }
        }
        val p2 = launch {
            val x = map.poll("X", 1000)
            synchronized(mtx) { r.add(x!!) }
        }

        delay(50)
        map.put("X", "A")
        map.put("X", "B")

        p1.join()
        p2.join()
        assertEquals(2, r.size)
        assertTrue(r.containsAll(listOf("A", "B")))
    }

    @Test
    fun takeAndPollInterleave() = runBlocking {
        // 当一个协程在 take，另一个在 poll 并带超时，两者都能正确接收
        val cResults = mutableListOf<String>()
        val m = Any()

        val t1 = launch {
            val x = map.take("Z")
            synchronized(m) { cResults.add("T1:$x") }
        }
        val t2 = launch {
            val y = map.poll("Z", 500)
            synchronized(m) { cResults.add("P1:$y") }
        }

        delay(50)
        map.put("Z", "foo")
        // 由于有一个 take，会先消费 "foo"。那么 poll 会等第二次 put
        delay(50)
        map.put("Z", "bar")

        t1.join()
        t2.join()

        // 最终顺序不一定是具体先哪个协程先拿到值，但要保证两条都被收到了
        assertEquals(2, cResults.size)
        assertTrue(cResults.containsAll(listOf("T1:foo", "P1:bar")))
    }

}