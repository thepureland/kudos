package io.kudos.ability.cache.common.core.keyvalue

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.notify.CacheMessage
import io.kudos.ability.cache.common.notify.ICacheMessageHandler
import io.kudos.context.kit.SpringKit
import org.springframework.cache.Cache
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.context.support.StaticApplicationContext
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Supplementary [MixCache] tests covering branches not exercised by [MixCacheTest]:
 *  - typed get (SINGLE_LOCAL / REMOTE / LOCAL_REMOTE + type-mismatch error + miss),
 *  - get-with-loader (all strategies, including mixGetOrLoad local-hit / remote-hit / load-and-fill),
 *  - putIfAbsent SINGLE_LOCAL / REMOTE,
 *  - clearLocal (single key / clear-all / null local),
 *  - pushMsgRedis swallowing a handler failure (one good + one throwing handler).
 *
 * @author K
 * @since 1.0.0
 */
internal class MixCacheExtraTest {

    private lateinit var ctx: StaticApplicationContext

    @BeforeTest
    fun setup() {
        ctx = StaticApplicationContext().apply { refresh() }
        SpringKit.applicationContext = ctx
    }

    @AfterTest
    fun teardown() {
        ctx.close()
    }

    private fun newCache(name: String): Cache = ConcurrentMapCache(name, true)

    // region typed get

    @Test
    fun typedGet_singleLocal() {
        val local = newCache("u").apply { put("k", "v") }
        assertEquals("v", MixCache(CacheStrategy.SINGLE_LOCAL, local, null).get("k", String::class.java))
    }

    @Test
    fun typedGet_remote() {
        val remote = newCache("u").apply { put("k", "v") }
        assertEquals("v", MixCache(CacheStrategy.REMOTE, null, remote).get("k", String::class.java))
    }

    @Test
    fun typedGet_localRemote_hitAndBackfill() {
        val local = newCache("u")
        val remote = newCache("u").apply { put("k", "v") }
        assertEquals("v", MixCache(CacheStrategy.LOCAL_REMOTE, local, remote).get("k", String::class.java))
    }

