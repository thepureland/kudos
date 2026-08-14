package io.kudos.ability.cache.common.core.keyvalue

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.model.payload.MutableListSearchPayload
import io.kudos.base.support.dao.IBaseReadOnlyDao
import org.mockito.Mockito
import org.springframework.cache.Cache
import org.springframework.cache.concurrent.ConcurrentMapCache
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [AbstractByIdCacheHandler]: db-backed loading (getById/getByIds), reloadAll, and the
 * insert/update/delete cache-sync hooks. The DAO is mocked; the cache layer is stubbed through
 * [KeyValueCacheKit.overrideForTesting].
 *
 * @author K
 * @since 1.0.0
 */
internal class AbstractByIdCacheHandlerTest {

    private val cacheName = "BY_ID_TEST"
    private lateinit var cacheManager: TestMixCacheManager
    private lateinit var provider: TestProvider

    @BeforeTest
    fun setup() {
        cacheManager = TestMixCacheManager()
        provider = TestProvider()
        KeyValueCacheKit.overrideForTesting(cacheManager, provider)
    }

    @AfterTest
    fun teardown() {
        KeyValueCacheKit.resetForTesting()
    }

    // region reloadAll

    @Test
    fun reloadAll_cacheInactive_doesNothing() {
        provider.register(cacheName, active = false)
        val handler = newHandler(searchAll = listOf(TestEntity("1")))
        handler.reloadAll(true)
        assertNull(cacheManager.underlying(cacheName).get("1"))
    }

    @Test
    fun reloadAll_active_loadsAllIntoCache_andSkipsBlankIds() {
        provider.register(cacheName, active = true)
        val handler = newHandler(searchAll = listOf(TestEntity("1"), TestEntity("2"), TestEntity("  ")))
        handler.reloadAll(true)
        assertEquals(TestEntity("1"), cacheManager.underlying(cacheName).get("1")?.get())
        assertEquals(TestEntity("2"), cacheManager.underlying(cacheName).get("2")?.get())
        // blank id is skipped
        assertNull(cacheManager.underlying(cacheName).get("  "))
    }

    @Test
    fun reloadAll_withClear_clearsBeforeLoading() {
        provider.register(cacheName, active = true)
        cacheManager.underlying(cacheName).put("stale", "old")
        val handler = newHandler(searchAll = listOf(TestEntity("1")))
        handler.reloadAll(clear = true)
        assertNull(cacheManager.underlying(cacheName).get("stale"), "stale entry should be cleared")
        assertEquals(TestEntity("1"), cacheManager.underlying(cacheName).get("1")?.get())
    }

    @Test
    fun reloadAll_nonStringId_isCachedUnderStringKey() {
        // 非 String 主键（这里是 Long）也必须以 String 形式作缓存键。
        // 缓存按相等性匹配，1L 与 "1" 是两个不同条目：预热若写 Long 键，而 doReload(key: String)
        // / getByIds / @BatchCacheable 这几条 String 路径写的是 "1"，同一条数据会出现两份，
        // 失效也会漏掉其中一份。PK=String 时两种形式恰好重合，所以这个问题一直被掩盖。
        provider.register(cacheName, active = true)
        val dao = Mockito.mock(LongDao::class.java, Mockito.withSettings().defaultAnswer { inv ->
            if (inv.method.name == "search") listOf(LongEntity(1L), LongEntity(2L)) else null
        })
        val handler = LongHandler(cacheName)
        val f = AbstractByIdCacheHandler::class.java.getDeclaredField("dao")
        f.isAccessible = true
        f.set(handler, dao)
        handler.reloadAll(true)
        assertEquals(LongEntity(1L), cacheManager.underlying(cacheName).get("1")?.get())
        assertEquals(LongEntity(2L), cacheManager.underlying(cacheName).get("2")?.get())
        assertNull(cacheManager.underlying(cacheName).get(1L), "不应再以原始 Long 类型作为键")
    }

    @Test
    fun syncOnUpdate_nonStringId_evictsStringKey() {
        // 与 reloadAll 配对：写入用 String 键，失效也必须用 String 键，否则删不掉预热写入的条目。
        provider.register(cacheName, active = true)
        cacheManager.underlying(cacheName).put("7", LongEntity(7L))

        val dao = Mockito.mock(LongDao::class.java, Mockito.withSettings().defaultAnswer { null })
        val handler = LongHandler(cacheName)
        val f = AbstractByIdCacheHandler::class.java.getDeclaredField("dao")
        f.isAccessible = true
        f.set(handler, dao)

        handler.syncOnDelete(7L)

        assertNull(cacheManager.underlying(cacheName).get("7"), "删除同步应能命中 String 形式的键")
    }

