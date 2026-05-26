package io.kudos.ability.cache.common.core.keyvalue

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.notify.CacheMessage
import io.kudos.ability.cache.common.notify.ICacheMessageHandler
import io.kudos.context.kit.SpringKit
import org.springframework.cache.Cache
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.context.support.StaticApplicationContext
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests covering [MixCache] write ordering and broadcasting for get / put / evict / putIfAbsent / clear
 * under each [CacheStrategy].
 *
 * Key checks:
 *  - `LOCAL_REMOTE` write order is **remote first, then local** — failure semantics documented in the README.
 *  - After a `LOCAL_REMOTE` write, an invalidation message is asynchronously broadcast to all `ICacheMessageHandler` implementations.
 *  - `putIfAbsent` does not broadcast when the value already exists, but does backfill local (read semantics, not write).
 *  - `SINGLE_LOCAL` / `REMOTE` write only to their respective layer, with no broadcast.
 *
 * Broadcasting is fire-and-forget (shared daemon executor); assertions use [CountDownLatch] to wait for a single message to arrive.
 */
internal class MixCacheTest {

    private lateinit var ctx: StaticApplicationContext
    private lateinit var handler: RecordingHandler

    @BeforeTest
    fun setup() {
        ctx = StaticApplicationContext().apply { refresh() }
        handler = RecordingHandler()
        ctx.beanFactory.registerSingleton("recordingHandler", handler)
        SpringKit.applicationContext = ctx
    }

    @AfterTest
    fun teardown() {
        ctx.close()
        // Do not clear the SpringKit.applicationContext field — the setter does not allow null, and later tests will overwrite it.
    }

    // region SINGLE_LOCAL

    @Test
    fun singleLocal_put_writesLocalOnly_andDoesNotBroadcast() {
        val local = newCache("user")
        val cache = MixCache(CacheStrategy.SINGLE_LOCAL, local, /* remote = */ null)

        cache.put("k", "v")

        assertEquals("v", local.get("k")?.get())
        assertEquals(0, handler.size(), "SINGLE_LOCAL must not broadcast")
    }

    @Test
    fun singleLocal_get_readsLocal() {
        val local = newCache("user").apply { put("k", "v") }
        val cache = MixCache(CacheStrategy.SINGLE_LOCAL, local, null)
        assertEquals("v", cache.get("k")?.get())
    }

    @Test
    fun singleLocal_evict_localOnly() {
        val local = newCache("user").apply { put("k", "v") }
        val cache = MixCache(CacheStrategy.SINGLE_LOCAL, local, null)

        cache.evict("k")

        assertNull(local.get("k"))
        assertEquals(0, handler.size())
    }

    // endregion

    // region REMOTE

    @Test
    fun remote_put_writesRemoteOnly_andDoesNotBroadcast() {
        val remote = newCache("user")
        val cache = MixCache(CacheStrategy.REMOTE, /* local = */ null, remote)

        cache.put("k", "v")

        assertEquals("v", remote.get("k")?.get())
        assertEquals(0, handler.size(), "REMOTE must not broadcast")
    }

    @Test
    fun remote_requireLocalThrows_whenLocalMissing() {
        val cache = MixCache(CacheStrategy.SINGLE_LOCAL, /* local = */ null, null)
        assertFails { cache.put("k", "v") }
    }

    // endregion

    // region LOCAL_REMOTE — core

    @Test
    fun localRemote_put_writesRemoteThenLocal_andBroadcasts() {
        val local = OrderRecordingCache(newCache("user"), "local")
        val remote = OrderRecordingCache(newCache("user"), "remote")
        val ops = mutableListOf<String>().apply {
            local.recordTo(this); remote.recordTo(this)
        }

        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, remote)
        handler.expect(1)

        cache.put("k", "v")