    @Test
    fun typedGet_localRemote_miss_returnsNull() {
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, newCache("u"), newCache("u"))
        assertNull(cache.get("absent", String::class.java))
    }

    @Test
    fun typedGet_localRemote_typeMismatch_throws() {
        val local = newCache("u").apply { put("k", 123) }
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, newCache("u"))
        assertFailsWith<IllegalStateException> { cache.get("k", String::class.java) }
    }

    @Test
    fun typedGet_localRemote_nullValueWrapper_returnsNull() {
        // a cached null value (penetration guard) -> wrapper non-null, value null -> returns null without type check
        val local = newCache("u").apply { put("k", null) }
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, newCache("u"))
        assertNull(cache.get("k", String::class.java))
    }

    // endregion

    // region get with loader

    @Test
    fun loaderGet_singleLocal_loadsOnMiss() {
        val local = newCache("u")
        val v = MixCache(CacheStrategy.SINGLE_LOCAL, local, null).get("k", Callable { "loaded" })
        assertEquals("loaded", v)
    }

    @Test
    fun loaderGet_remote_loadsOnMiss() {
        val remote = newCache("u")
        val v = MixCache(CacheStrategy.REMOTE, null, remote).get("k", Callable { "loaded" })
        assertEquals("loaded", v)
    }

    @Test
    fun loaderGet_localRemote_localHit() {
        val local = newCache("u").apply { put("k", "local") }
        val remote = newCache("u").apply { put("k", "remote") }
        val v = MixCache(CacheStrategy.LOCAL_REMOTE, local, remote).get("k", Callable { error("should not load") })
        assertEquals("local", v)
    }

    @Test
    fun loaderGet_localRemote_remoteHit_backfillsLocal() {
        val local = newCache("u")
        val remote = newCache("u").apply { put("k", "remote") }
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, remote)
        val v = cache.get("k", Callable { error("should not load") })
        assertEquals("remote", v)
        assertEquals("remote", local.get("k")?.get())
    }

    @Test
    fun loaderGet_localRemote_bothMiss_loadsAndFillsBoth() {
        val local = newCache("u")
        val remote = newCache("u")
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, remote)
        val v = cache.get("k", Callable { "loaded" })
        assertEquals("loaded", v)
        assertEquals("loaded", local.get("k")?.get())
        assertEquals("loaded", remote.get("k")?.get())
    }

    // endregion

    // region untyped get / require throws

    @Test
    fun untypedGet_remote() {
        val remote = newCache("u").apply { put("k", "v") }
        assertEquals("v", MixCache(CacheStrategy.REMOTE, null, remote).get("k")?.get())
    }

    @Test
    fun untypedGet_remote_nullRemote_returnsNull() {
        assertNull(MixCache(CacheStrategy.REMOTE, null, null).get("k"))
    }

    @Test
    fun requireRemote_throwsWhenRemoteMissing_typedGet() {
        assertFailsWith<IllegalArgumentException> {
            MixCache(CacheStrategy.REMOTE, newCache("u"), null).get("k", String::class.java)
        }
    }

    @Test
    fun requireRemote_throwsWhenRemoteMissing_loaderGet() {
        assertFailsWith<IllegalArgumentException> {
            MixCache(CacheStrategy.REMOTE, newCache("u"), null).get("k", Callable { "v" })
        }
    }

    // endregion

    // region putIfAbsent non-LOCAL_REMOTE

    @Test
    fun putIfAbsent_singleLocal() {
        val local = newCache("u")
        val cache = MixCache(CacheStrategy.SINGLE_LOCAL, local, null)
        assertNull(cache.putIfAbsent("k", "v"))
        assertEquals("v", local.get("k")?.get())
        assertEquals("v", cache.putIfAbsent("k", "other")?.get())
    }

    @Test
    fun putIfAbsent_remote() {
        val remote = newCache("u")
        val cache = MixCache(CacheStrategy.REMOTE, null, remote)
        assertNull(cache.putIfAbsent("k", "v"))
        assertEquals("v", remote.get("k")?.get())
    }

    @Test
    fun putIfAbsent_localRemote_localPutIfAbsentFails_swallowed_stillBroadcasts() {
        val good = RecordingHandler()
        ctx.beanFactory.registerSingleton("goodHandler", good)
        good.expect(1)
        val local = FailingCache(newCache("u"), failOn = "putIfAbsent")
        val remote = newCache("u")
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, remote)

        val prev = cache.putIfAbsent("k", "v")

        assertNull(prev, "remote was empty -> newly inserted")
        assertEquals("v", remote.get("k")?.get())
        assertTrue(good.await(2, TimeUnit.SECONDS), "local failure must still broadcast")
    }

    @Test
    fun putIfAbsent_localRemote_backfillFails_swallowed_returnsExisting() {
        val local = FailingCache(newCache("u"), failOn = "put")
        val remote = newCache("u").apply { put("k", "remote-v") }
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, remote)

        val prev = cache.putIfAbsent("k", "new-v")

        assertEquals("remote-v", prev?.get(), "returns the pre-existing remote value despite backfill failure")
    }

    // endregion

    // region clearLocal

    @Test
    fun clearLocal_singleKey_evictsLocal() {
        val local = newCache("u").apply { put("k", "v") }
        MixCache(CacheStrategy.LOCAL_REMOTE, local, newCache("u")).clearLocal("k")
        assertNull(local.get("k"))
    }

    @Test
    fun clearLocal_nullKey_clearsLocal() {
        val local = newCache("u").apply { put("a", "1"); put("b", "2") }
        MixCache(CacheStrategy.LOCAL_REMOTE, local, newCache("u")).clearLocal(null)
        assertNull(local.get("a"))
        assertNull(local.get("b"))
    }

    @Test
    fun clearLocal_nullLocalCache_isNoOp() {
        // REMOTE-only mix cache has no local; clearLocal must not throw
        MixCache(CacheStrategy.REMOTE, null, newCache("u")).clearLocal("k")
        MixCache(CacheStrategy.REMOTE, null, newCache("u")).clearLocal(null)
    }

    // endregion

    // region pushMsgRedis failure handling

    @Test
    fun pushMsgRedis_swallowsHandlerFailure_andDeliversToOthers() {
        val good = RecordingHandler()
        ctx.beanFactory.registerSingleton("goodHandler", good)
        ctx.beanFactory.registerSingleton("badHandler", ThrowingHandler())
        good.expect(1)

        val local = newCache("u")
        val remote = newCache("u")
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, remote)
        cache.put("k", "v") // triggers async broadcast to both handlers

        assertTrue(good.await(2, TimeUnit.SECONDS), "good handler should still receive despite bad handler throwing")
    }

    @Test
    fun pushMsgRedis_noHandlers_isNoOp() {
        // no ICacheMessageHandler beans -> pushMsgRedis short-circuits
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, newCache("u"), newCache("u"))
        cache.put("k", "v") // must not throw
    }

    // endregion

    private class RecordingHandler : ICacheMessageHandler {
        @Volatile private var latch = CountDownLatch(0)
        fun expect(n: Int) { latch = CountDownLatch(n) }
        fun await(t: Long, u: TimeUnit) = latch.await(t, u)
        override fun sendMessage(message: CacheMessage) { latch.countDown() }
        override fun receiveMessage(message: CacheMessage) {}
    }

    private class ThrowingHandler : ICacheMessageHandler {
        override fun sendMessage(message: CacheMessage) = error("boom")
        override fun receiveMessage(message: CacheMessage) {}
    }

    /** Delegates to [delegate] but throws on the named write op — used for putIfAbsent failure-catch branches. */
    private class FailingCache(private val delegate: Cache, private val failOn: String) : Cache by delegate {
        override fun put(key: Any, value: Any?) {
            if (failOn == "put") error("forced put failure"); delegate.put(key, value)
        }
        override fun putIfAbsent(key: Any, value: Any?): Cache.ValueWrapper? {
            if (failOn == "putIfAbsent") error("forced putIfAbsent failure"); return delegate.putIfAbsent(key, value)
        }
    }
}
