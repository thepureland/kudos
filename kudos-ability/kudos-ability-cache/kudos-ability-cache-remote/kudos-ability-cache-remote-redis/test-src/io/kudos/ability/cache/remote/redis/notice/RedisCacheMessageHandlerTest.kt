package io.kudos.ability.cache.remote.redis.notice

import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.notify.CacheMessage
import io.kudos.ability.cache.common.support.CacheCleanRegister
import io.kudos.ability.cache.common.support.ICacheCleanListener
import io.kudos.ability.cache.common.support.IHashCacheSync
import io.kudos.ability.data.memdb.redis.RedisTemplates
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.redis.connection.DefaultMessage
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [RedisCacheMessageHandler].
 *
 * Covers, with mocked Redis collaborators and reflection-injected dependencies:
 * - sendMessage: node-id stamping and publishing on the versioned channel
 * - onMessage: successful dispatch, deserialization failure (logged, swallowed), null payload (skipped),
 *   and receiveMessage exceptions being caught so the listener thread survives
 * - receiveMessage: self-node skip, foreign key-value clear, hash-type clear/evict for null / scalar /
 *   collection keys (nulls filtered), absent hash sync hook, and clean-listener firing with the
 *   version-stripped cache name
 * - the redisTemplate accessor
 *
 * @author K
 * @since 1.0.0
 */
internal class RedisCacheMessageHandlerTest {

    private val nodeId = "node-self"
    private val store = "cacheStore"

    private lateinit var handler: RedisCacheMessageHandler
    private lateinit var template: RedisTemplate<Any, Any?>
    private lateinit var mixCacheManager: MixCacheManager
    private lateinit var versionConfig: CacheVersionConfig
    private var hashSync: RecordingHashSync? = null

    private class RecordingHashSync : IHashCacheSync {
        val cleared = mutableListOf<String>()
        val evicted = mutableListOf<Pair<String, Any>>()
        override fun clearLocal(cacheName: String) { cleared += cacheName }
        override fun evictLocal(cacheName: String, id: Any) { evicted += cacheName to id }
    }

    /** Deterministic serializer stub; deserializes to a fixed object or throws. */
    private class FixedSerializer(
        private val result: Any?,
        private val throwOnDeserialize: Boolean = false
    ) : RedisSerializer<Any> {
        override fun serialize(value: Any?): ByteArray = ByteArray(0)
        override fun deserialize(bytes: ByteArray?): Any? {
            if (throwOnDeserialize) throw RuntimeException("deserialize boom")
            return result
        }
    }

    @Suppress("UNCHECKED_CAST")
    @BeforeTest
    fun setUp() {
        template = mock(RedisTemplate::class.java) as RedisTemplate<Any, Any?>
        mixCacheManager = mock(MixCacheManager::class.java)
        versionConfig = CacheVersionConfig()
        hashSync = RecordingHashSync()

        handler = RedisCacheMessageHandler(nodeId)
        inject("remoteStore", store)
        inject("redisTemplates", RedisTemplates(mutableMapOf(store to template), template))
        inject("mixCacheManager", mixCacheManager)
        inject("versionConfig", versionConfig)
        inject("hashCacheSync", hashSync)
    }

    private fun inject(fieldName: String, value: Any?) {
        val field = RedisCacheMessageHandler::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(handler, value)
    }

    // ---------- sendMessage ----------

    @Test
    fun sendMessage_stampsNodeIdAndPublishesOnVersionedChannel() {
        val message = CacheMessage("c1", "k1")
        handler.sendMessage(message)
        assertEquals(nodeId, message.nodeId, "sendMessage must stamp the sender node id")
        verify(template).convertAndSend(versionConfig.realMsgChannel, message)
    }

    // ---------- onMessage ----------

    @Test
    fun onMessage_dispatchesDeserializedForeignMessage() {
        val foreign = CacheMessage("c-onmsg", "k").apply { nodeId = "other-node" }
        `when`(template.valueSerializer).thenReturn(FixedSerializer(foreign))

        handler.onMessage(DefaultMessage("ch".toByteArray(), ByteArray(0)), null)

        verify(mixCacheManager).clearLocal("c-onmsg", "k")
    }

    @Test
    fun onMessage_deserializationFailure_isLoggedNotThrown() {
        `when`(template.valueSerializer).thenReturn(FixedSerializer(null, throwOnDeserialize = true))

        // must not propagate — a throw here would kill the listener thread
        handler.onMessage(DefaultMessage("ch".toByteArray(), ByteArray(0)), null)

        verify(mixCacheManager, never()).clearLocal(ArgumentMatchers.anyString(), ArgumentMatchers.any())
    }

    @Test
    fun onMessage_nullPayload_isSkipped() {
        `when`(template.valueSerializer).thenReturn(FixedSerializer(null))

        handler.onMessage(DefaultMessage("ch".toByteArray(), ByteArray(0)), null)

        verify(mixCacheManager, never()).clearLocal(ArgumentMatchers.anyString(), ArgumentMatchers.any())
    }

    @Test
    fun onMessage_receiveMessageFailure_isCaught() {
        val foreign = CacheMessage("c-fail", "k").apply { nodeId = "other-node" }
        `when`(template.valueSerializer).thenReturn(FixedSerializer(foreign))
        doThrow(RuntimeException("clear boom")).`when`(mixCacheManager).clearLocal("c-fail", "k")

        // must not propagate — subsequent messages would otherwise be misread as failed
        handler.onMessage(DefaultMessage("ch".toByteArray(), "payload".toByteArray()), "p".toByteArray())

        verify(mixCacheManager).clearLocal("c-fail", "k")
    }

