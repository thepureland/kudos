package io.kudos.ability.cache.interservice.aop

import io.kudos.ability.cache.interservice.common.ClientCacheItem
import io.kudos.ability.cache.interservice.common.RequestResult
import org.springframework.util.ConcurrentReferenceHashMap
import java.util.function.Function
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * test for ClientCacheUidGenerator
 *
 * Covers:
 * - cache disabled (default): uid is recomputed on every call and tracks object mutation
 * - cache enabled: uid is reused for an equality-stable key even after the JSON content changes
 * - cache enabled: equal-but-distinct instances yield the same uid value (equality-keyed cache)
 * - consistency with ClientCacheItem.genUid
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class ClientCacheUidGeneratorTest {

    @Test
    fun generate_cacheDisabledByDefault_recomputesAfterMutation() {
        val generator = ClientCacheUidGenerator()
        val obj = RequestResult(1, "one")

        val uid1 = generator.generate(obj)
        assertEquals(ClientCacheItem.genUid(obj), uid1)

        obj.msg = "two"
        val uid2 = generator.generate(obj)
        assertEquals(ClientCacheItem.genUid(obj), uid2)
        assertNotEquals(uid1, uid2)
    }

    @Test
    fun generate_cacheEnabled_reusesUidForEqualityStableKey() {
        val generator = ClientCacheUidGenerator(cacheEnabled = true)
        val obj = StableKeyPayload("one")

        val uid1 = generator.generate(obj)
        assertEquals(ClientCacheItem.genUid(obj), uid1)

        // The equality-keyed cache deliberately returns the stale uid after the content mutates,
        // because equals/hashCode of the key did not change.
        obj.payload = "mutated"
        assertEquals(uid1, generator.generate(obj))
        assertNotEquals(ClientCacheItem.genUid(obj), generator.generate(obj))
    }

    @Test
    fun generate_cacheDisabled_alwaysTracksContentForStableKey() {
        val generator = ClientCacheUidGenerator(cacheEnabled = false)
        val obj = StableKeyPayload("one")

        val uid1 = generator.generate(obj)
        obj.payload = "mutated"

        assertNotEquals(uid1, generator.generate(obj))
        assertEquals(ClientCacheItem.genUid(obj), generator.generate(obj))
    }

    @Test
    fun generate_cacheEnabled_equalDistinctInstancesProduceSameUid() {
        val generator = ClientCacheUidGenerator(cacheEnabled = true)
        val obj1 = RequestResult(7, "seven")
        val obj2 = RequestResult(7, "seven")

        assertEquals(generator.generate(obj1), generator.generate(obj2))
    }

    @Test
    fun generate_cacheEnabled_distinctValuesProduceDistinctUids() {
        val generator = ClientCacheUidGenerator(cacheEnabled = true)

        assertNotEquals(
            generator.generate(RequestResult(1, "one")),
            generator.generate(RequestResult(2, "two"))
        )
    }

    @Test
    fun generate_unicodeAndEmptyContent_areStable() {
        val generator = ClientCacheUidGenerator()
        val unicode = RequestResult(0, "中文-Ωß-🙂")

        assertEquals(generator.generate(unicode), generator.generate(unicode))
        assertEquals(generator.generate(RequestResult(null, "")), generator.generate(RequestResult(null, "")))
    }

    @Test
    fun generate_cacheEnabled_nullFromCache_fallsBackToDirectComputation() {
        // computeIfAbsent never returns null with the real mapping function; swap in a
        // null-returning cache to exercise the defensive elvis fallback.
        val generator = ClientCacheUidGenerator(cacheEnabled = true)
        val nullReturningCache = object : ConcurrentReferenceHashMap<Any, String>() {
            override fun computeIfAbsent(key: Any?, mappingFunction: Function<in Any, out String>): String? = null
        }
        val field = ClientCacheUidGenerator::class.java.getDeclaredField("uidCache")
        field.isAccessible = true
        field.set(generator, nullReturningCache)

        val obj = RequestResult(1, "one")
        assertEquals(ClientCacheItem.genUid(obj), generator.generate(obj))
    }

    /**
     * Mutable payload whose equals/hashCode never change, so the equality-keyed uid cache keeps
     * hitting the same entry even after the JSON content changes.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class StableKeyPayload(var payload: String) {
        override fun equals(other: Any?): Boolean = other is StableKeyPayload
        override fun hashCode(): Int = 0
    }
}