    @Test
    fun reloadAll_withoutClear_keepsExisting() {
        provider.register(cacheName, active = true)
        cacheManager.underlying(cacheName).put("stale", "old")
        val handler = newHandler(searchAll = listOf(TestEntity("1")))
        handler.reloadAll(clear = false)
        assertEquals("old", cacheManager.underlying(cacheName).get("stale")?.get())
        assertEquals(TestEntity("1"), cacheManager.underlying(cacheName).get("1")?.get())
    }

    // endregion

    // region getById / getByIds (exposed via test subclass)

    @Test
    fun getById_returnsEntity() {
        provider.register(cacheName, active = true)
        val handler = newHandler(getResult = TestEntity("7"))
        assertEquals(TestEntity("7"), handler.exposeGetById("7"))
    }

    @Test
    fun getById_returnsNullWhenMissing() {
        provider.register(cacheName, active = true)
        val handler = newHandler(getResult = null)
        assertNull(handler.exposeGetById("404"))
    }

    @Test
    fun getById_blankStringId_throws() {
        provider.register(cacheName, active = true)
        val handler = newHandler()
        assertFailsWith<IllegalArgumentException> { handler.exposeGetById("") }
    }

    @Test
    fun getByIds_emptyCollection_throws() {
        provider.register(cacheName, active = true)
        val handler = newHandler()
        assertFailsWith<IllegalArgumentException> { handler.exposeGetByIds(emptyList()) }
    }

    @Test
    fun getByIds_mapsByUsableId_andSkipsBlank() {
        provider.register(cacheName, active = true)
        val handler = newHandler(searchByIds = listOf(TestEntity("1"), TestEntity("2"), TestEntity("  ")))
        val result = handler.exposeGetByIds(listOf("1", "2", "3"))
        assertEquals(setOf("1", "2"), result.keys)
        assertEquals(TestEntity("1"), result["1"])
    }

    // endregion

    // region sync hooks

    @Test
    fun syncOnInsert_writeInTime_callsDoReload() {
        provider.register(cacheName, active = true, writeInTime = true)
        val handler = newHandler()
        handler.syncOnInsert("9")
        assertEquals(listOf("9"), handler.doReloadKeys)
    }

    @Test
    fun syncOnInsert_notWriteInTime_skipsReload() {
        provider.register(cacheName, active = true, writeInTime = false)
        val handler = newHandler()
        handler.syncOnInsert("9")
        assertTrue(handler.doReloadKeys.isEmpty())
    }

    @Test
    fun syncOnInsert_inactive_skips() {
        provider.register(cacheName, active = false, writeInTime = true)
        val handler = newHandler()
        handler.syncOnInsert("9")
        assertTrue(handler.doReloadKeys.isEmpty())
    }

    @Test
    fun syncOnUpdate_evictsAndReloadsWhenWriteInTime() {
        provider.register(cacheName, active = true, writeInTime = true)
        cacheManager.underlying(cacheName).put("9", "old")
        val handler = newHandler()
        handler.syncOnUpdate("9")
        assertNull(cacheManager.underlying(cacheName).get("9"))
        assertEquals(listOf("9"), handler.doReloadKeys)
    }

    @Test
    fun syncOnUpdate_evictsOnlyWhenNotWriteInTime() {
        provider.register(cacheName, active = true, writeInTime = false)
        cacheManager.underlying(cacheName).put("9", "old")
        val handler = newHandler()
        handler.syncOnUpdate("9")
        assertNull(cacheManager.underlying(cacheName).get("9"))
        assertTrue(handler.doReloadKeys.isEmpty())
    }

    @Test
    fun syncOnUpdate_inactive_skips() {
        provider.register(cacheName, active = false)
        cacheManager.underlying(cacheName).put("9", "old")
        val handler = newHandler()
        handler.syncOnUpdate("9")
        assertEquals("old", cacheManager.underlying(cacheName).get("9")?.get())
    }

    @Test
    fun syncOnDelete_evicts() {
        provider.register(cacheName, active = true)
        cacheManager.underlying(cacheName).put("9", "old")
        val handler = newHandler()
        handler.syncOnDelete("9")
        assertNull(cacheManager.underlying(cacheName).get("9"))
    }

    @Test
    fun syncOnDelete_inactive_skips() {
        provider.register(cacheName, active = false)
        cacheManager.underlying(cacheName).put("9", "old")
        val handler = newHandler()
        handler.syncOnDelete("9")
        assertEquals("old", cacheManager.underlying(cacheName).get("9")?.get())
    }

