package io.kudos.base.support

import org.junit.jupiter.api.Assertions.assertEquals
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * test for Registry
 *
 * @since ChatGPT
 * @author K
 * @since 1.0.0
 */
internal class RegistryTest {

    @Test
    fun testLookup_onEmptyKey_returnsEmptyListAndDoesNotModifyRegistry() {
        // Use a key that has never been registered
        val key = "uniqueKey_empty"
        val list1 = Registry.lookup(key)
        // First lookup should be an empty list
        assertTrue(list1.isEmpty(), "lookup on a non-existent key should return an empty list")
        // Looking up again, internal map is still empty
        val list2 = Registry.lookup(key)
        assertTrue(list2.isEmpty(), "lookup on a non-existent key should not change Registry internal state")
    }

    @Test
    fun testRegister_singleObject_andNoDuplicate() {
        val key = "testKey_single"
        val obj = Any()

        // Initial lookup is still empty
        assertTrue(Registry.lookup(key).isEmpty(), "Initial lookup for a new key should be empty")

        // First register
        Registry.register(key, obj)
        val afterFirst = Registry.lookup(key)
        assertEquals(1, afterFirst.size, "After registering one object, lookup should return size = 1")
        assertTrue(afterFirst.contains(obj), "Lookup list should contain the just-registered obj")

        // Registering the same obj a second time should not add a duplicate
        Registry.register(key, obj)
        val afterSecond = Registry.lookup(key)
        assertEquals(1, afterSecond.size, "After registering the same object again, list size should still be 1, no duplicates")
        assertTrue(afterSecond.contains(obj), "Lookup list should still contain obj")
    }

    @Test
    fun testRegister_multipleObjects_andAllowDuplicatesInBulk() {
        val key = "testKey_bulk"
        val obj1 = "A"
        val obj2 = 123
        val obj3 = "A" // Same reference as obj1 or equals, but bulk register does not de-duplicate

        // Bulk register of an empty array should be a no-op
        Registry.register(key /*key*/, *emptyArray<Any>())
        assertTrue(Registry.lookup(key).isEmpty(), "Bulk register with an empty vararg should not change internal state")

        // First bulk register obj1, obj2
        Registry.register(key, obj1, obj2)
        val afterBulk1 = Registry.lookup(key)
        assertEquals(2, afterBulk1.size, "After bulk registering two objects, lookup size should be 2")
        assertTrue(afterBulk1.contains(obj1) && afterBulk1.contains(obj2),
            "Lookup list should contain obj1 and obj2")

        // Bulk register again with obj3 (equals obj1) and obj2 (same as before)
        Registry.register(key, obj3, obj2)
        val afterBulk2 = Registry.lookup(key)
        // The bulk method has no de-dup logic, so the list accumulates
        assertEquals(4, afterBulk2.size, "Bulk register does not de-duplicate; should accumulate to 4 elements")
        // Verify the inserted order
        // - index 0: obj1
        // - index 1: obj2
        // - index 2: obj3 (equals obj1)
        // - index 3: obj2
        assertEquals(listOf(obj1, obj2, obj3, obj2), afterBulk2,
            "After bulk register, list order matches the input order and duplicates are allowed")
    }

    // ============================================================
    // Note: Registry is an object singleton; tests below use unique key prefixes to avoid cross-pollution
    // ============================================================

    @Test
    fun testLookup_returnsSnapshotNotLiveView() {
        // KDoc notes "the returned list is a snapshot and does not expose internal mutable state" - pin this down
        val key = "snapshot_test_key"
        Registry.register(key, "x")
        val snapshot = Registry.lookup(key)
        // Even if cast and mutated, internal state must not be affected
        @Suppress("UNCHECKED_CAST")
        val mutated = runCatching { (snapshot as MutableList<Any>).add("y") }
        // The snapshot is implemented via .toList() returning an immutable list; write operations throw
        assertTrue(
            mutated.isFailure || Registry.lookup(key).size == 1,
            "lookup should return a snapshot: either immutable or not affecting internals"
        )
        // Looking up again should still only contain "x"
        assertEquals(listOf<Any>("x"), Registry.lookup(key))
    }

    @Test
    fun testDifferentKeysAreIsolated() {
        val keyA = "isolation_test_A"
        val keyB = "isolation_test_B"
        Registry.register(keyA, "alpha")
        Registry.register(keyB, "beta")
        assertEquals(listOf<Any>("alpha"), Registry.lookup(keyA))
        assertEquals(listOf<Any>("beta"), Registry.lookup(keyB))
        // Register for one key must not affect another key
        Registry.register(keyA, "alpha2")
        assertEquals(2, Registry.lookup(keyA).size)
        assertEquals(1, Registry.lookup(keyB).size, "keyB is not affected by changes to keyA")
    }

    @Test
    fun testSingleRegisterDedupesByEqualsNotByIdentity() {
        // String equals is content-based. Two new String("dup") have different refs but are equal,
        // single register should de-duplicate.
        val key = "equals_dedup_key"
        val a = String(charArrayOf('d', 'u', 'p'))
        val b = String(charArrayOf('d', 'u', 'p'))
        assertFalse(a === b, "Precondition: a and b have different references")
        assertEquals(a, b, "Precondition: a equals b")
        Registry.register(key, a)
        Registry.register(key, b)
        assertEquals(1, Registry.lookup(key).size, "Single register de-duplicates by equals")
    }

    @Test
    fun testConcurrentRegisterIsSafe() {
        // Smoke test: 8 threads x 100 registrations of distinct objects; must not drop data nor throw
        val key = "concurrent_register_key"
        val threadCount = 8
        val loops = 100
        val pool = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        repeat(threadCount) { tid ->
            pool.submit {
                repeat(loops) { i ->
                    Registry.register(key, "obj-$tid-$i")
                }
                latch.countDown()
            }
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Concurrent register should finish within 5s")
        pool.shutdown()
        // All objects are unique; expected size = threadCount * loops
        assertEquals(threadCount * loops, Registry.lookup(key).size)
    }

    @Test
    fun testLookup_afterMixOfSingleAndBulkRegistration() {
        val key = "testKey_mixed"
        val obj1 = "X"
        val obj2 = "Y"
        val obj3 = "X"  // equals obj1

        // Single register obj1
        Registry.register(key, obj1)
        val listAfterSingle = Registry.lookup(key)
        assertEquals(1, listAfterSingle.size, "After single register, size = 1")
        assertEquals(obj1, listAfterSingle[0], "First element should be obj1")

        // Bulk register obj2, obj3
        Registry.register(key, obj2, obj3)
        val listAfterBulk = Registry.lookup(key)
        // Bulk register does not check duplicates, so the list becomes [obj1, obj2, obj3]
        assertEquals(3, listAfterBulk.size, "After mixed register, size should be 3")
        assertEquals(listOf(obj1, obj2, obj3), listAfterBulk, "List order should be [obj1, obj2, obj3]")

        // Single register obj2 again (already in the list); single register de-duplicates
        Registry.register(key, obj2)
        val listAfterSingleAgain = Registry.lookup(key)
        // obj2 already exists, so size remains 3; no duplicate is added
        assertEquals(3, listAfterSingleAgain.size, "Single registering a duplicate object should de-duplicate, size unchanged")
        assertEquals(listAfterBulk, listAfterSingleAgain, "Order and content unchanged")
    }

}
