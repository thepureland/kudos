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
 * [MixCache] 各 [CacheStrategy] 下 get / put / evict / putIfAbsent / clear 的写入顺序与广播单测。
 *
 * 关键检查点：
 *  - `LOCAL_REMOTE` 的写入顺序是**先远端后本地**——失败语义在 README 已说明
 *  - `LOCAL_REMOTE` 写完后异步广播失效消息给所有 `ICacheMessageHandler` 实现
 *  - `putIfAbsent` 已存在时不广播、但回填本地（读语义而非写语义）
 *  - `SINGLE_LOCAL` / `REMOTE` 走对应的单端，不广播
 *
 * 广播是 fire-and-forget（共享 daemon executor），断言时用 [CountDownLatch] 等单条消息抵达。
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
        // 不清 SpringKit.applicationContext 字段——getter 不允许写 null，且后续 test 会重新覆盖
    }

    // region SINGLE_LOCAL

    @Test
    fun singleLocal_put_writesLocalOnly_andDoesNotBroadcast() {
        val local = newCache("user")
        val cache = MixCache(CacheStrategy.SINGLE_LOCAL, local, /* remote = */ null)

        cache.put("k", "v")

        assertEquals("v", local.get("k")?.get())
        assertEquals(0, handler.size(), "SINGLE_LOCAL 不应广播")
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
        assertEquals(0, handler.size(), "REMOTE 不应广播")
    }

    @Test
    fun remote_requireLocalThrows_whenLocalMissing() {
        val cache = MixCache(CacheStrategy.SINGLE_LOCAL, /* local = */ null, null)
        assertFails { cache.put("k", "v") }
    }

    // endregion

    // region LOCAL_REMOTE — 核心

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

        assertTrue(handler.await(2, TimeUnit.SECONDS), "广播应在 2s 内异步抵达")
        assertEquals(listOf("remote:put:k", "local:put:k"), ops, "写入顺序应当先远端后本地")
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
        // 远端命中后回填本地
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
        assertNull(handler.messages().single().key, "clear 的广播 key 应为 null")
    }

    @Test
    fun localRemote_putIfAbsent_remoteWasEmpty_writesBothAndBroadcasts() {
        val local = newCache("user")
        val remote = newCache("user")
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, remote)
        handler.expect(1)

        val previous = cache.putIfAbsent("k", "v")

        assertNull(previous, "首次 putIfAbsent 应返回 null")
        assertEquals("v", local.get("k")?.get())
        assertEquals("v", remote.get("k")?.get())
        assertTrue(handler.await(2, TimeUnit.SECONDS), "新插入应广播")
    }

    @Test
    fun localRemote_putIfAbsent_remoteHadValue_backfillsLocalWithoutBroadcast() {
        val local = newCache("user")
        val remote = newCache("user").apply { put("k", "remote-v") }
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, remote)

        val previous = cache.putIfAbsent("k", "new-v")

        assertEquals("remote-v", previous?.get(), "返回的应是远端原值")
        // 远端值回填到本地，避免本节点后续再查
        assertEquals("remote-v", local.get("k")?.get())
        // 等一会确认没有广播被异步触发
        Thread.sleep(200)
        assertEquals(0, handler.size(), "远端已存在不应广播失效")
    }

    @Test
    fun localRemote_remoteWriteFails_propagatesException_noBroadcast_noLocalWrite() {
        // 远端写抛异常 → 整体回滚：异常上抛、本地保持原值、不广播
        val local = newCache("user").apply { put("k", "old-local") }
        val remote = WriteFailingCache(newCache("user"), failOn = setOf("put"))
        remote.delegate.put("k", "old-remote")
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, remote)
        handler.expect(1) // 期望 0；用 latch 等不到来证明无广播

        assertFails { cache.put("k", "new-v") }

        assertEquals("old-local", local.get("k")?.get(), "远端失败时本地应保持原值")
        assertEquals("old-remote", remote.delegate.get("k")?.get(), "远端失败时远端应保持原值")
        // 等一段让 fire-and-forget 有机会执行——确实没广播
        assertTrue(!handler.await(300, TimeUnit.MILLISECONDS), "远端失败不应触发广播")
    }

    @Test
    fun localRemote_localWriteFails_swallowed_broadcastStillFires() {
        // 远端写成功，本地写失败 —— 异常被吞 + warn 日志 + 广播仍发出
        val local = WriteFailingCache(newCache("user"), failOn = setOf("put"))
        local.delegate.put("k", "old-local")
        val remote = newCache("user").apply { put("k", "old-remote") }
        val cache = MixCache(CacheStrategy.LOCAL_REMOTE, local, remote)
        handler.expect(1)

        // 调用不应抛
        cache.put("k", "new-v")

        assertEquals("new-v", remote.get("k")?.get(), "远端应当被成功更新")
        assertEquals("old-local", local.delegate.get("k")?.get(),
            "本地写失败后保持原值（不抛、不破坏其他状态）")
        assertTrue(handler.await(2, TimeUnit.SECONDS),
            "本地失败但远端成功 → 仍广播让其他节点剔除其本地副本")
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

    /** 把入站消息记录下来 + 计数 latch，便于断言 async 广播抵达。 */
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

    /** 包一层 [Cache] 把 put/evict/clear 的发生顺序追加到外部列表，用于断言"先远端后本地"。 */
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

    /** 指定写操作抛异常，其余 delegate。测 LOCAL_REMOTE 失败语义用。 */
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

    /** 任何读 / 写都抛——用于断言"本地命中不应触发远端访问"。 */
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