    @Test
    fun syncOnBatchDelete_evictsAll() {
        provider.register(cacheName, active = true)
        cacheManager.underlying(cacheName).put("a", "1")
        cacheManager.underlying(cacheName).put("b", "2")
        val handler = newHandler()
        handler.syncOnBatchDelete(listOf("a", "b"))
        assertNull(cacheManager.underlying(cacheName).get("a"))
        assertNull(cacheManager.underlying(cacheName).get("b"))
    }

    @Test
    fun syncOnBatchDelete_inactive_skips() {
        provider.register(cacheName, active = false)
        cacheManager.underlying(cacheName).put("a", "1")
        val handler = newHandler()
        handler.syncOnBatchDelete(listOf("a"))
        assertEquals("1", cacheManager.underlying(cacheName).get("a")?.get())
    }

    // endregion

    // ---- helpers ----

    private fun newHandler(
        getResult: TestEntity? = null,
        searchAll: List<TestEntity> = emptyList(),
        searchByIds: List<TestEntity> = emptyList(),
    ): TestByIdHandler {
        // A single default-answer routes every DAO call, avoiding Mockito argument matchers (which clash with
        // Kotlin's non-null generic parameters). Only get(...) and search(payload,...) are exercised by the SUT.
        val dao = Mockito.mock(TestDao::class.java, Mockito.withSettings().defaultAnswer { inv ->
            when (inv.method.name) {
                "get" -> getResult
                "search" -> {
                    val first = inv.arguments.firstOrNull()
                    // reloadAll passes a payload WITHOUT criterions; getByIds passes one WITH an IN criterion.
                    if (first is MutableListSearchPayload && !first.getCriterions().isNullOrEmpty()) searchByIds
                    else searchAll
                }
                else -> Mockito.RETURNS_DEFAULTS.answer(inv)
            }
        })
        val handler = TestByIdHandler(cacheName)
        val field = AbstractByIdCacheHandler::class.java.getDeclaredField("dao")
        field.isAccessible = true
        field.set(handler, dao)
        return handler
    }

    /** Test entity. */
    data class TestEntity(override val id: String) : IIdEntity<String>

    /** Entity with a non-CharSequence (Long) id to cover toUsableId's else branch. */
    data class LongEntity(override val id: Long) : IIdEntity<Long>

    /** DAO type fixed to TestEntity so GenericKit can resolve the entity class from the superclass generics. */
    interface TestDao : IBaseReadOnlyDao<String, TestEntity>

    interface LongDao : IBaseReadOnlyDao<Long, LongEntity>

    private class LongHandler(private val name: String) :
        AbstractByIdCacheHandler<Long, LongEntity, LongDao>() {
        override fun cacheName(): String = name
        override fun itemDesc(): String = "long-entity"
        override fun doReload(key: String): LongEntity? = null
    }

    /** Concrete handler exposing the protected db helpers for direct testing. */
    private class TestByIdHandler(private val name: String) :
        AbstractByIdCacheHandler<String, TestEntity, TestDao>() {
        val doReloadKeys = mutableListOf<String>()
        override fun cacheName(): String = name
        override fun itemDesc(): String = "test-entity"
        override fun doReload(key: String): TestEntity? {
            doReloadKeys.add(key)
            return null
        }
        fun exposeGetById(id: String): TestEntity? = getById(id)
        fun exposeGetByIds(ids: Collection<String>): Map<String, TestEntity> = getByIds(ids)
    }

    private class TestMixCacheManager : MixCacheManager() {
        private val under = ConcurrentHashMap<String, Cache>()
        private val wrappers = ConcurrentHashMap<String, MixCache>()
        fun underlying(name: String): Cache = under.computeIfAbsent(name) { ConcurrentMapCache(it, true) }
        override fun getCache(name: String): Cache =
            wrappers.computeIfAbsent(name) { MixCache(CacheStrategy.REMOTE, null, underlying(it)) }
    }

    private class TestProvider : ICacheConfigProvider {
        private val configs = ConcurrentHashMap<String, CacheConfig>()
        fun register(name: String, active: Boolean, writeInTime: Boolean = false) {
            configs[name] = CacheConfig().apply {
                this.name = name; this.active = active; this.writeInTime = writeInTime
            }
        }
        override fun getCacheConfig(name: String): CacheConfig? = configs[name]
        override fun getAllCacheConfigs(): Map<String, CacheConfig> = configs
        override fun getLocalCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
    }
}