    // ---------- receiveMessage ----------

    @Test
    fun receiveMessage_selfNodeMessage_doesNotClearLocal() {
        val selfMsg = CacheMessage("c-self", "k").apply { nodeId = this@RedisCacheMessageHandlerTest.nodeId }
        handler.receiveMessage(selfMsg)
        verify(mixCacheManager, never()).clearLocal(ArgumentMatchers.anyString(), ArgumentMatchers.any())
        assertTrue(hashSync!!.cleared.isEmpty())
        assertTrue(hashSync!!.evicted.isEmpty())
    }

    @Test
    fun receiveMessage_foreignKeyValueMessage_clearsLocal() {
        val msg = CacheMessage("c-kv", "key1").apply { nodeId = "other" }
        handler.receiveMessage(msg)
        verify(mixCacheManager).clearLocal("c-kv", "key1")
    }

    @Test
    fun receiveMessage_foreignHash_nullKey_clearsWholeLocalHash() {
        val msg = CacheMessage("c-hash", null).apply { nodeId = "other"; cacheType = "hash" }
        handler.receiveMessage(msg)
        assertEquals(listOf("c-hash"), hashSync!!.cleared)
        assertTrue(hashSync!!.evicted.isEmpty())
        verify(mixCacheManager, never()).clearLocal(ArgumentMatchers.anyString(), ArgumentMatchers.any())
    }

    @Test
    fun receiveMessage_foreignHash_scalarKey_evictsSingleId() {
        val msg = CacheMessage("c-hash", "id1").apply { nodeId = "other"; cacheType = "hash" }
        handler.receiveMessage(msg)
        assertEquals(listOf("c-hash" to "id1" as Any), hashSync!!.evicted)
        assertTrue(hashSync!!.cleared.isEmpty())
    }

    @Test
    fun receiveMessage_foreignHash_collectionKey_evictsEachNonNullId() {
        val msg = CacheMessage("c-hash", listOf("id1", null, "id2")).apply { nodeId = "other"; cacheType = "hash" }
        handler.receiveMessage(msg)
        assertEquals(listOf("c-hash" to "id1" as Any, "c-hash" to "id2" as Any), hashSync!!.evicted)
        assertTrue(hashSync!!.cleared.isEmpty())
    }

    @Test
    fun receiveMessage_foreignHash_emptyCollection_evictsNothing() {
        val msg = CacheMessage("c-hash", emptyList<Any>()).apply { nodeId = "other"; cacheType = "hash" }
        handler.receiveMessage(msg)
        assertTrue(hashSync!!.evicted.isEmpty())
        assertTrue(hashSync!!.cleared.isEmpty())
    }

    @Test
    fun receiveMessage_foreignHash_withoutHashSyncHook_isNoOp() {
        inject("hashCacheSync", null)
        val msg = CacheMessage("c-hash", "id1").apply { nodeId = "other"; cacheType = "hash" }
        handler.receiveMessage(msg) // no NPE / no exception
        verify(mixCacheManager, never()).clearLocal(ArgumentMatchers.anyString(), ArgumentMatchers.any())
    }

    @Test
    fun receiveMessage_firesCleanListenersWithVersionStrippedName() {
        val logicalName = "c-listener-${System.nanoTime()}" // unique: CacheCleanRegister is process-global
        val versionedName = versionConfig.getFinalCacheName(logicalName)
        val calls = mutableListOf<Pair<String, Any?>>()
        val listener = object : ICacheCleanListener {
            override fun cleanCache(cacheName: String, key: Any?) { calls += cacheName to key }
            override fun afterPropertiesSet() {}
        }
        CacheCleanRegister.register(logicalName, listener)

        // message carries the versioned name (as published); listener must receive the logical name
        val msg = CacheMessage(versionedName, "k9").apply { nodeId = "other" }
        handler.receiveMessage(msg)

        assertEquals(listOf(logicalName to "k9" as Any?), calls)
        verify(mixCacheManager).clearLocal(versionedName, "k9")
    }

    @Test
    fun receiveMessage_selfNode_stillFiresCleanListeners() {
        val logicalName = "c-self-listener-${System.nanoTime()}"
        val versionedName = versionConfig.getFinalCacheName(logicalName)
        val calls = mutableListOf<Pair<String, Any?>>()
        val listener = object : ICacheCleanListener {
            override fun cleanCache(cacheName: String, key: Any?) { calls += cacheName to key }
            override fun afterPropertiesSet() {}
        }
        CacheCleanRegister.register(logicalName, listener)

        val msg = CacheMessage(versionedName, null).apply { nodeId = this@RedisCacheMessageHandlerTest.nodeId }
        handler.receiveMessage(msg)

        assertEquals(listOf(logicalName to null as Any?), calls)
        verify(mixCacheManager, never()).clearLocal(ArgumentMatchers.anyString(), ArgumentMatchers.any())
    }

    // ---------- redisTemplate accessor ----------

    @Test
    fun redisTemplateAccessor_returnsTemplateOfRemoteStore() {
        assertSame(template, handler.redisTemplate)
    }
}
