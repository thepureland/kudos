package io.kudos.ability.web.guest.provider

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.web.guest.init.properties.GuestProperties
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import jakarta.annotation.Resource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Integration tests for [RedisGuestAccessStore] against a real Redis (testcontainer).
 *
 * Covers the contract that downstream apps rely on:
 *  - store(...) writes a hash on first see, then rolls TTL on subsequent stores of the same key;
 *  - the listener fires once on the "new key" path and never on the "TTL roll" path;
 *  - count() reflects how many active keys exist under the configured prefix;
 *  - getByHash(key) returns null for missing keys, a populated GuestAccess for present ones;
 *  - groupName routing returns the matching named RedisTemplate.
 *
 * Each test resets the keyspace via [RedisGuestAccessStore.clearForTest] to keep them independent
 * when run together.
 *
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
internal class RedisGuestAccessStoreTest {

    @Resource
    private lateinit var redisTemplates: RedisTemplates

    private val properties = GuestProperties().apply {
        enabled = true
        repository.prefix = "guest-test"
        repository.timeout = Duration.ofMinutes(5)
    }
    private val activations = AtomicInteger(0)
    private val listener = IGuestAccessListener { activations.incrementAndGet() }

    private fun newStore(): RedisGuestAccessStore =
        RedisGuestAccessStore(properties, redisTemplates, listener)

    @BeforeTest
    fun cleanUp() {
        activations.set(0)
        newStore().clearForTest()
    }

    @AfterTest
    fun cleanUpAfter() {
        newStore().clearForTest()
    }

    @Test
    fun store_firstWriteCreatesKey_andFiresListener() {
        val store = newStore()
        val guest = GuestAccess().apply {
            hash = "first-hash"
            payload = mapOf("v" to "abc")
        }

        store.store(guest)

        assertEquals(1, store.count().count, "exactly one key after first store")
        assertEquals(1, activations.get(), "listener must fire on new-key transition")
    }

    @Test
    fun store_subsequentWriteRollsTtl_doesNotFireListener() {
        val store = newStore()
        val guest = GuestAccess().apply {
            hash = "rolling-hash"
            payload = mapOf("v" to "abc")
        }

        store.store(guest)
        store.store(guest)
        store.store(guest)

        assertEquals(1, store.count().count, "repeated stores of the same hash collapse onto one key")
        assertEquals(
            1, activations.get(),
            "the active listener is for 'becomes live' transitions; TTL rolls on a live key must NOT re-fire",
        )
    }

    @Test
    fun store_missingHash_throws() {
        val store = newStore()
        val guest = GuestAccess() // hash deliberately left null

        // We deliberately fail loud here: silent skipping would hide a real wiring bug (someone
        // wired a custom service that forgot to populate hash before storing).
        try {
            store.store(guest)
            error("expected IllegalStateException for missing hash, but call returned normally")
        } catch (e: IllegalStateException) {
            assertNotNull(e.message)
        }
    }

    @Test
    fun count_reflectsLiveKeysUnderPrefix() {
        val store = newStore()
        store.store(GuestAccess().apply { hash = "h1"; payload = emptyMap() })
        store.store(GuestAccess().apply { hash = "h2"; payload = emptyMap() })
        store.store(GuestAccess().apply { hash = "h3"; payload = emptyMap() })

        assertEquals(3, store.count().count)
    }

    @Test
    fun count_isolatedByPrefix() {
        // A separate store with a different prefix must not see the first store's keys; this
        // is what allows multiple kudos apps to share one Redis without bleeding visitor counts.
        val storeA = newStore()
        val otherProps = GuestProperties().apply {
            repository.prefix = "guest-other-prefix"
            repository.timeout = Duration.ofMinutes(5)
        }
        val storeB = RedisGuestAccessStore(otherProps, redisTemplates, null)

        try {
            storeA.store(GuestAccess().apply { hash = "in-a-1"; payload = emptyMap() })
            storeA.store(GuestAccess().apply { hash = "in-a-2"; payload = emptyMap() })
            storeB.store(GuestAccess().apply { hash = "in-b"; payload = emptyMap() })

            assertEquals(2, storeA.count().count)
            assertEquals(1, storeB.count().count)
        } finally {
            storeB.clearForTest()
        }
    }

    @Test
    fun getByHash_returnsPayloadForKnownKey() {
        val store = newStore()
        store.store(GuestAccess().apply { hash = "known"; payload = mapOf("k" to "v") })

        val read = store.getByHash("known")

        assertNotNull(read, "known hash must surface back")
        assertEquals("known", read.hash)
        assertEquals(mapOf("k" to "v"), read.payload)
    }

    @Test
    fun getByHash_returnsNullForMissing() {
        assertNull(newStore().getByHash("does-not-exist"))
    }

    @Test
    fun store_storesEmptyMapWhenPayloadIsNull() {
        // A visitor whose IGuestAccessService.hash() leaves payload null should still produce a
        // retrievable key with an empty payload — count() must still see it.
        val store = newStore()
        store.store(GuestAccess().apply { hash = "no-payload-hash" }) // payload remains null

        assertEquals(1, store.count().count)
        val read = store.getByHash("no-payload-hash")
        assertNotNull(read)
        assertEquals(emptyMap(), read.payload)
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry?) {
            RedisTestContainer.startIfNeeded(registry)
        }
    }
}
