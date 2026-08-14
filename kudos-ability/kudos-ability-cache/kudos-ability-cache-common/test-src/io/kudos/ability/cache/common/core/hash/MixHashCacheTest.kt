package io.kudos.ability.cache.common.core.hash

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.notify.CacheMessage
import io.kudos.ability.cache.common.notify.ICacheMessageHandler
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.context.kit.SpringKit
import org.springframework.context.support.StaticApplicationContext
import java.util.Collections
import kotlin.reflect.KClass
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [MixHashCache] strategy routing: reads (local-first backfill under LOCAL_REMOTE,
 * remote-or-local for others), writes with require-not-null guards, secondary-index capture, and
 * pub/sub notifications (single-message batch for saveBatch / deleteByIds).
 *
 * @author K
 * @since 1.0.0
 */
internal class MixHashCacheTest {

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
    }

    private data class E(override val id: String, val type: String? = null) : IIdEntity<String>

    // region remoteOrLocal / construction guards

    @Test
    fun read_remotePreferredOverLocal_forNonLocalRemoteStrategies() {
        val local = RecordingHashCache().apply { put(E("1", "L")) }
        val remote = RecordingHashCache().apply { put(E("1", "R")) }
        val cache = MixHashCache("c", CacheStrategy.REMOTE, local, remote, "node")
        assertEquals("R", (cache.getById<String, E>("c", "1", E::class))?.type)
    }

    @Test
    fun read_fallsBackToLocalWhenNoRemote() {
        val local = RecordingHashCache().apply { put(E("1", "L")) }
        val cache = MixHashCache("c", CacheStrategy.SINGLE_LOCAL, local, null, "node")
        assertEquals("L", (cache.getById<String, E>("c", "1", E::class))?.type)
    }

    @Test
    fun read_throwsWhenBothImplsNull() {
        val cache = MixHashCache("c", CacheStrategy.REMOTE, null, null, "node")
        assertFails { cache.getById<String, E>("c", "1", E::class) }
    }

    // endregion

    // region getById LOCAL_REMOTE

    @Test
    fun getById_localRemote_localHitReturnsWithoutRemote() {
        val local = RecordingHashCache().apply { put(E("1", "L")) }
        val remote = ThrowingHashCache()
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, local, remote, "node")
        assertEquals("L", (cache.getById<String, E>("c", "1", E::class))?.type)
    }

    @Test
    fun declaredIndexProps_areUsedForBackfillBeforeAnyWrite() {
        // 声明式的意义就在这个场景：进程只读过、从没写过，回填仍要建出二级索引。
        // 旧实现里这两个集合初始为空、由"最近一次写入"覆盖，因此纯读进程回填出的是没有索引的实体，
        // 索引结构变成了调用时序的函数。
        val local = RecordingHashCache()
        val remote = RecordingHashCache().apply { put(E("1", "A")) }
        val cache = MixHashCache(
            "c", CacheStrategy.LOCAL_REMOTE, local, remote, "n",
            declaredFilterable = setOf("type"), declaredSortable = setOf("score")
        )

        cache.getById<String, E>("c", "1", E::class)

        assertEquals(setOf("type"), local.lastFilterable, "回填应使用 handler 声明的可筛选属性")
        assertEquals(setOf("score"), local.lastSortable, "回填应使用 handler 声明的可排序属性")
    }

    @Test
    fun writesWidenDeclaredIndexProps_ratherThanReplacingThem() {
        // 写入传来的属性集只应扩大索引范围，不应把声明的属性挤掉。
        // IHashCache 的多个方法对这两个参数都默认空集，若采用"覆盖"语义，
        // 一次窄集合（甚至空集合）的写入就会让缓存忘掉其他调用方仍在查询的索引。
        val local = RecordingHashCache()
        val remote = RecordingHashCache().apply { put(E("1", "A")) }
        val cache = MixHashCache(
            "c", CacheStrategy.LOCAL_REMOTE, local, remote, "n",
            declaredFilterable = setOf("type"), declaredSortable = emptySet()
        )

        cache.save("c", E("2", "B"), setOf("status"), emptySet())
        cache.getById<String, E>("c", "1", E::class)

        assertEquals(
            setOf("type", "status"), local.lastFilterable,
            "声明的属性与写入带来的属性应取并集"
        )
    }

    @Test
    fun getById_localRemote_missBackfillsLocal() {
        val local = RecordingHashCache()
        val remote = RecordingHashCache().apply { put(E("1", "R")) }
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, local, remote, "node")
        assertEquals("R", (cache.getById<String, E>("c", "1", E::class))?.type)
        // backfilled into local
        assertEquals("R", (local.getById<String, E>("c", "1", E::class))?.type)
    }

    @Test
    fun getById_localRemote_bothMissReturnsNull() {
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, RecordingHashCache(), RecordingHashCache(), "node")
        assertNull(cache.getById<String, E>("c", "1", E::class))
    }

    @Test
    fun getById_localRemote_noLocal_readsRemoteOnly() {
        val remote = RecordingHashCache().apply { put(E("1", "R")) }
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, null, remote, "node")
        assertEquals("R", (cache.getById<String, E>("c", "1", E::class))?.type)
    }

    // endregion

    // region existsById

    @Test
    fun existsById_perStrategy() {
        val local = RecordingHashCache().apply { put(E("1")) }
        val remote = RecordingHashCache().apply { put(E("2")) }
        assertTrue(MixHashCache("c", CacheStrategy.SINGLE_LOCAL, local, remote, "n").existsById("c", "1"))
        assertFalse(MixHashCache("c", CacheStrategy.SINGLE_LOCAL, local, remote, "n").existsById("c", "2"))
        assertTrue(MixHashCache("c", CacheStrategy.REMOTE, local, remote, "n").existsById("c", "2"))
        val lr = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, local, remote, "n")
        assertTrue(lr.existsById("c", "1"))
        assertTrue(lr.existsById("c", "2"))
        assertFalse(lr.existsById("c", "3"))
    }

    @Test
    fun existsById_nullImpls_false() {
        assertFalse(MixHashCache("c", CacheStrategy.SINGLE_LOCAL, null, null, "n").existsById("c", "1"))
        assertFalse(MixHashCache("c", CacheStrategy.REMOTE, null, null, "n").existsById("c", "1"))
    }

    // endregion

    // region save / saveBatch + notify

    @Test
    fun save_singleLocal_writesLocalOnly_noNotify() {
        val local = RecordingHashCache()
        val cache = MixHashCache("c", CacheStrategy.SINGLE_LOCAL, local, null, "n")
        cache.save("c", E("1"), setOf("type"), emptySet())
        assertTrue(local.entities.containsKey("1"))
        assertEquals(0, handler.size())
    }

    @Test
    fun save_singleLocal_nullLocal_throws() {
        val cache = MixHashCache("c", CacheStrategy.SINGLE_LOCAL, null, null, "n")
        assertFails { cache.save("c", E("1")) }
    }

    @Test
    fun save_remote_nullRemote_throws() {
        val cache = MixHashCache("c", CacheStrategy.REMOTE, RecordingHashCache(), null, "n")
        assertFails { cache.save("c", E("1")) }
    }

    @Test
    fun save_localRemote_writesBothAndNotifiesWithId() {
        val local = RecordingHashCache()
        val remote = RecordingHashCache()
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, local, remote, "n")
        cache.save("c", E("1"), setOf("type"), setOf("score"))
        assertTrue(local.entities.containsKey("1"))
        assertTrue(remote.entities.containsKey("1"))
        assertEquals(listOf("1"), handler.keys())
    }

    @Test
    fun save_localRemote_nullRemote_throws() {
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, RecordingHashCache(), null, "n")
        assertFails { cache.save("c", E("1")) }
    }

    @Test
    fun saveBatch_localRemote_singleBatchNotifyWithIdList() {
        val local = RecordingHashCache()
        val remote = RecordingHashCache()
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, local, remote, "n")
        cache.saveBatch("c", listOf(E("1"), E("2")), setOf("type"), emptySet())
        assertTrue(remote.entities.keys.containsAll(setOf("1", "2")))
        // a single notification carrying the id list
        assertEquals(1, handler.size())
        assertEquals(listOf("1", "2"), handler.messages().single().key)
    }

    @Test
    fun saveBatch_singleLocal_writesLocalOnly() {
        val local = RecordingHashCache()
        val cache = MixHashCache("c", CacheStrategy.SINGLE_LOCAL, local, null, "n")
        cache.saveBatch("c", listOf(E("1")))
        assertTrue(local.entities.containsKey("1"))
        assertEquals(0, handler.size())
    }

    @Test
    fun saveBatch_remote_writesRemoteOnly() {
        val remote = RecordingHashCache()
        val cache = MixHashCache("c", CacheStrategy.REMOTE, null, remote, "n")
        cache.saveBatch("c", listOf(E("1")))
        assertTrue(remote.entities.containsKey("1"))
    }

    // endregion

    // region delete

    @Test
    fun deleteById_localRemote_deletesBothAndNotifies() {
        val local = RecordingHashCache().apply { put(E("1")) }
        val remote = RecordingHashCache().apply { put(E("1")) }
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, local, remote, "n")
        cache.deleteById("c", "1", E::class, emptySet(), emptySet())
        assertFalse(local.entities.containsKey("1"))
        assertFalse(remote.entities.containsKey("1"))
        assertEquals(listOf("1"), handler.keys())
    }

    @Test
    fun deleteById_singleLocal() {
        val local = RecordingHashCache().apply { put(E("1")) }
        MixHashCache("c", CacheStrategy.SINGLE_LOCAL, local, null, "n")
            .deleteById("c", "1", E::class, emptySet(), emptySet())
        assertFalse(local.entities.containsKey("1"))
    }

    @Test
    fun deleteById_remote() {
        val remote = RecordingHashCache().apply { put(E("1")) }
        MixHashCache("c", CacheStrategy.REMOTE, null, remote, "n")
            .deleteById("c", "1", E::class, emptySet(), emptySet())
        assertFalse(remote.entities.containsKey("1"))
    }

    @Test
    fun deleteByIds_emptyShortCircuits() {
        val remote = RecordingHashCache()
        MixHashCache("c", CacheStrategy.LOCAL_REMOTE, RecordingHashCache(), remote, "n")
            .deleteByIds("c", emptyList<String>(), E::class, emptySet(), emptySet())
        assertEquals(0, handler.size())
    }

    @Test
    fun deleteByIds_localRemote_singleNotify() {
        val local = RecordingHashCache().apply { put(E("1")); put(E("2")) }
        val remote = RecordingHashCache().apply { put(E("1")); put(E("2")) }
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, local, remote, "n")
        cache.deleteByIds("c", listOf("1", "2"), E::class, emptySet(), emptySet())
        assertFalse(remote.entities.containsKey("1"))
        assertEquals(1, handler.size())
        assertEquals(listOf("1", "2"), handler.messages().single().key)
    }

    @Test
    fun deleteByIds_singleLocal() {
        val local = RecordingHashCache().apply { put(E("1")) }
        MixHashCache("c", CacheStrategy.SINGLE_LOCAL, local, null, "n")
            .deleteByIds("c", listOf("1"), E::class, emptySet(), emptySet())
        assertFalse(local.entities.containsKey("1"))
    }

    @Test
    fun deleteByIds_remote() {
        val remote = RecordingHashCache().apply { put(E("1")) }
        MixHashCache("c", CacheStrategy.REMOTE, null, remote, "n")
            .deleteByIds("c", listOf("1"), E::class, emptySet(), emptySet())
        assertFalse(remote.entities.containsKey("1"))
    }

    // endregion

    // region findByIds / listAll / listBySetIndex / listPageByZSetIndex / list

    @Test
    fun findByIds_localRemote_fullLocalHit() {
        val local = RecordingHashCache().apply { put(E("1")); put(E("2")) }
        val remote = ThrowingHashCache()
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, local, remote, "n")
        val res = cache.findByIds<E>("c", listOf("1", "2"), E::class)
        assertEquals(2, res.size)
    }

    @Test
    fun findByIds_localRemote_partialLocal_fallsToRemoteAndBackfills() {
        val local = RecordingHashCache().apply { put(E("1")) }
        val remote = RecordingHashCache().apply { put(E("1")); put(E("2")) }
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, local, remote, "n")
        val res = cache.findByIds<E>("c", listOf("1", "2"), E::class)
        assertEquals(2, res.size)
        // remote results backfilled to local
        assertTrue(local.entities.keys.containsAll(setOf("1", "2")))
    }

    @Test
    fun findByIds_localRemote_remoteEmptyReturnsEmpty() {
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, RecordingHashCache(), null, "n")
        assertTrue(cache.findByIds<E>("c", listOf("1"), E::class).isEmpty())
    }

    @Test
    fun findByIds_nonLocalRemote_delegates() {
        val remote = RecordingHashCache().apply { put(E("1")) }
        val cache = MixHashCache("c", CacheStrategy.REMOTE, null, remote, "n")
        assertEquals(1, cache.findByIds<E>("c", listOf("1"), E::class).size)
    }

    @Test
    fun listAll_localRemote_backfill() {
        val local = RecordingHashCache()
        val remote = RecordingHashCache().apply { put(E("1")) }
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, local, remote, "n")
        assertEquals(1, cache.listAll<String, E>("c", E::class).size)
        assertTrue(local.entities.containsKey("1"))
    }

    @Test
    fun listAll_localRemote_partialLocalMustNotShadowRemote() {
        // 本地层只是远端的一个任意子集（getById 每次只回填一条），因此集合查询不能"本地非空就返回"。
        // 旧实现下，先 getById(1) 回填一条之后再 listAll()，会把这一条当成全集返回 —— 这是错误结果而非陈旧结果。
        val local = RecordingHashCache().apply { put(E("1")) }
        val remote = RecordingHashCache().apply { put(E("1")); put(E("2")) }
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, local, remote, "n")
        assertEquals(2, cache.listAll<String, E>("c", E::class).size, "应以远端为准返回全集")
    }

    @Test
    fun listAll_localRemote_bothEmpty() {
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, RecordingHashCache(), null, "n")
        assertTrue(cache.listAll<String, E>("c", E::class).isEmpty())
    }

    @Test
    fun listAll_nonLocalRemote() {
        val remote = RecordingHashCache().apply { put(E("1")) }
        assertEquals(1, MixHashCache("c", CacheStrategy.REMOTE, null, remote, "n").listAll<String, E>("c", E::class).size)
    }

    @Test
    fun listBySetIndex_localRemote_backfillAndIndexCapture() {
        val local = RecordingHashCache()
        val remote = RecordingHashCache().apply { putSetIndex("type", "A", listOf(E("1", "A"))) }
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, local, remote, "n")
        val res = cache.listBySetIndex<String, E>("c", E::class, "type", "A")
        assertEquals(1, res.size)
    }

    @Test
    fun collectionReads_returnStableLocalInstancesAcrossCalls() {
        // LOCAL_REMOTE 下重复读同一实体必须返回同一对象引用——这是 HashCacheKit.isLocalCacheEnabled
        // 明确文档化的契约，HashCacheableBySecondaryAspect 也依赖它。
        // 成员集合取自远端（保证完整），但实例必须取自本地：若每次都用远端新反序列化的对象覆盖本地，
        // 两次读同一实体就会拿到不同实例。此前只有集成测试覆盖这一点，单测漏掉了。
        val local = RecordingHashCache()
        val remote = FreshInstanceHashCache()
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, local, remote, "n")

        val first = cache.listBySetIndex<String, E>("c", E::class, "type", "A")
        val second = cache.listBySetIndex<String, E>("c", E::class, "type", "A")

        assertEquals(1, first.size)
        assertSame(first.first(), second.first(), "重复读同一实体必须返回同一对象引用")
    }

    /** Remote stand-in that hands back a *new* instance on every read, as real deserialization does. */
    private class FreshInstanceHashCache : RecordingHashCache() {
        override fun <PK, E : IIdEntity<PK>> listBySetIndex(
            cacheName: String, entityClass: KClass<E>, property: String, value: Any
        ): List<E> {
            @Suppress("UNCHECKED_CAST")
            return listOf(E("1", "A") as E)
        }
    }

    @Test
    fun listBySetIndex_localRemote_partialLocalMustNotShadowRemote() {
        val local = RecordingHashCache().apply { putSetIndex("type", "A", listOf(E("1", "A"))) }
        val remote = RecordingHashCache().apply { putSetIndex("type", "A", listOf(E("1", "A"), E("2", "A"))) }
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, local, remote, "n")
        assertEquals(2, cache.listBySetIndex<String, E>("c", E::class, "type", "A").size, "应以远端为准")
    }

    @Test
    fun listBySetIndex_localRemote_remoteEmpty() {
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, RecordingHashCache(), null, "n")
        assertTrue(cache.listBySetIndex<String, E>("c", E::class, "type", "A").isEmpty())
    }

    @Test
    fun listBySetIndex_nonLocalRemote() {
        val remote = RecordingHashCache().apply { putSetIndex("type", "A", listOf(E("1", "A"))) }
        assertEquals(1, MixHashCache("c", CacheStrategy.REMOTE, null, remote, "n")
            .listBySetIndex<String, E>("c", E::class, "type", "A").size)
    }

    @Test
    fun listPageByZSetIndex_localRemote_backfill() {
        val local = RecordingHashCache()
        val remote = RecordingHashCache().apply { putZSet("score", listOf(E("1"))) }
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, local, remote, "n")
        assertEquals(1, cache.listPageByZSetIndex<String, E>("c", E::class, "score", 0, 10, true).size)
    }

    @Test
    fun listPageByZSetIndex_localRemote_partialLocalMustNotShadowRemote() {
        val local = RecordingHashCache().apply { putZSet("score", listOf(E("1"))) }
        val remote = RecordingHashCache().apply { putZSet("score", listOf(E("1"), E("2"))) }
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, local, remote, "n")
        assertEquals(2, cache.listPageByZSetIndex<String, E>("c", E::class, "score", 0, 10, true).size, "应以远端为准")
    }

    @Test
    fun listPageByZSetIndex_localRemote_remoteEmpty() {
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, RecordingHashCache(), null, "n")
        assertTrue(cache.listPageByZSetIndex<String, E>("c", E::class, "score", 0, 10, true).isEmpty())
    }

    @Test
    fun listPageByZSetIndex_nonLocalRemote() {
        val remote = RecordingHashCache().apply { putZSet("score", listOf(E("1"))) }
        assertEquals(1, MixHashCache("c", CacheStrategy.REMOTE, null, remote, "n")
            .listPageByZSetIndex<String, E>("c", E::class, "score", 0, 10, true).size)
    }

    @Test
    fun list_localRemote_backfill() {
        val local = RecordingHashCache()
        val remote = RecordingHashCache().apply { put(E("1")) }
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, local, remote, "n")
        assertEquals(1, cache.list<String, E>("c", E::class, null, 1, 10).size)
        assertTrue(local.entities.containsKey("1"))
    }

    @Test
    fun list_localRemote_partialLocalMustNotShadowRemote() {
        val local = RecordingHashCache().apply { put(E("1")) }
        val remote = RecordingHashCache().apply { put(E("1")); put(E("2")) }
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, local, remote, "n")
        assertEquals(2, cache.list<String, E>("c", E::class, null, 1, 10).size, "应以远端为准")
    }

    @Test
    fun list_localRemote_remoteEmpty() {
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, RecordingHashCache(), null, "n")
        assertTrue(cache.list<String, E>("c", E::class, null, 1, 10).isEmpty())
    }

    @Test
    fun list_nonLocalRemote() {
        val remote = RecordingHashCache().apply { put(E("1")) }
        assertEquals(1, MixHashCache("c", CacheStrategy.REMOTE, null, remote, "n")
            .list<String, E>("c", E::class, null, 1, 10).size)
    }

    // endregion

    // region refreshAll / clear

    @Test
    fun refreshAll_localRemote_notifiesNull() {
        val local = RecordingHashCache()
        val remote = RecordingHashCache()
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, local, remote, "n")
        cache.refreshAll("c", listOf(E("1")), emptySet(), emptySet())
        assertTrue(remote.entities.containsKey("1"))
        assertEquals(1, handler.size())
        assertNull(handler.messages().single().key)
    }

    @Test
    fun refreshAll_singleLocal() {
        val local = RecordingHashCache()
        MixHashCache("c", CacheStrategy.SINGLE_LOCAL, local, null, "n").refreshAll("c", listOf(E("1")))
        assertTrue(local.entities.containsKey("1"))
    }

    @Test
    fun refreshAll_remote() {
        val remote = RecordingHashCache()
        MixHashCache("c", CacheStrategy.REMOTE, null, remote, "n").refreshAll("c", listOf(E("1")))
        assertTrue(remote.entities.containsKey("1"))
    }

    @Test
    fun clear_localRemote_notifiesNull() {
        val local = RecordingHashCache().apply { put(E("1")) }
        val remote = RecordingHashCache().apply { put(E("1")) }
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, local, remote, "n")
        cache.clear("c")
        assertTrue(remote.entities.isEmpty())
        assertEquals(1, handler.size())
        assertNull(handler.messages().single().key)
    }

    @Test
    fun clear_singleLocal() {
        val local = RecordingHashCache().apply { put(E("1")) }
        MixHashCache("c", CacheStrategy.SINGLE_LOCAL, local, null, "n").clear("c")
        assertTrue(local.entities.isEmpty())
    }

    @Test
    fun clear_remote() {
        val remote = RecordingHashCache().apply { put(E("1")) }
        MixHashCache("c", CacheStrategy.REMOTE, null, remote, "n").clear("c")
        assertTrue(remote.entities.isEmpty())
    }

    @Test
    fun noHandlers_notifyIsNoOp() {
        // remove the handler bean -> pushHashNotify short-circuits on empty handlers
        ctx.close()
        ctx = StaticApplicationContext().apply { refresh() }
        SpringKit.applicationContext = ctx
        val cache = MixHashCache("c", CacheStrategy.LOCAL_REMOTE, RecordingHashCache(), RecordingHashCache(), "n")
        cache.save("c", E("1")) // must not throw despite no handlers
    }

    // endregion

    // ---- helpers ----

    private class RecordingHandler : ICacheMessageHandler {
        private val received = Collections.synchronizedList(mutableListOf<CacheMessage>())
        fun messages(): List<CacheMessage> = received.toList()
        fun size(): Int = received.size
        fun keys(): List<Any?> = received.map { it.key }
        override fun sendMessage(message: CacheMessage) { received.add(message) }
        override fun receiveMessage(message: CacheMessage) {}
    }

    /**
     * In-memory IHashCache backed by [entities]; secondary-index queries are derived from the live entity
     * set by reflecting the named property, so that backfilling from remote (which only re-puts entities)
     * makes subsequent local index queries hit — matching the production index-rebuild behaviour.
     */
    private open class RecordingHashCache : IHashCache {
        val entities = linkedMapOf<Any?, IIdEntity<*>>()
        fun put(e: IIdEntity<*>) { entities[e.id] = e }
        fun putSetIndex(@Suppress("UNUSED_PARAMETER") prop: String, @Suppress("UNUSED_PARAMETER") value: Any, list: List<IIdEntity<*>>) {
            list.forEach { entities[it.id] = it }
        }
        fun putZSet(@Suppress("UNUSED_PARAMETER") name: String, list: List<IIdEntity<*>>) {
            list.forEach { entities[it.id] = it }
        }

        private fun propValue(entity: IIdEntity<*>, property: String): Any? {
            val getter = entity.javaClass.methods.firstOrNull {
                it.parameterCount == 0 && it.name == "get" + property.replaceFirstChar { c -> c.uppercase() }
            } ?: return null
            return getter.invoke(entity)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <PK, E : IIdEntity<PK>> getById(cacheName: String, id: PK, entityClass: KClass<E>): E? =
            entities[id] as E?
        override fun existsById(cacheName: String, id: Any): Boolean = entities.containsKey(id)
        /** Index property sets seen on the most recent write, so tests can assert what a backfill asked for. */
        var lastFilterable: Set<String> = emptySet()
        var lastSortable: Set<String> = emptySet()

        override fun <PK, E : IIdEntity<PK>> save(cacheName: String, entity: E, filterableProperties: Set<String>, sortableProperties: Set<String>) {
            entities[entity.id] = entity
            lastFilterable = filterableProperties
            lastSortable = sortableProperties
        }
        override fun <PK, E : IIdEntity<PK>> saveBatch(cacheName: String, entities: List<E>, filterableProperties: Set<String>, sortableProperties: Set<String>) {
            entities.forEach { this.entities[it.id] = it }
            lastFilterable = filterableProperties
            lastSortable = sortableProperties
        }
        override fun <PK, E : IIdEntity<PK>> deleteById(cacheName: String, id: PK, entityClass: KClass<E>, filterableProperties: Set<String>, sortableProperties: Set<String>) {
            entities.remove(id)
        }
        @Suppress("UNCHECKED_CAST")
        override fun <E : IIdEntity<*>> findByIds(cacheName: String, ids: Collection<*>, entityClass: KClass<E>): List<E> =
            ids.mapNotNull { entities[it] } as List<E>
        @Suppress("UNCHECKED_CAST")
        override fun <PK, E : IIdEntity<PK>> listAll(cacheName: String, entityClass: KClass<E>): List<E> =
            entities.values.toList() as List<E>
        @Suppress("UNCHECKED_CAST")
        override fun <PK, E : IIdEntity<PK>> listBySetIndex(cacheName: String, entityClass: KClass<E>, property: String, value: Any): List<E> =
            entities.values.filter { propValue(it, property) == value } as List<E>
        @Suppress("UNCHECKED_CAST")
        override fun <PK, E : IIdEntity<PK>> listPageByZSetIndex(cacheName: String, entityClass: KClass<E>, zsetIndexName: String, offset: Long, limit: Long, desc: Boolean): List<E> =
            entities.values.toList() as List<E>
        @Suppress("UNCHECKED_CAST")
        override fun <PK, E : IIdEntity<PK>> list(cacheName: String, entityClass: KClass<E>, criteria: Criteria?, pageNo: Int, pageSize: Int, vararg orders: Order): List<E> =
            entities.values.toList() as List<E>
        override fun <PK, E : IIdEntity<PK>> refreshAll(cacheName: String, entities: List<E>, filterableProperties: Set<String>, sortableProperties: Set<String>) {
            this.entities.clear(); entities.forEach { this.entities[it.id] = it }
        }
        override fun clear(cacheName: String) { entities.clear() }
    }

    /** Throws on every method — proves a code path is not reached. */
    private class ThrowingHashCache : IHashCache {
        override fun <PK, E : IIdEntity<PK>> getById(cacheName: String, id: PK, entityClass: KClass<E>): E? = error("must not be called")
        override fun existsById(cacheName: String, id: Any): Boolean = error("must not be called")
        override fun <PK, E : IIdEntity<PK>> save(cacheName: String, entity: E, filterableProperties: Set<String>, sortableProperties: Set<String>) = error("must not be called")
        override fun <PK, E : IIdEntity<PK>> saveBatch(cacheName: String, entities: List<E>, filterableProperties: Set<String>, sortableProperties: Set<String>) = error("must not be called")
        override fun <PK, E : IIdEntity<PK>> deleteById(cacheName: String, id: PK, entityClass: KClass<E>, filterableProperties: Set<String>, sortableProperties: Set<String>) = error("must not be called")
        override fun <E : IIdEntity<*>> findByIds(cacheName: String, ids: Collection<*>, entityClass: KClass<E>): List<E> = error("must not be called")
        override fun <PK, E : IIdEntity<PK>> listAll(cacheName: String, entityClass: KClass<E>): List<E> = error("must not be called")
        override fun <PK, E : IIdEntity<PK>> listBySetIndex(cacheName: String, entityClass: KClass<E>, property: String, value: Any): List<E> = error("must not be called")
        override fun <PK, E : IIdEntity<PK>> listPageByZSetIndex(cacheName: String, entityClass: KClass<E>, zsetIndexName: String, offset: Long, limit: Long, desc: Boolean): List<E> = error("must not be called")
        override fun <PK, E : IIdEntity<PK>> list(cacheName: String, entityClass: KClass<E>, criteria: Criteria?, pageNo: Int, pageSize: Int, vararg orders: Order): List<E> = error("must not be called")
        override fun <PK, E : IIdEntity<PK>> refreshAll(cacheName: String, entities: List<E>, filterableProperties: Set<String>, sortableProperties: Set<String>) = error("must not be called")
        override fun clear(cacheName: String) = error("must not be called")
    }
}
