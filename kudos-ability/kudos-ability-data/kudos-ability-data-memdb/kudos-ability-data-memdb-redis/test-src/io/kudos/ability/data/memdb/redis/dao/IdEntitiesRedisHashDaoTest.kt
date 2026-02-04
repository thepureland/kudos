package io.kudos.ability.data.memdb.redis.dao

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.sort.Order
import io.kudos.base.support.IIdEntity
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import jakarta.annotation.Resource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * IdEntitiesRedisHashDao 测试用例（基于 RedisTestContainer）
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
internal class IdEntitiesRedisHashDaoTest {

    @Resource
    private lateinit var redisTemplates: RedisTemplates

    /** 每个测试使用独立 key 前缀，避免测试间数据互相污染 */
    private fun key(prefix: String) = "rdb:test:IdEntitiesHashDao:$prefix"

    private fun dao(): IdEntitiesRedisHashDao = IdEntitiesRedisHashDao(redisTemplates)

    /** 测试用索引属性：type 建 Set+ZSet，sortScore 建 ZSet */
    private val setIdx = setOf("type")
    private val zsetIdx = setOf("type", "sortScore")

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry?) {
            RedisTestContainer.startIfNeeded(registry)
        }
    }

    @Test
    fun saveAndGetById() {
        val dao = dao()
        val k = key("saveAndGetById")
        val entity = TestRow(id = "u1", name = "Alice", type = 1)
        dao.save(k, entity)
        val found = dao.getById(k, "u1", TestRow::class)
        assertEquals("u1", found?.id)
        assertEquals("Alice", found?.name)
        assertEquals(1, found?.type)
    }

    @Test
    fun getByIdReturnsNullWhenMissing() {
        val dao = dao()
        val k = key("getByIdMissing")
        val found = dao.getById(k, "nonexistent", TestRow::class)
        assertNull(found)
    }

    @Test
    fun findByIds() {
        val dao = dao()
        val k = key("findByIds")
        dao.save(k, TestRow(id = "u1", name = "A"))
        dao.save(k, TestRow(id = "u2", name = "B"))
        dao.save(k, TestRow(id = "u3", name = "C"))
        val list = dao.findByIds(k, listOf("u1", "u3", "u99"), TestRow::class)
        assertEquals(2, list.size)
        assertTrue(list.any { it.id == "u1" && it.name == "A" })
        assertTrue(list.any { it.id == "u3" && it.name == "C" })
    }

    @Test
    fun findByIdsEmpty() {
        val dao = dao()
        val k = key("findByIdsEmpty")
        val list = dao.findByIds(k, emptyList<String>(), TestRow::class)
        assertTrue(list.isEmpty())
    }

    @Test
    fun listAll() {
        val dao = dao()
        val k = key("listAll")
        dao.save(k, TestRow(id = "u1", name = "A"))
        dao.save(k, TestRow(id = "u2", name = "B"))
        val all = dao.listAll(k, TestRow::class)
        assertEquals(2, all.size)
        assertTrue(all.any { it.id == "u1" })
        assertTrue(all.any { it.id == "u2" })
    }

    @Test
    fun deleteById() {
        val dao = dao()
        val k = key("deleteById")
        dao.save(k, TestRow(id = "u1", name = "A"))
        assertEquals("A", dao.getById(k, "u1", TestRow::class)?.name)
        dao.deleteById(k, "u1", TestRow::class)  // 未建索引，无需传索引参数
        assertNull(dao.getById(k, "u1", TestRow::class))
    }

    /** 带索引保存时，删除需传入相同的索引属性集合才能正确从索引中移除 */
    @Test
    fun deleteById_withIndex() {
        val dao = dao()
        val k = key("deleteByIdWithIndex")
        dao.save(k, TestRowWithTime(id = "1", type = 1, sortScore = 100.0), setIdx, zsetIdx)
        dao.save(k, TestRowWithTime(id = "2", type = 1, sortScore = 200.0), setIdx, zsetIdx)
        assertEquals(2, dao.listBySetIndex(k, TestRowWithTime::class, "type", 1).size)
        dao.deleteById(k, "1", TestRowWithTime::class, setIdx, zsetIdx)
        assertNull(dao.getById(k, "1", TestRowWithTime::class))
        assertEquals(1, dao.listBySetIndex(k, TestRowWithTime::class, "type", 1).size)
        assertEquals("2", dao.listBySetIndex(k, TestRowWithTime::class, "type", 1).first().id)
    }

    @Test
    fun refreshAll() {
        val dao = dao()
        val k = key("refreshAll")
        dao.save(k, TestRow(id = "old", name = "Old"))
        val newList = listOf(
            TestRow(id = "a", name = "A"),
            TestRow(id = "b", name = "B")
        )
        dao.refreshAll(k, newList)  // 不建索引，使用默认空集合
        assertNull(dao.getById(k, "old", TestRow::class))
        assertEquals("A", dao.getById(k, "a", TestRow::class)?.name)
        assertEquals("B", dao.getById(k, "b", TestRow::class)?.name)
        val all = dao.listAll(k, TestRow::class)
        assertEquals(2, all.size)
    }

    @Test
    fun refreshAll_withIndex() {
        val dao = dao()
        val k = key("refreshAllWithIndex")
        dao.save(k, TestRowWithTime(id = "old", type = 1, sortScore = 50.0), setIdx, zsetIdx)
        val newList = listOf(
            TestRowWithTime(id = "a", type = 1, sortScore = 100.0),
            TestRowWithTime(id = "b", type = 2, sortScore = 200.0)
        )
        dao.refreshAll(k, newList, setIdx, zsetIdx)
        assertNull(dao.getById(k, "old", TestRowWithTime::class))
        assertEquals(1, dao.listBySetIndex(k, TestRowWithTime::class, "type", 1).size)
        assertEquals(1, dao.listBySetIndex(k, TestRowWithTime::class, "type", 2).size)
        val page = dao.listPageByZSetIndex(k, TestRowWithTime::class, "sortScore", 0, 2, desc = true)
        assertEquals("b", page[0].id)
        assertEquals("a", page[1].id)
    }

    @Test
    fun saveBatch_thenGetByIdAndListAll() {
        val dao = dao()
        val k = key("saveBatch")
        val list = listOf(
            TestRow(id = "b1", name = "Batch1", type = 1),
            TestRow(id = "b2", name = "Batch2", type = 2),
            TestRow(id = "b3", name = "Batch3", type = 1)
        )
        dao.saveBatch(k, list)
        assertEquals("Batch1", dao.getById(k, "b1", TestRow::class)?.name)
        assertEquals("Batch2", dao.getById(k, "b2", TestRow::class)?.name)
        assertEquals("Batch3", dao.getById(k, "b3", TestRow::class)?.name)
        val all = dao.listAll(k, TestRow::class)
        assertEquals(3, all.size)
        assertTrue(all.any { it.id == "b1" && it.type == 1 })
        assertTrue(all.any { it.id == "b2" && it.type == 2 })
    }

    @Test
    fun saveBatch_emptyList() {
        val dao = dao()
        val k = key("saveBatchEmpty")
        dao.saveBatch(k, emptyList<TestRow>())
        val all = dao.listAll(k, TestRow::class)
        assertTrue(all.isEmpty())
    }

    @Test
    fun saveBatch_skipsNullId() {
        val dao = dao()
        val k = key("saveBatchNullId")
        dao.saveBatch(
            k,
            listOf(
                TestRow(id = "ok1", name = "OK", type = 1),
                TestRow(id = null, name = "Skipped", type = 0),
                TestRow(id = "ok2", name = "OK2", type = 2)
            )
        )
        val all = dao.listAll(k, TestRow::class)
        assertEquals(2, all.size)
        assertTrue(all.any { it.id == "ok1" })
        assertTrue(all.any { it.id == "ok2" })
    }

    @Test
    fun saveBatch_withIndex() {
        val dao = dao()
        val k = key("saveBatchWithIndex")
        dao.saveBatch(
            k,
            listOf(
                TestRowWithTime(id = "i1", type = 1, sortScore = 100.0),
                TestRowWithTime(id = "i2", type = 1, sortScore = 200.0),
                TestRowWithTime(id = "i3", type = 2, sortScore = 150.0)
            ),
            setIdx,
            zsetIdx
        )
        val type1 = dao.listBySetIndex(k, TestRowWithTime::class, "type", 1)
        assertEquals(2, type1.size)
        val type2 = dao.listBySetIndex(k, TestRowWithTime::class, "type", 2)
        assertEquals(1, type2.size)
        assertEquals("i3", type2.first().id)
        val page = dao.listPageByZSetIndex(k, TestRowWithTime::class, "sortScore", 0, 2, desc = true)
        assertEquals(2, page.size)
        assertEquals("i2", page[0].id)
        assertEquals("i3", page[1].id)
    }

    // ---------- 二级索引（Set/ZSet）测试：通过方法参数传入索引属性集合 ----------

    @Test
    fun listBySetIndex() {
        val dao = dao()
        val k = key("listBySetIndex")
        dao.save(k, TestRowWithTime(id = "1", type = 1, sortScore = 100.0), setIdx, zsetIdx)
        dao.save(k, TestRowWithTime(id = "2", type = 1, sortScore = 200.0), setIdx, zsetIdx)
        dao.save(k, TestRowWithTime(id = "3", type = 2, sortScore = 150.0), setIdx, zsetIdx)
        val type1 = dao.listBySetIndex(k, TestRowWithTime::class, "type", 1)
        assertEquals(2, type1.size)
        assertTrue(type1.all { it.type == 1 })
        val type2 = dao.listBySetIndex(k, TestRowWithTime::class, "type", 2)
        assertEquals(1, type2.size)
        assertEquals("3", type2.first().id)
    }

    @Test
    fun listPageByZSetIndex() {
        val dao = dao()
        val k = key("listPageByZSetIndex")
        dao.save(k, TestRowWithTime(id = "a", type = 0, sortScore = 10.0), setIdx, zsetIdx)
        dao.save(k, TestRowWithTime(id = "b", type = 0, sortScore = 20.0), setIdx, zsetIdx)
        dao.save(k, TestRowWithTime(id = "c", type = 0, sortScore = 30.0), setIdx, zsetIdx)
        val pageDesc = dao.listPageByZSetIndex(k, TestRowWithTime::class, "sortScore", 0, 2, desc = true)
        assertEquals(2, pageDesc.size)
        assertEquals("c", pageDesc[0].id)
        assertEquals("b", pageDesc[1].id)
        val pageAsc = dao.listPageByZSetIndex(k, TestRowWithTime::class, "sortScore", 0, 2, desc = false)
        assertEquals(2, pageAsc.size)
        assertEquals("a", pageAsc[0].id)
        assertEquals("b", pageAsc[1].id)
    }

    // ---------- list(criteria, pageNo, pageSize, orders) 测试 ----------

    @Test
    fun list_noCriteria_firstPage() {
        val dao = dao()
        val k = key("listNoCriteria")
        dao.save(k, TestRowWithTime(id = "1", type = 1, sortScore = 100.0), setIdx, zsetIdx)
        dao.save(k, TestRowWithTime(id = "2", type = 1, sortScore = 200.0), setIdx, zsetIdx)
        dao.save(k, TestRowWithTime(id = "3", type = 2, sortScore = 150.0), setIdx, zsetIdx)
        val page = dao.list(k, TestRowWithTime::class, null, 1, 2, Order.desc("sortScore"))
        assertEquals(2, page.size)
        assertEquals("2", page[0].id)
        assertEquals("3", page[1].id)
    }

    @Test
    fun list_withSetCriteria_andOrder() {
        val dao = dao()
        val k = key("listSetCriteria")
        dao.save(k, TestRowWithTime(id = "1", type = 1, sortScore = 100.0), setIdx, zsetIdx)
        dao.save(k, TestRowWithTime(id = "2", type = 1, sortScore = 200.0), setIdx, zsetIdx)
        dao.save(k, TestRowWithTime(id = "3", type = 2, sortScore = 150.0), setIdx, zsetIdx)
        val criteria = Criteria.of("type", OperatorEnum.EQ, 1)
        val page = dao.list(k, TestRowWithTime::class, criteria, 1, 10, Order.desc("sortScore"))
        assertEquals(2, page.size)
        assertEquals("2", page[0].id)
        assertEquals("1", page[1].id)
    }

    @Test
    fun list_pagination() {
        val dao = dao()
        val k = key("listPagination")
        dao.save(k, TestRowWithTime(id = "a", type = 0, sortScore = 10.0), setIdx, zsetIdx)
        dao.save(k, TestRowWithTime(id = "b", type = 0, sortScore = 20.0), setIdx, zsetIdx)
        dao.save(k, TestRowWithTime(id = "c", type = 0, sortScore = 30.0), setIdx, zsetIdx)
        val page1 = dao.list(k, TestRowWithTime::class, null, 1, 2, Order.asc("sortScore"))
        assertEquals(2, page1.size)
        assertEquals("a", page1[0].id)
        assertEquals("b", page1[1].id)
        val page2 = dao.list(k, TestRowWithTime::class, null, 2, 2, Order.asc("sortScore"))
        assertEquals(1, page2.size)
        assertEquals("c", page2[0].id)
    }
}

/** 简单测试行实体 */
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
