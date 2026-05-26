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
        // single producer / single consumer
        launch {
            delay(50) // ensure put happens slightly after take
            map.put("key1", "hello")
        }

        val result = map.take("key1")
        assertEquals("hello", result)
    }

    @Test
    fun takeBlocksUntilPut() = runBlocking {
        val job = launch {
            // start a take first; it will suspend because put has not been called yet
            val value = map.take("blockKey")
            assertEquals("world", value)
        }

        // ensure take has suspended
        delay(100)

        // then put
        map.put("blockKey", "world")
        job.join()
    }

    @Test
    fun pollTimesOutReturnsNull() = runBlocking {
        val start = System.currentTimeMillis()
        val value = map.poll("neverPut", 200)
        val elapsed = System.currentTimeMillis() - start

        assertNull(value)
        // approximately 200ms timeout
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
        // a negative timeout should throw IllegalArgumentException
        runBlocking {
            assertFailsWith<IllegalArgumentException> {
                map.poll("any", -10)
            }
        }
    }

    @Test
    fun multipleWaitersEachGetOneValue() = runBlocking {
        // Two coroutines call take() on the same key concurrently; we send two puts.
        // Each take should receive a different value, exactly once.
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

        // wait for both to suspend
        delay(100)

        // put twice with different values
        map.put("multiKey", "one")
        map.put("multiKey", "two")

        // wait for both takes to return
        waiter1.join()
        waiter2.join()

        // results should contain "one" and "two", each consumed only once
        assertEquals(2, results.size)
        assertTrue(results.containsAll(listOf("one", "two")))
    }

    @Test
    fun putAfterMultiplePollers() = runBlocking {
        // poll should support the same semantics: with a large enough timeout, the second poll can also receive the second put
        val r = mutableListOf<String>()
        val mtx = Any()

        val p1 = launch {
            val x = map.poll("X", 1000)   // should wait for the put
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
        // When one coroutine is taking and another is polling with a timeout, both should receive correctly.
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
        // Because one take exists, it consumes "foo" first. poll then waits for the second put.
        delay(50)
        map.put("Z", "bar")

        t1.join()
        t2.join()

        // The order of which coroutine receives first is not guaranteed, but both messages must be received.
        assertEquals(2, cResults.size)
        assertTrue(cResults.containsAll(listOf("T1:foo", "P1:bar")))
    }

}
