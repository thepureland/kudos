package io.kudos.ability.cache.local.caffeine.hash

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.sort.Order
import io.kudos.base.support.IIdEntity
import io.kudos.test.common.init.EnableKudosTest
import org.junit.jupiter.api.BeforeEach
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 基于 Hash 存储结构的本地（Caffeine）缓存测试用例。
 * 通过 [HashCacheKit.getHashCache] 获取 "testHash" 缓存，覆盖 save/getById/deleteById/listAll/findByIds/listBySetIndex/listPageByZSetIndex/list/refreshAll 等。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnableKudosTest
@Import(HashTestCacheConfigProvider::class)
internal class LocalHashCacheTest {

    private val cacheName = "testHash"

    private val setIdx = setOf("type")
    private val zsetIdx = setOf("type", "sortScore")

    companion object {
        @DynamicPropertySource
        @JvmStatic
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.cache.enabled") { "true" }
            registry.add("cache.config.strategy") { CacheStrategy.SINGLE_LOCAL.name }
        }
    }

    @BeforeEach
    fun clearCache() {
        HashCacheKit.getHashCache(cacheName)?.refreshAll(cacheName, emptyList<TestRow>(), emptySet(), emptySet())
    }

    @Test
    fun getHashCacheNotNull() {
        val cache = HashCacheKit.getHashCache(cacheName)
        assertNotNull(cache)
    }

    @Test
    fun saveAndGetById() {
        val cache = HashCacheKit.getHashCache(cacheName)!!
        cache.save(cacheName, TestRow(id = "u1", name = "Alice", type = 1))
        val found = cache.getById(cacheName, "u1", TestRow::class)
        assertEquals("u1", found?.id)
        assertEquals("Alice", found?.name)
        assertEquals(1, found?.type)
    }

    @Test
    fun getByIdReturnsNullWhenMissing() {
        val cache = HashCacheKit.getHashCache(cacheName)!!
        val found = cache.getById(cacheName, "nonexistent", TestRow::class)
        assertNull(found)
    }

    @Test
    fun findByIds() {
        val cache = HashCacheKit.getHashCache(cacheName)!!
        cache.save(cacheName, TestRow(id = "u1", name = "A"))
        cache.save(cacheName, TestRow(id = "u2", name = "B"))
        cache.save(cacheName, TestRow(id = "u3", name = "C"))
        val list = cache.findByIds(cacheName, listOf("u1", "u3", "u99"), TestRow::class)
        assertEquals(2, list.size)
        assertTrue(list.any { it.id == "u1" && it.name == "A" })
        assertTrue(list.any { it.id == "u3" && it.name == "C" })
    }

    @Test
    fun findByIdsEmpty() {
        val cache = HashCacheKit.getHashCache(cacheName)!!
        val list = cache.findByIds(cacheName, emptyList<String>(), TestRow::class)
        assertTrue(list.isEmpty())
    }

    @Test
    fun listAll() {
        val cache = HashCacheKit.getHashCache(cacheName)!!
        cache.save(cacheName, TestRow(id = "u1", name = "A"))
        cache.save(cacheName, TestRow(id = "u2", name = "B"))
        val all = cache.listAll(cacheName, TestRow::class)
        assertEquals(2, all.size)
        assertTrue(all.any { it.id == "u1" })
        assertTrue(all.any { it.id == "u2" })
    }

    @Test
    fun deleteById() {
        val cache = HashCacheKit.getHashCache(cacheName)!!
        cache.save(cacheName, TestRow(id = "u1", name = "A"))
        assertEquals("A", cache.getById(cacheName, "u1", TestRow::class)?.name)
        cache.deleteById(cacheName, "u1", TestRow::class)
        assertNull(cache.getById(cacheName, "u1", TestRow::class))
    }

    @Test
    fun deleteById_withIndex() {
        val cache = HashCacheKit.getHashCache(cacheName)!!
        cache.save(cacheName, TestRowWithTime(id = "1", type = 1, sortScore = 100.0), setIdx, zsetIdx)
        cache.save(cacheName, TestRowWithTime(id = "2", type = 1, sortScore = 200.0), setIdx, zsetIdx)
        assertEquals(2, cache.listBySetIndex(cacheName, TestRowWithTime::class, "type", 1).size)
        cache.deleteById(cacheName, "1", TestRowWithTime::class, setIdx, zsetIdx)
        assertNull(cache.getById(cacheName, "1", TestRowWithTime::class))
        assertEquals(1, cache.listBySetIndex(cacheName, TestRowWithTime::class, "type", 1).size)
        assertEquals("2", cache.listBySetIndex(cacheName, TestRowWithTime::class, "type", 1).first().id)
    }

    @Test
    fun refreshAll() {
        val cache = HashCacheKit.getHashCache(cacheName)!!
        cache.save(cacheName, TestRow(id = "old", name = "Old"))
        val newList = listOf(
            TestRow(id = "a", name = "A"),
            TestRow(id = "b", name = "B")
        )
        cache.refreshAll(cacheName, newList)
        assertNull(cache.getById(cacheName, "old", TestRow::class))
        assertEquals("A", cache.getById(cacheName, "a", TestRow::class)?.name)
        assertEquals("B", cache.getById(cacheName, "b", TestRow::class)?.name)
        val all = cache.listAll(cacheName, TestRow::class)
        assertEquals(2, all.size)
    }

    @Test
    fun refreshAll_withIndex() {
        val cache = HashCacheKit.getHashCache(cacheName)!!
        cache.save(cacheName, TestRowWithTime(id = "old", type = 1, sortScore = 50.0), setIdx, zsetIdx)
        val newList = listOf(
            TestRowWithTime(id = "a", type = 1, sortScore = 100.0),
            TestRowWithTime(id = "b", type = 2, sortScore = 200.0)
        )
        cache.refreshAll(cacheName, newList, setIdx, zsetIdx)
        assertNull(cache.getById(cacheName, "old", TestRowWithTime::class))
        assertEquals(1, cache.listBySetIndex(cacheName, TestRowWithTime::class, "type", 1).size)
        assertEquals(1, cache.listBySetIndex(cacheName, TestRowWithTime::class, "type", 2).size)
        val page = cache.listPageByZSetIndex(cacheName, TestRowWithTime::class, "sortScore", 0, 2, desc = true)
        assertEquals("b", page[0].id)
        assertEquals("a", page[1].id)
    }

    @Test
    fun saveBatch_thenGetByIdAndListAll() {
        val cache = HashCacheKit.getHashCache(cacheName)!!
        val list = listOf(
            TestRow(id = "b1", name = "Batch1", type = 1),
            TestRow(id = "b2", name = "Batch2", type = 2),
            TestRow(id = "b3", name = "Batch3", type = 1)
        )
        cache.saveBatch(cacheName, list)
        assertEquals("Batch1", cache.getById(cacheName, "b1", TestRow::class)?.name)
        assertEquals("Batch2", cache.getById(cacheName, "b2", TestRow::class)?.name)
        assertEquals("Batch3", cache.getById(cacheName, "b3", TestRow::class)?.name)
        val all = cache.listAll(cacheName, TestRow::class)
        assertEquals(3, all.size)
    }

    @Test
    fun listBySetIndex() {
        val cache = HashCacheKit.getHashCache(cacheName)!!
        cache.save(cacheName, TestRowWithTime(id = "1", type = 1, sortScore = 100.0), setIdx, zsetIdx)
        cache.save(cacheName, TestRowWithTime(id = "2", type = 1, sortScore = 200.0), setIdx, zsetIdx)
        cache.save(cacheName, TestRowWithTime(id = "3", type = 2, sortScore = 150.0), setIdx, zsetIdx)
        val type1 = cache.listBySetIndex(cacheName, TestRowWithTime::class, "type", 1)
        assertEquals(2, type1.size)
        assertTrue(type1.all { it.type == 1 })
        val type2 = cache.listBySetIndex(cacheName, TestRowWithTime::class, "type", 2)
        assertEquals(1, type2.size)
        assertEquals("3", type2.first().id)
    }

    @Test
    fun listPageByZSetIndex() {
        val cache = HashCacheKit.getHashCache(cacheName)!!
        cache.save(cacheName, TestRowWithTime(id = "a", type = 0, sortScore = 10.0), setIdx, zsetIdx)
        cache.save(cacheName, TestRowWithTime(id = "b", type = 0, sortScore = 20.0), setIdx, zsetIdx)
        cache.save(cacheName, TestRowWithTime(id = "c", type = 0, sortScore = 30.0), setIdx, zsetIdx)
        val pageDesc = cache.listPageByZSetIndex(cacheName, TestRowWithTime::class, "sortScore", 0, 2, desc = true)
        assertEquals(2, pageDesc.size)
        assertEquals("c", pageDesc[0].id)
        assertEquals("b", pageDesc[1].id)
        val pageAsc = cache.listPageByZSetIndex(cacheName, TestRowWithTime::class, "sortScore", 0, 2, desc = false)
        assertEquals(2, pageAsc.size)
        assertEquals("a", pageAsc[0].id)
        assertEquals("b", pageAsc[1].id)
    }

    @Test
    fun list_noCriteria_firstPage() {
        val cache = HashCacheKit.getHashCache(cacheName)!!
        cache.save(cacheName, TestRowWithTime(id = "1", type = 1, sortScore = 100.0), setIdx, zsetIdx)
        cache.save(cacheName, TestRowWithTime(id = "2", type = 1, sortScore = 200.0), setIdx, zsetIdx)
        cache.save(cacheName, TestRowWithTime(id = "3", type = 2, sortScore = 150.0), setIdx, zsetIdx)
        val page = cache.list(cacheName, TestRowWithTime::class, null, 1, 2, Order.desc("sortScore"))
        assertEquals(2, page.size)
        assertEquals("2", page[0].id)
        assertEquals("3", page[1].id)
    }

    @Test
    fun list_withCriteria_andOrder() {
        val cache = HashCacheKit.getHashCache(cacheName)!!
        cache.save(cacheName, TestRowWithTime(id = "1", type = 1, sortScore = 100.0), setIdx, zsetIdx)
        cache.save(cacheName, TestRowWithTime(id = "2", type = 1, sortScore = 200.0), setIdx, zsetIdx)
        cache.save(cacheName, TestRowWithTime(id = "3", type = 2, sortScore = 150.0), setIdx, zsetIdx)
        val criteria = Criteria.of("type", OperatorEnum.EQ, 1)
        val page = cache.list(cacheName, TestRowWithTime::class, criteria, 1, 10, Order.desc("sortScore"))
        assertEquals(2, page.size)
        assertEquals("2", page[0].id)
        assertEquals("1", page[1].id)
    }

    @Test
    fun list_pagination() {
        val cache = HashCacheKit.getHashCache(cacheName)!!
        cache.save(cacheName, TestRowWithTime(id = "a", type = 0, sortScore = 10.0), setIdx, zsetIdx)
        cache.save(cacheName, TestRowWithTime(id = "b", type = 0, sortScore = 20.0), setIdx, zsetIdx)
        cache.save(cacheName, TestRowWithTime(id = "c", type = 0, sortScore = 30.0), setIdx, zsetIdx)
        val page1 = cache.list(cacheName, TestRowWithTime::class, null, 1, 2, Order.asc("sortScore"))
        assertEquals(2, page1.size)
        assertEquals("a", page1[0].id)
        assertEquals("b", page1[1].id)
        val page2 = cache.list(cacheName, TestRowWithTime::class, null, 2, 2, Order.asc("sortScore"))
        assertEquals(1, page2.size)
        assertEquals("c", page2[0].id)
    }
}

/** 简单测试实体 */
data class TestRow(
    override var id: String? = null,
    var name: String? = null,
    var type: Int? = null
) : IIdEntity<String>

/** 带 type 与 sortScore 的实体，用于二级索引测试 */
data class TestRowWithTime(
    override var id: String? = null,
    var type: Int? = null,
    var sortScore: Double? = null
) : IIdEntity<String>