        assertTrue(handler.await(2, TimeUnit.SECONDS), "Broadcast should arrive asynchronously within 2s")
        assertEquals(listOf("remote:put:k", "local:put:k"), ops, "Write order should be remote first, then local")
        val msg = handler.messages().single()
        assertEquals("user", msg.cacheName)
        assertEquals("k", msg.key)
    }

    @Test
    fun localRemote_get_localHit_doesNotTouchRemote() {
        val local = newCache("user").apply { put("k", "v") }
        val remote = ThrowOnAnyCache("remote", "user")
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, remote)

        assertEquals("v", cache.get("k")?.get())
    }

    @Test
    fun localRemote_get_localMissBackfillsFromRemote() {
        val local = newCache("user")
        val remote = newCache("user").apply { put("k", "v") }
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, remote)

        assertEquals("v", cache.get("k")?.get())
        // After a remote hit, backfill local.
        assertEquals("v", local.get("k")?.get())
    }

    @Test
    fun localRemote_evict_clearsBothAndBroadcasts() {
        val local = newCache("user").apply { put("k", "v") }
        val remote = newCache("user").apply { put("k", "v") }
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, remote)
        handler.expect(1)

        cache.evict("k")

        assertTrue(handler.await(2, TimeUnit.SECONDS))
        assertNull(local.get("k"))
        assertNull(remote.get("k"))
        assertEquals("k", handler.messages().single().key)
    }

    @Test
    fun localRemote_clear_broadcastsWithNullKey() {
        val local = newCache("user").apply { put("k", "v") }
        val remote = newCache("user").apply { put("k", "v") }
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, remote)
        handler.expect(1)

        cache.clear()

        assertTrue(handler.await(2, TimeUnit.SECONDS))
        assertNull(handler.messages().single().key, "Broadcast key for clear should be null")
    }

    @Test
    fun localRemote_putIfAbsent_remoteWasEmpty_writesBothAndBroadcasts() {
        val local = newCache("user")
        val remote = newCache("user")
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, remote)
        handler.expect(1)

        val previous = cache.putIfAbsent("k", "v")

        assertNull(previous, "First putIfAbsent should return null")
        assertEquals("v", local.get("k")?.get())
        assertEquals("v", remote.get("k")?.get())
        assertTrue(handler.await(2, TimeUnit.SECONDS), "A new insert should broadcast")
    }

    @Test
    fun localRemote_putIfAbsent_remoteHadValue_backfillsLocalWithoutBroadcast() {
        val local = newCache("user")
        val remote = newCache("user").apply { put("k", "remote-v") }
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, remote)

        val previous = cache.putIfAbsent("k", "new-v")

        assertEquals("remote-v", previous?.get(), "Should return the original remote value")
        // Backfill local with the remote value to avoid re-querying from this node later.
        assertEquals("remote-v", local.get("k")?.get())
        // Wait a moment to confirm no broadcast was triggered asynchronously.
        Thread.sleep(200)
        assertEquals(0, handler.size(), "Should not broadcast invalidation when the remote value already exists")
    }

    @Test
    fun localRemote_remoteWriteFails_propagatesException_noBroadcast_noLocalWrite() {
        // Remote write throws → full rollback: exception propagates, local keeps original value, no broadcast.
        val local = newCache("user").apply { put("k", "old-local") }
        val remote = WriteFailingCache(newCache("user"), failOn = setOf("put"))
        remote.delegate.put("k", "old-remote")
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, remote)
        handler.expect(1) // Expect 0; failure to satisfy the latch proves no broadcast occurred.

        assertFails { cache.put("k", "new-v") }

        assertEquals("old-local", local.get("k")?.get(), "Local should keep its original value when remote fails")
        assertEquals("old-remote", remote.delegate.get("k")?.get(), "Remote should keep its original value when remote fails")
        // Wait a bit so the fire-and-forget executor has a chance to run — confirms there was no broadcast.
        assertTrue(!handler.await(300, TimeUnit.MILLISECONDS), "A remote failure must not trigger a broadcast")
    }

    @Test
    fun localRemote_localWriteFails_swallowed_broadcastStillFires() {
        // Remote write succeeds, local write fails — exception is swallowed, a warn log is emitted, and the broadcast still fires.
        val local = WriteFailingCache(newCache("user"), failOn = setOf("put"))
        local.delegate.put("k", "old-local")
        val remote = newCache("user").apply { put("k", "old-remote") }
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, remote)
        handler.expect(1)

        // The call should not throw.
        cache.put("k", "new-v")

        assertEquals("new-v", remote.get("k")?.get(), "Remote should be updated successfully")
        assertEquals("old-local", local.delegate.get("k")?.get(),
            "Local keeps its original value after a write failure (no throw, no collateral state damage)")
        assertTrue(handler.await(2, TimeUnit.SECONDS),
            "Local failure but remote success → still broadcast so other nodes evict their local copies")
    }

    @Test
    fun localRemote_getName_returnsLocalName() {
        val local = newCache("user-local")
        val remote = newCache("user-remote")
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, remote)
        assertEquals("user-local", cache.name)
    }

    @Test
    fun nativeCache_returnsSelf() {
        val local = newCache("user")
        val cache = MixCache(CacheStrategy.SINGLE_LOCAL, local, null)
        assertSame(cache, cache.nativeCache)
    }

    // endregion

    // region helpers

    private fun newCache(name: String): Cache = ConcurrentMapCache(name, /* allowNullValues = */ true)

    /** Records inbound messages plus a counting latch so assertions can confirm async broadcasts arrived. */
    private class RecordingHandler : ICacheMessageHandler {
        private val received = Collections.synchronizedList(mutableListOf<CacheMessage>())
        @Volatile private var latch: CountDownLatch = CountDownLatch(0)
        fun expect(count: Int) {
            received.clear()
            latch = CountDownLatch(count)
        }
        fun await(timeout: Long, unit: TimeUnit): Boolean = latch.await(timeout, unit)
        fun messages(): List<CacheMessage> = received.toList()
        fun size(): Int = received.size
        override fun sendMessage(message: CacheMessage) {
            received.add(message)
            latch.countDown()
        }
        override fun receiveMessage(message: CacheMessage) {}
    }

    /** Wraps a [Cache] and appends the order of put/evict/clear to an external list so tests can assert "remote first, then local". */
    private class OrderRecordingCache(private val delegate: Cache, private val tag: String) : Cache by delegate {
        @Volatile private var sink: MutableList<String>? = null
        fun recordTo(target: MutableList<String>) { sink = target }
        override fun put(key: Any, value: Any?) {
            synchronized(this) { sink?.add("$tag:put:$key") }
            delegate.put(key, value)
        }
        override fun evict(key: Any) {
            synchronized(this) { sink?.add("$tag:evict:$key") }
            delegate.evict(key)
        }
        override fun clear() {
            synchronized(this) { sink?.add("$tag:clear") }
            delegate.clear()
        }
    }

    /** Throws on the specified write operations and delegates the rest. Used for LOCAL_REMOTE failure semantics tests. */
    private class WriteFailingCache(val delegate: Cache, private val failOn: Set<String>) : Cache by delegate {
        override fun put(key: Any, value: Any?) {
            if ("put" in failOn) error("forced put failure on ${delegate.name}")
            delegate.put(key, value)
        }
        override fun evict(key: Any) {
            if ("evict" in failOn) error("forced evict failure on ${delegate.name}")
            delegate.evict(key)
        }
        override fun clear() {
            if ("clear" in failOn) error("forced clear failure on ${delegate.name}")
            delegate.clear()
        }
        override fun putIfAbsent(key: Any, value: Any?): Cache.ValueWrapper? {
            if ("putIfAbsent" in failOn) error("forced putIfAbsent failure on ${delegate.name}")
            return delegate.putIfAbsent(key, value)
        }
    }

    /** Throws on any read/write — used to assert that "a local hit should not trigger remote access". */
    private class ThrowOnAnyCache(private val tag: String, private val cacheName: String) : Cache {
        override fun getName(): String = cacheName
        override fun getNativeCache(): Any = this
        override fun get(key: Any): Cache.ValueWrapper? = error("$tag should not be queried")
        override fun <T : Any> get(key: Any, type: Class<T>?): T? = error("$tag should not be queried")
        override fun <T : Any> get(key: Any, valueLoader: java.util.concurrent.Callable<T>): T? = error("$tag should not be queried")
        override fun put(key: Any, value: Any?): Unit = error("$tag should not be written")
        override fun evict(key: Any): Unit = error("$tag should not be written")
        override fun clear(): Unit = error("$tag should not be cleared")
    }
    // endregion
}
