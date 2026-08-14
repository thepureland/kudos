package io.kudos.ability.data.memdb.redis.dao

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.sort.Order
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import jakarta.annotation.Resource
import org.mockito.Mockito
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test cases for IdEntitiesRedisHashDao (based on RedisTestContainer).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
internal class IdEntitiesRedisHashDaoTest {

    @Resource
    private lateinit var redisTemplates: RedisTemplates

    /** Each test uses an independent key prefix to avoid cross-test data pollution. */
    private fun key(prefix: String) = "rdb:test:IdEntitiesHashDao:$prefix"

    private fun dao(): IdEntitiesRedisHashDao = IdEntitiesRedisHashDao(redisTemplates)

    /** Index attributes for tests: `type` indexed as Set+ZSet, `sortScore` indexed as ZSet. */
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
        dao.deleteById(k, "u1", TestRow::class)  // no indexes, no index arguments required
        assertNull(dao.getById(k, "u1", TestRow::class))
    }

    /** Deleting a missing id is a no-op apart from the hash-field delete; existing rows are untouched. */
    @Test
    fun deleteById_missingEntity_isNoop() {
        val dao = dao()
        val k = key("deleteMissing")
        dao.save(k, TestRow(id = "keep", name = "K"))
        dao.deleteById(k, "ghost", TestRow::class, setIdx, zsetIdx)
        val all = dao.listAll(k, TestRow::class)
        assertEquals(1, all.size)
        assertEquals("keep", all[0].id)
    }

    /** When saving with indexes, delete must be passed the same set of index attributes to correctly remove them from the indexes. */
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
        dao.refreshAll(k, newList)  // no indexes; uses default empty set
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
        redisTemplates.defaultRedisTemplate.delete(k)
        dao.saveBatch(
            k,
            listOf(
                TestRow(id = "ok1", name = "OK", type = 1),
                TestRow(id = "", name = "Skipped", type = 0),
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

    // ---------- Secondary index (Set/ZSet) tests: index attribute set passed via method arguments ----------

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

    // ---------- list(criteria, pageNo, pageSize, orders) tests ----------

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

    @Test
    fun list_withoutOrders_pagesInNaturalOrder() {
        val dao = dao()
        val k = key("listNoOrders")
        dao.save(k, TestRowWithTime(id = "1", type = 1, sortScore = 100.0))
        dao.save(k, TestRowWithTime(id = "2", type = 1, sortScore = 200.0))
        dao.save(k, TestRowWithTime(id = "3", type = 2, sortScore = 150.0))
        val all = dao.list(k, TestRowWithTime::class, null, 1, 10)
        assertEquals(3, all.size)
        assertEquals(setOf("1", "2", "3"), all.map { it.id }.toSet())
        val page2 = dao.list(k, TestRowWithTime::class, null, 2, 2)
        assertEquals(1, page2.size)
    }

    @Test
    fun list_nonPositivePageArgs_areNormalizedToOne() {
        val dao = dao()
        val k = key("listPageArgNorm")
        dao.save(k, TestRowWithTime(id = "a", type = 0, sortScore = 10.0), setIdx, zsetIdx)
        dao.save(k, TestRowWithTime(id = "b", type = 0, sortScore = 20.0), setIdx, zsetIdx)
        val page = dao.list(k, TestRowWithTime::class, null, 0, 0, Order.asc("sortScore"))
        assertEquals(1, page.size)
        assertEquals("a", page[0].id)
    }

    /**
     * Sorted paging with criteria goes through a temporary Set + ZINTERSTORE. The scores must come from the
     * sort index alone (weight 0 on the candidate set), and every temporary key must be cleaned up.
     */
    @Test
    fun list_sortedWithCriteria_ordersCorrectly_andLeavesNoTempKeys() {
        val dao = dao()
        val k = key("listIntersect")
        dao.save(k, TestRowWithTime(id = "1", type = 1, sortScore = 300.0), setIdx, zsetIdx)
        dao.save(k, TestRowWithTime(id = "2", type = 1, sortScore = 100.0), setIdx, zsetIdx)
        dao.save(k, TestRowWithTime(id = "3", type = 2, sortScore = 400.0), setIdx, zsetIdx)
        dao.save(k, TestRowWithTime(id = "4", type = 1, sortScore = 200.0), setIdx, zsetIdx)
        val criteria = Criteria.of("type", OperatorEnum.EQ, 1)

        val desc = dao.list(k, TestRowWithTime::class, criteria, 1, 10, Order.desc("sortScore"))
        assertEquals(listOf("1", "4", "2"), desc.map { it.id })
        val asc = dao.list(k, TestRowWithTime::class, criteria, 1, 10, Order.asc("sortScore"))
        assertEquals(listOf("2", "4", "1"), asc.map { it.id })
        // paging happens inside Redis: page 2 of size 2 is the tail of the ordered candidate set
        val page2 = dao.list(k, TestRowWithTime::class, criteria, 2, 2, Order.desc("sortScore"))
        assertEquals(listOf("2"), page2.map { it.id })

        val leftovers = redisTemplates.defaultRedisTemplate.keys("$k:idx:tmp*")
        assertTrue(leftovers.isEmpty(), "temporary intersection keys must be deleted, found: $leftovers")
    }

    /** Without criteria the sort index is paged directly; the result must match the criteria-less semantics. */
    @Test
    fun list_sortedWithoutCriteria_pagesTheSortIndexDirectly() {
        val dao = dao()
        val k = key("listDirectRange")
        dao.save(k, TestRowWithTime(id = "a", type = 0, sortScore = 10.0), setIdx, zsetIdx)
        dao.save(k, TestRowWithTime(id = "b", type = 0, sortScore = 30.0), setIdx, zsetIdx)
        dao.save(k, TestRowWithTime(id = "c", type = 0, sortScore = 20.0), setIdx, zsetIdx)
        // desc: b(30), c(20), a(10) -> page 1 of size 2
        assertEquals(
            listOf("b", "c"),
            dao.list(k, TestRowWithTime::class, null, 1, 2, Order.desc("sortScore")).map { it.id }
        )
        // asc: a(10), c(20), b(30) -> page 2 of size 2 is the tail
        assertEquals(
            listOf("b"),
            dao.list(k, TestRowWithTime::class, null, 2, 2, Order.asc("sortScore")).map { it.id }
        )
    }

    /** Rows excluded from the sort index (non-numeric value) must not surface in a sorted query. */
    @Test
    fun list_sorted_skipsRowsMissingFromTheSortIndex() {
        val dao = dao()
        val k = key("listSortedMissingIndex")
        dao.save(k, MixedScoreRow(id = "num", score = 5), emptySet(), setOf("score"))
        dao.save(k, MixedScoreRow(id = "text", score = "abc"), emptySet(), setOf("score"))
        val sorted = dao.list(k, MixedScoreRow::class, null, 1, 10, Order.desc("score"))
        assertEquals(listOf("num"), sorted.map { it.id })
        // ...while an unsorted query still sees both
        assertEquals(2, dao.list(k, MixedScoreRow::class, null, 1, 10).size)
    }

    @Test
    fun list_criteriaMatchingNothing_returnsEmpty() {
        val dao = dao()
        val k = key("listEmptyCriteriaResult")
        dao.save(k, TestRowWithTime(id = "1", type = 1, sortScore = 100.0), setIdx, zsetIdx)
        val criteria = Criteria.of("type", OperatorEnum.EQ, 99)
        assertTrue(dao.list(k, TestRowWithTime::class, criteria, 1, 10, Order.desc("sortScore")).isEmpty())
    }

    @Test
    fun listBySetIndex_missingValue_returnsEmpty() {
        val dao = dao()
        val k = key("listBySetIndexMissing")
        dao.save(k, TestRowWithTime(id = "1", type = 1, sortScore = 100.0), setIdx, zsetIdx)
        assertTrue(dao.listBySetIndex(k, TestRowWithTime::class, "type", 42).isEmpty())
    }

    @Test
    fun listAll_missingKey_returnsEmpty() {
        assertTrue(dao().listAll(key("listAllMissing"), TestRow::class).isEmpty())
    }

    // ---------- existsById / clear / refreshAll(empty) ----------

    @Test
    fun existsById_doesNotDeserialize() {
        val dao = dao()
        val k = key("existsById")
        dao.save(k, TestRow(id = "e1", name = "E"))
        assertTrue(dao.existsById(k, "e1"))
        assertFalse(dao.existsById(k, "ghost"))
    }

    @Test
    fun clear_removesDataAndAllIndexes() {
        val dao = dao()
        val k = key("clear")
        dao.save(k, TestRowWithTime(id = "1", type = 1, sortScore = 100.0), setIdx, zsetIdx)
        dao.save(k, TestRowWithTime(id = "2", type = 2, sortScore = 200.0), setIdx, zsetIdx)
        dao.clear(k)
        assertTrue(dao.listAll(k, TestRowWithTime::class).isEmpty())
        assertTrue(dao.listBySetIndex(k, TestRowWithTime::class, "type", 1).isEmpty())
        assertTrue(dao.listPageByZSetIndex(k, TestRowWithTime::class, "sortScore", 0, 10).isEmpty())
    }

    // ---------- expireAll ----------

    @Test
    fun expireAll_setsTtlOnMainKeyAndEveryIndexKey() {
        // 只给主 key 设过期是有害的：Set/ZSet 索引会活得更久，继续吐出实体已消失的 id，
        // listBySetIndex 于是返回幻影成员。所以这个方法必须覆盖它拥有的全部 key。
        val dao = dao()
        val k = key("expireAllCoversAllKeys")
        dao.save(k, TestRowWithTime(id = "1", type = 1, sortScore = 100.0), setIdx, zsetIdx)
        dao.save(k, TestRowWithTime(id = "2", type = 2, sortScore = 200.0), setIdx, zsetIdx)

        val template = redisTemplates.defaultRedisTemplate
        val indexKeys = template.keys("$k:idx*")
        assertTrue(indexKeys.isNotEmpty(), "前置条件：应已建出索引 key")

        dao.expireAll(k, Duration.ofSeconds(600))

        val mainTtl = template.getExpire(k)
        assertTrue(mainTtl > 0, "主 key 应已设置 TTL，实际=$mainTtl")
        indexKeys.forEach { idxKey ->
            val ttl = template.getExpire(idxKey)
            assertTrue(ttl > 0, "索引 key [$idxKey] 也必须设置 TTL，否则会残留指向已消失实体的 id，实际=$ttl")
        }
    }

    @Test
    fun expireAll_rejectsNonPositiveTtl() {
        val dao = dao()
        assertFailsWith<IllegalArgumentException> { dao.expireAll(key("expireAllZero"), Duration.ZERO) }
        assertFailsWith<IllegalArgumentException> {
            dao.expireAll(key("expireAllNegative"), Duration.ofSeconds(-1))
        }
    }

    @Test
    fun expireAll_reappliedByLaterWrites_keepsRegionAlive() {
        // TTL 由每次写入重新施加，因此区域的存活时间是"自最后一次写入起 ttl"。
        val dao = dao()
        val k = key("expireAllRefresh")
        dao.save(k, TestRowWithTime(id = "1", type = 1, sortScore = 1.0), setIdx, zsetIdx)
        dao.expireAll(k, Duration.ofSeconds(2))

        val template = redisTemplates.defaultRedisTemplate
        val shortTtl = template.getExpire(k)
        assertTrue(shortTtl in 1..2, "应为短 TTL，实际=$shortTtl")

        dao.expireAll(k, Duration.ofSeconds(600))
        assertTrue(template.getExpire(k) > 2, "重新施加后 TTL 应被延长")
        assertEquals(1, dao.listAll(k, TestRowWithTime::class).size, "延长 TTL 不应影响数据本身")
    }

    @Test
    fun refreshAll_emptyList_clearsTable() {
        val dao = dao()
        val k = key("refreshAllEmpty")
        dao.save(k, TestRowWithTime(id = "1", type = 1, sortScore = 100.0), setIdx, zsetIdx)
        dao.refreshAll(k, emptyList<TestRowWithTime>(), setIdx, zsetIdx)
        assertTrue(dao.listAll(k, TestRowWithTime::class).isEmpty())
        assertTrue(dao.listBySetIndex(k, TestRowWithTime::class, "type", 1).isEmpty())
    }

    // ---------- id normalization / null id ----------

    @Test
    fun pkField_isTrimmedOnSaveAndQuery() {
        val dao = dao()
        val k = key("pkTrim")
        dao.save(k, TestRow(id = "  padded  ", name = "P"))
        assertEquals("P", dao.getById(k, "padded", TestRow::class)?.name)
        assertTrue(dao.existsById(k, " padded "))
    }

    @Test
    fun save_nullId_throwsIllegalArgument() {
        val ex = assertFailsWith<IllegalArgumentException> {
            dao().save(key("saveNullId"), NullableIdRow(id = null, name = "X"))
        }
        assertEquals("entity.id must not be null", ex.message)
    }

    @Test
    fun saveBatch_filtersNullIdEntities() {
        val dao = dao()
        val k = key("saveBatchNullIdEntity")
        dao.saveBatch(k, listOf(NullableIdRow(id = "n1", name = "N1"), NullableIdRow(id = null, name = "skip")))
        val all = dao.listAll(k, NullableIdRow::class)
        assertEquals(1, all.size)
        assertEquals("n1", all[0].id)
    }

    @Test
    fun saveBatch_allInvalidIds_isNoop() {
        val dao = dao()
        val k = key("saveBatchAllInvalid")
        dao.saveBatch(k, listOf(NullableIdRow(id = null), NullableIdRow(id = "   ")))
        assertTrue(dao.listAll(k, NullableIdRow::class).isEmpty())
    }

    // ---------- deserialization branches ----------

    /** A value stored as a raw JSON string must be parsed through the String branch of parseToEntity. */
    @Test
    fun getById_rawJsonStringValue_isParsed() {
        val k = key("rawJsonString")
        redisTemplates.defaultRedisTemplate.opsForHash<String, Any>()
            .put(k, "js1", """{"id":"js1","name":"FromJson","type":7}""")
        val found = dao().getById(k, "js1", TestRow::class)
        assertEquals("FromJson", found?.name)
        assertEquals(7, found?.type)
    }

    /** A corrupted row only logs a warning and is skipped; it must not break getById or listAll. */
    @Test
    fun corruptedRow_isSkippedInsteadOfFailing() {
        val k = key("corruptedRow")
        redisTemplates.defaultRedisTemplate.opsForHash<String, Any>().put(k, "bad", "this is not json {")
        dao().save(k, TestRow(id = "good", name = "G"))
        assertNull(dao().getById(k, "bad", TestRow::class))
        val all = dao().listAll(k, TestRow::class)
        assertEquals(1, all.size)
        assertEquals("good", all[0].id)
    }

    // ---------- getPropertyValue branches (field / getter / is-getter / missing) ----------

    /** Property declared on the superclass: getDeclaredField fails on the subclass, falls back to getXxx(). */
    @Test
    fun index_onInheritedProperty_resolvesViaGetter() {
        val dao = dao()
        val k = key("inheritedProp")
        val e = CategoryDerived().apply { id = "d1"; category = "books" }
        dao.save(k, e, setOf("category"))
        val found = dao.listBySetIndex(k, CategoryDerived::class, "category", "books")
        assertEquals(1, found.size)
        assertEquals("d1", found[0].id)
    }

    /** Value exposed only through an isXxx() method resolves via the boolean-getter fallback. */
    @Test
    fun index_onIsGetterOnlyProperty_resolvesViaIsMethod() {
        val dao = dao()
        val k = key("isGetterProp")
        val e = VipRow().apply { id = "v1" }
        dao.save(k, e, setOf("vip"))
        val found = dao.listBySetIndex(k, VipRow::class, "vip", true)
        assertEquals(1, found.size)
        assertEquals("v1", found[0].id)
    }

    /** Unknown index property names are silently skipped; the row itself is still saved. */
    @Test
    fun index_onMissingProperty_isSkipped() {
        val dao = dao()
        val k = key("missingProp")
        dao.save(k, TestRow(id = "m1", name = "M"), setOf("nonexistent"), setOf("alsoMissing"))
        assertTrue(dao.listBySetIndex(k, TestRow::class, "nonexistent", "whatever").isEmpty())
        assertEquals("M", dao.getById(k, "m1", TestRow::class)?.name)
    }

    /** Null property values produce no index entries. */
    @Test
    fun index_nullPropertyValue_isSkipped() {
        val dao = dao()
        val k = key("nullPropValue")
        dao.save(k, TestRow(id = "n1", name = null, type = null), setOf("type"), setOf("type"))
        assertTrue(dao.listBySetIndex(k, TestRow::class, "type", 1).isEmpty())
        assertEquals("n1", dao.getById(k, "n1", TestRow::class)?.id)
    }

    // ---------- non-numeric sortable values ----------

    /**
     * Non-numeric values are NOT written into the ZSet index (a sentinel score would silently corrupt the
     * sort order); the row itself is still saved and only numeric rows appear in the sorted index.
     */
    @Test
    fun zsetScore_nonNumericValues_areSkippedInsteadOfIndexed() {
        val dao = dao()
        val k = key("scoreFallback")
        dao.save(k, MixedScoreRow(id = "a-str", score = "abc"), emptySet(), setOf("score"))
        dao.save(k, MixedScoreRow(id = "b-bool", score = true), emptySet(), setOf("score"))
        dao.save(k, MixedScoreRow(id = "c-num", score = 5), emptySet(), setOf("score"))
        dao.save(k, MixedScoreRow(id = "d-numstr", score = "12.5"), emptySet(), setOf("score"))
        val asc = dao.listPageByZSetIndex(k, MixedScoreRow::class, "score", 0, 4, desc = false)
        assertEquals(listOf("c-num", "d-numstr"), asc.map { it.id })
        // the skipped rows are still readable through the main data
        assertEquals(4, dao.listAll(k, MixedScoreRow::class).size)
    }

    // ---------- stale index cleanup on update ----------

    /** Updating an indexed property must remove the id from the index entry of the previous value. */
    @Test
    fun save_updateChangingIndexedValue_removesStaleSetIndexEntry() {
        val dao = dao()
        val k = key("staleIndexOnUpdate")
        dao.save(k, TestRowWithTime(id = "1", type = 1, sortScore = 100.0), setIdx, zsetIdx)
        dao.save(k, TestRowWithTime(id = "1", type = 2, sortScore = 100.0), setIdx, zsetIdx)
        // the id must have moved from set:type:1 to set:type:2, not be present in both
        assertTrue(dao.listBySetIndex(k, TestRowWithTime::class, "type", 1).isEmpty())
        assertEquals(listOf("1"), dao.listBySetIndex(k, TestRowWithTime::class, "type", 2).map { it.id })
    }

    /** Same guarantee for saveBatch, whose previous versions are fetched in one HMGET. */
    @Test
    fun saveBatch_updateChangingIndexedValue_removesStaleSetIndexEntries() {
        val dao = dao()
        val k = key("staleIndexOnBatchUpdate")
        dao.saveBatch(
            k,
            listOf(
                TestRowWithTime(id = "1", type = 1, sortScore = 100.0),
                TestRowWithTime(id = "2", type = 1, sortScore = 200.0)
            ),
            setIdx, zsetIdx
        )
        dao.saveBatch(
            k,
            listOf(
                TestRowWithTime(id = "1", type = 3, sortScore = 100.0),
                TestRowWithTime(id = "2", type = 1, sortScore = 250.0)
            ),
            setIdx, zsetIdx
        )
        assertEquals(listOf("2"), dao.listBySetIndex(k, TestRowWithTime::class, "type", 1).map { it.id })
        assertEquals(listOf("1"), dao.listBySetIndex(k, TestRowWithTime::class, "type", 3).map { it.id })
    }

    /** When the new value no longer yields a score (null / non-numeric), the old ZSet member must be removed. */
    @Test
    fun save_updateDroppingSortableValue_removesStaleZSetMember() {
        val dao = dao()
        val k = key("staleZSetOnUpdate")
        dao.save(k, MixedScoreRow(id = "1", score = 5), emptySet(), setOf("score"))
        assertEquals(listOf("1"), dao.listPageByZSetIndex(k, MixedScoreRow::class, "score", 0, 10).map { it.id })
        dao.save(k, MixedScoreRow(id = "1", score = "abc"), emptySet(), setOf("score"))
        assertTrue(dao.listPageByZSetIndex(k, MixedScoreRow::class, "score", 0, 10).isEmpty())
    }

    // ---------- defensive null-return guards (mocked template) ----------

    /**
     * Spring Data's multiGet / (reverse)range can theoretically return null (e.g. inside a pipeline);
     * the dao must degrade to empty lists. Uses a Mockito template since a real Redis never returns null here.
     */
    @Test
    fun nullReturnsFromTemplate_degradeToEmptyLists() {
        @Suppress("UNCHECKED_CAST")
        val template = Mockito.mock(RedisTemplate::class.java) as RedisTemplate<Any, Any?>
        @Suppress("UNCHECKED_CAST")
        val hashOps = Mockito.mock(HashOperations::class.java) as HashOperations<Any, String, Any>
        Mockito.`when`(template.opsForHash<String, Any>()).thenReturn(hashOps)
        Mockito.`when`(hashOps.multiGet(Mockito.any(), Mockito.anyList())).thenReturn(null)
        @Suppress("UNCHECKED_CAST")
        val zsetOps = Mockito.mock(ZSetOperations::class.java) as ZSetOperations<Any, Any?>
        Mockito.`when`(template.opsForZSet()).thenReturn(zsetOps)
        Mockito.`when`(zsetOps.reverseRange(Mockito.any(), Mockito.anyLong(), Mockito.anyLong())).thenReturn(null)
        Mockito.`when`(zsetOps.range(Mockito.any(), Mockito.anyLong(), Mockito.anyLong())).thenReturn(null)

        val mockedDao = IdEntitiesRedisHashDao(RedisTemplates(mutableMapOf(), template))
        assertTrue(mockedDao.findByIds("k", listOf("x"), TestRow::class).isEmpty())
        assertTrue(mockedDao.listPageByZSetIndex("k", TestRow::class, "s", 0, 10, desc = true).isEmpty())
        assertTrue(mockedDao.listPageByZSetIndex("k", TestRow::class, "s", 0, 10, desc = false).isEmpty())
    }
}

/**
 * Simple test row entity.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class TestRow(
    override var id: String = "",
    var name: String? = null,
    var type: Int? = null
) : IIdEntity<String>

/**
 * Entity with `type` and `sortScore`, used for secondary index tests.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class TestRowWithTime(
    override var id: String = "",
    var type: Int? = null,
    var sortScore: Double? = null
) : IIdEntity<String>

/**
 * Entity with a nullable id, used for null-id branch tests.
 *
 * @author K
 * @since 1.0.0
 */
data class NullableIdRow(
    override var id: String? = null,
    var name: String? = null
) : IIdEntity<String?>

/**
 * Base entity declaring `category`, so that the property lives on the superclass.
 *
 * @author K
 * @since 1.0.0
 */
open class CategoryBase : IIdEntity<String> {
    override var id: String = ""
    var category: String? = null
}

/**
 * Subclass without own fields: getDeclaredField on it fails for `category`, forcing the getter fallback.
 *
 * @author K
 * @since 1.0.0
 */
class CategoryDerived : CategoryBase()

/**
 * Entity exposing a value only through an `isVip()` method (no field, no getVip), for the is-getter fallback.
 *
 * @author K
 * @since 1.0.0
 */
class VipRow : IIdEntity<String> {
    override var id: String = ""

    @Suppress("unused")
    fun isVip(): Boolean = true
}

/**
 * Entity with an `Any?` score property to drive the toDouble fallback branches.
 *
 * @author K
 * @since 1.0.0
 */
data class MixedScoreRow(
    override var id: String = "",
    var score: Any? = null
) : IIdEntity<String>
