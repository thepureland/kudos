package io.kudos.ability.cache.remote.redis

import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.data.memdb.redis.dao.IdEntitiesRedisHashDao
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [RedisHashCache].
 *
 * Replaces the internally-constructed [IdEntitiesRedisHashDao] with a mock (reflection) and verifies
 * every public method delegates to the dao with the version-prefixed cache key, including:
 * - read paths (getById / existsById / findByIds / listAll / listBySetIndex / listPageByZSetIndex / list)
 * - write paths (save / saveBatch / deleteById / refreshAll / clear)
 * - deleteByIds: empty-collection early return (no dao interaction) vs per-id delegation
 *
 * @author K
 * @since 1.0.0
 */
internal class RedisHashCacheTest {

    private data class Row(override var id: String = "") : IIdEntity<String>

    private lateinit var dao: IdEntitiesRedisHashDao
    private lateinit var cache: RedisHashCache
    private lateinit var versionConfig: CacheVersionConfig

    private val cacheName = "hashName"
    private val prefixed: String get() = versionConfig.getFinalCacheName(cacheName)
    private val filterable = setOf("type")
    private val sortable = setOf("score")

    @BeforeTest
    fun setUp() {
        versionConfig = CacheVersionConfig() // default version "default"
        cache = RedisHashCache(mock(RedisTemplates::class.java), versionConfig)
        dao = mock(IdEntitiesRedisHashDao::class.java)
        val field = RedisHashCache::class.java.getDeclaredField("dao")
        field.isAccessible = true
        field.set(cache, dao)
    }

    @Test
    fun getById_delegatesWithVersionPrefixedKey() {
        val row = Row("1")
        `when`(dao.getById(prefixed, "1", Row::class)).thenReturn(row)
        assertEquals(row, cache.getById(cacheName, "1", Row::class))
        assertNull(cache.getById(cacheName, "absent", Row::class))
    }

    @Test
    fun existsById_delegates() {
        `when`(dao.existsById(prefixed, "1")).thenReturn(true)
        assertTrue(cache.existsById(cacheName, "1"))
        assertFalse(cache.existsById(cacheName, "2"))
    }

    @Test
    fun save_delegates() {
        val row = Row("1")
        cache.save(cacheName, row, filterable, sortable)
        verify(dao).save(prefixed, row, filterable, sortable)
    }

    @Test
    fun saveBatch_delegates() {
        val rows = listOf(Row("1"), Row("2"))
        cache.saveBatch(cacheName, rows, filterable, sortable)
        verify(dao).saveBatch(prefixed, rows, filterable, sortable)
    }

    @Test
    fun deleteById_delegates() {
        cache.deleteById(cacheName, "1", Row::class, filterable, sortable)
        verify(dao).deleteById(prefixed, "1", Row::class, filterable, sortable)
    }

    @Test
    fun deleteByIds_emptyCollection_isNoOp() {
        cache.deleteByIds(cacheName, emptyList<String>(), Row::class, filterable, sortable)
        verifyNoInteractions(dao)
    }

    @Test
    fun deleteByIds_delegatesPerId() {
        cache.deleteByIds(cacheName, listOf("1", "2"), Row::class, filterable, sortable)
        verify(dao).deleteById(prefixed, "1", Row::class, filterable, sortable)
        verify(dao).deleteById(prefixed, "2", Row::class, filterable, sortable)
    }

    @Test
    fun findByIds_delegates() {
        val rows = listOf(Row("1"))
        `when`(dao.findByIds(prefixed, listOf("1"), Row::class)).thenReturn(rows)
        assertEquals(rows, cache.findByIds(cacheName, listOf("1"), Row::class))
    }

    @Test
    fun listAll_delegates() {
        val rows = listOf(Row("1"), Row("2"))
        `when`(dao.listAll(prefixed, Row::class)).thenReturn(rows)
        assertEquals(rows, cache.listAll(cacheName, Row::class))
    }

    @Test
    fun listBySetIndex_delegates() {
        val rows = listOf(Row("1"))
        `when`(dao.listBySetIndex(prefixed, Row::class, "type", 1)).thenReturn(rows)
        assertEquals(rows, cache.listBySetIndex(cacheName, Row::class, "type", 1))
    }

    @Test
    fun listPageByZSetIndex_delegates() {
        val rows = listOf(Row("3"), Row("2"))
        `when`(dao.listPageByZSetIndex(prefixed, Row::class, "score", 0L, 2L, true)).thenReturn(rows)
        assertEquals(rows, cache.listPageByZSetIndex(cacheName, Row::class, "score", 0L, 2L, desc = true))
    }

    @Test
    fun list_delegatesWithCriteriaPagingAndOrders() {
        val rows = listOf(Row("1"))
        val order = Order.desc("score")
        // concrete arguments only: Kotlin's non-null parameter types reject Mockito matcher placeholders
        `when`(dao.list(prefixed, Row::class, null, 1, 10, order)).thenReturn(rows)
        assertEquals(rows, cache.list(cacheName, Row::class, null as Criteria?, 1, 10, order))
    }

    @Test
    fun refreshAll_delegates() {
        val rows = listOf(Row("1"))
        cache.refreshAll(cacheName, rows, filterable, sortable)
        verify(dao).refreshAll(prefixed, rows, filterable, sortable)
    }

    @Test
    fun clear_delegates() {
        cache.clear(cacheName)
        verify(dao).clear(prefixed)
    }
}
