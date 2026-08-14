package io.kudos.ability.data.memdb.redis.dao

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.data.memdb.redis.dao.support.RedisHashEntitySpec
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.sort.Order
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import jakarta.annotation.Resource
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
 * Tests for [BoundIdEntitiesRedisHashDao] (based on RedisTestContainer).
 *
 * The point of the bound API is that the index property sets cannot disagree between write and delete, so
 * besides delegation coverage this asserts the index stays consistent across a save → update → delete cycle
 * without the caller ever repeating the spec.
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
internal class BoundIdEntitiesRedisHashDaoTest {

    @Resource
    private lateinit var redisTemplates: RedisTemplates

    private fun spec(prefix: String) = RedisHashEntitySpec(
        dataKeyPrefix = "rdb:test:BoundIdEntitiesHashDao:$prefix",
        entityClass = TestRowWithTime::class,
        filterableProperties = setOf("type"),
        sortableProperties = setOf("sortScore"),
    )

    private fun dao(prefix: String) = BoundIdEntitiesRedisHashDao(redisTemplates, spec(prefix))

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry?) {
            RedisTestContainer.startIfNeeded(registry)
        }
    }

    @Test
    fun blankDataKeyPrefix_isRejected() {
        assertFailsWith<IllegalArgumentException> {
            RedisHashEntitySpec(dataKeyPrefix = "  ", entityClass = TestRowWithTime::class)
        }
    }

    @Test
    fun saveAndRead_withoutRepeatingTheSpec() {
        val dao = dao("saveAndRead")
        dao.clear()
        dao.save(TestRowWithTime(id = "1", type = 1, sortScore = 100.0))
        assertEquals(100.0, dao.getById("1")?.sortScore)
        assertTrue(dao.existsById("1"))
        assertFalse(dao.existsById("ghost"))
        assertNull(dao.getById("ghost"))
        assertEquals(1, dao.listAll().size)
    }

    @Test
    fun saveBatch_findByIds_andIndexQueries() {
        val dao = dao("batch")
        dao.clear()
        dao.saveBatch(
            listOf(
                TestRowWithTime(id = "1", type = 1, sortScore = 100.0),
                TestRowWithTime(id = "2", type = 2, sortScore = 200.0),
                TestRowWithTime(id = "3", type = 1, sortScore = 300.0),
            )
        )
        assertEquals(setOf("1", "3"), dao.findByIds(listOf("1", "3")).map { it.id }.toSet())
        assertEquals(setOf("1", "3"), dao.listBySetIndex("type", 1).map { it.id }.toSet())
        assertEquals(
            listOf("3", "2"),
            dao.listPageByZSetIndex("sortScore", 0, 2, desc = true).map { it.id }
        )
        assertEquals(
            listOf("3", "1"),
            dao.list(Criteria.of("type", OperatorEnum.EQ, 1), 1, 10, Order.desc("sortScore")).map { it.id }
        )
    }

    /**
     * The failure mode the bound API exists to prevent: with the untyped DAO, a delete called with different
     * index property sets than the save leaves the id behind in the Set index. Here both sides read the same
     * spec, so the index cannot rot.
     */
    @Test
    fun indexStaysConsistent_acrossSaveUpdateDelete() {
        val dao = dao("indexConsistency")
        dao.clear()
        dao.save(TestRowWithTime(id = "1", type = 1, sortScore = 100.0))
        assertEquals(listOf("1"), dao.listBySetIndex("type", 1).map { it.id })

        // update moves it to another index entry
        dao.save(TestRowWithTime(id = "1", type = 2, sortScore = 150.0))
        assertTrue(dao.listBySetIndex("type", 1).isEmpty())
        assertEquals(listOf("1"), dao.listBySetIndex("type", 2).map { it.id })

        // delete removes it from both the hash and the index
        dao.deleteById("1")
        assertNull(dao.getById("1"))
        assertTrue(dao.listBySetIndex("type", 2).isEmpty())
        assertTrue(dao.listPageByZSetIndex("sortScore", 0, 10).isEmpty())
    }

    @Test
    fun refreshAll_rebuildsRowsAndIndexes() {
        val dao = dao("refreshAll")
        dao.clear()
        dao.save(TestRowWithTime(id = "old", type = 9, sortScore = 1.0))
        dao.refreshAll(
            listOf(
                TestRowWithTime(id = "a", type = 1, sortScore = 10.0),
                TestRowWithTime(id = "b", type = 1, sortScore = 20.0),
            )
        )
        assertNull(dao.getById("old"))
        assertTrue(dao.listBySetIndex("type", 9).isEmpty())
        assertEquals(setOf("a", "b"), dao.listBySetIndex("type", 1).map { it.id }.toSet())
        assertEquals(listOf("b", "a"), dao.listPageByZSetIndex("sortScore", 0, 10, desc = true).map { it.id })
    }

    @Test
    fun expireAll_coversMainKeyAndIndexKeys() {
        val dao = dao("expireAll")
        dao.clear()
        dao.save(TestRowWithTime(id = "1", type = 1, sortScore = 100.0))
        dao.expireAll(Duration.ofSeconds(600))
        val template = redisTemplates.defaultRedisTemplate
        val prefix = dao.spec.dataKeyPrefix
        assertTrue(template.getExpire(prefix) > 0)
        template.keys("$prefix:idx*").forEach {
            assertTrue(template.getExpire(it) > 0, "index key [$it] must carry the TTL too")
        }
    }

    @Test
    fun clear_removesEverything() {
        val dao = dao("clear")
        dao.save(TestRowWithTime(id = "1", type = 1, sortScore = 100.0))
        dao.clear()
        assertTrue(dao.listAll().isEmpty())
        assertTrue(dao.listBySetIndex("type", 1).isEmpty())
    }

    /** A custom delegate (e.g. one pointing at a non-default Redis instance) can be bound too. */
    @Test
    fun customDelegate_isUsed() {
        val spec = spec("customDelegate")
        val delegate = IdEntitiesRedisHashDao(redisTemplates)
        val dao = BoundIdEntitiesRedisHashDao(delegate, spec)
        dao.clear()
        dao.save(TestRowWithTime(id = "1", type = 1, sortScore = 100.0))
        // visible through the underlying untyped DAO at the spec's key
        assertEquals("1", delegate.getById(spec.dataKeyPrefix, "1", TestRowWithTime::class)?.id)
    }
}
