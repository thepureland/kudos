package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.ability.data.rdb.ktorm.table.TestTableKtorm
import io.kudos.ability.data.rdb.ktorm.table.TestTableKtormDao
import io.kudos.ability.data.rdb.ktorm.table.TestTableKtorms
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.SearchPayload
import io.kudos.base.support.payload.UpdatePayload
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.ktorm.dsl.eq
import org.ktorm.dsl.like
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * BaseDao测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
internal open class BaseCrudDaoTest {

    @Resource
    private lateinit var testTableDao: TestTableKtormDao


    //region Insert

    @Test
    @Transactional
    open fun insert() {
        class InsertPayload {
            var id: Int? = null
            var name: String? = null
            var height: Int? = null
        }

        val insertPayload = InsertPayload().apply {
            id = 12
            name = "name99"
            height = 199
        }

        assertNotNull(testTableDao.insert(insertPayload))
        assertEquals(12, testTableDao.allSearch().size)

        val entity = TestTableKtorm {
            id = 0
            name = "name0"
        }
        assertEquals(0, testTableDao.insert(entity))
        assertEquals(13, testTableDao.allSearch().size)
    }

    @Test
    @Transactional
    open fun insertOnly() {
        val entity = TestTableKtorm {
            id = 0
            name = "name0"
            weight = 70.0
            height = 175
        }
        val id = testTableDao.insertOnly(entity, TestTableKtorm::id.name, TestTableKtorm::name.name)
        assertEquals(0, id)
        val result = testTableDao.get(0)!!
        assert(result.weight == null)
        assert(result.height == null)
    }

    @Test
    @Transactional
    open fun insertExclude() {
        val entity = TestTableKtorm {
            id = 0
            name = "name0"
            weight = 70.0
            height = 175
        }
        val id = testTableDao.insertExclude(entity, TestTableKtorm::weight.name, TestTableKtorm::height.name)
        assertEquals(0, id)
        val result = testTableDao.get(0)!!
        assert(result.weight == null)
        assert(result.height == null)
    }

    @Test
    @Transactional
    open fun batchInsert() {
        // 实体
        val entities = listOf(
            TestTableKtorm {
                id = 21
                name = "name21"
            },
            TestTableKtorm {
                id = 22
                name = "name22"
            },
            TestTableKtorm {
                id = 23
                name = "name23"
            },
        )
        assertEquals(3, testTableDao.batchInsert(entities, 2))
        assertEquals(14, testTableDao.allSearch().size)

        // 插入项载体
        class InsertPayload {
            var id: Int? = null
            var name: String? = null
            var height: Int? = null
        }

        val payloads = listOf(
            InsertPayload().apply {
                id = 31
                name = "name31"
                height = 131
            },
            InsertPayload().apply {
                id = 32
                name = "name32"
                height = 132
            }
        )
        assertEquals(2, testTableDao.batchInsert(payloads))
        assertEquals(16, testTableDao.allSearch().size)
    }

    @Test
    @Transactional
    open fun batchInsertOnly() {
        val entities = listOf(
            TestTableKtorm {
                id = 21
                name = "name21"
                weight = 70.0
                height = 175
            },
            TestTableKtorm {
                id = 22
                name = "name22"
                weight = 70.0
                height = 175
            },
            TestTableKtorm {
                id = 23
                name = "name23"
                weight = 70.0
                height = 175
            },
        )
        val count = testTableDao.batchInsertOnly(entities, 4, TestTableKtorm::id.name, TestTableKtorm::name.name)
        assertEquals(3, count)
        assertEquals(14, testTableDao.allSearch().size)
        val result = testTableDao.get(21)!!
        assert(result.weight == null)
        assert(result.height == null)
    }

    @Test
    @Transactional
    open fun batchInsertExclude() {
        val entities = listOf(
            TestTableKtorm {
                id = 21
                name = "name21"
                weight = 70.0
                height = 175
            },
            TestTableKtorm {
                id = 22
                name = "name22"
                weight = 70.0
                height = 175
            },
            TestTableKtorm {
                id = 23
                name = "name23"
                weight = 70.0
                height = 175
            },
        )
        val count = testTableDao.batchInsertExclude(entities, 4, TestTableKtorm::weight.name, TestTableKtorm::height.name)
        assertEquals(3, count)
        assertEquals(14, testTableDao.allSearch().size)
        val result = testTableDao.get(21)!!
        assert(result.weight == null)
        assert(result.height == null)
    }
    //endregion Insert


    //region Update
    @Test
    @Transactional
    open fun update() {
        var entity = testTableDao.get(-1)!!
        entity.name = "name"
        val success = testTableDao.update(entity)
        assert(success)
        entity = testTableDao.get(-1)!!
        assertEquals("name", entity.name)
    }

    @Test
    @Transactional
    open fun updateWhen() {
        var entity = testTableDao.get(-1)!!
        entity.name = "name"

        // Criteria为空
        assertFailsWith<IllegalArgumentException> { testTableDao.updateWhen(entity, Criteria()) }

        // 满足Criteria条件
        var criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.EQ, "name1")
        assert(testTableDao.updateWhen(entity, criteria))
        entity = testTableDao.get(-1)!!
        assertEquals("name", entity.name)

        // 不满足Criteria条件
        entity.name = "name1"
        criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.EQ, "non-exists")
        assert(!testTableDao.updateWhen(entity, criteria))
        entity = testTableDao.get(-1)!!
        assertEquals("name", entity.name)
    }

    @Test
    @Transactional
    open fun updateProperties() {
        val properties = mapOf(TestTableKtorm::name.name to -2, TestTableKtorm::name.name to "new-name") // 主键应该要不会被更新
        assert(testTableDao.updateProperties(-1, properties))
        assertEquals("new-name", testTableDao.get(-1)!!.name)
        assertEquals("name2", testTableDao.get(-2)!!.name)
    }

    @Test
    @Transactional
    open fun updatePropertiesWhen() {
        // Criteria为空
        val properties = mapOf(TestTableKtorm::id.name to -2, TestTableKtorm::name.name to "name") // 主键应该要不会被更新
        assertFailsWith<IllegalArgumentException> { testTableDao.updatePropertiesWhen(-1, properties, Criteria()) }

        // 满足Criteria条件
        var criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.EQ, "name1")
        assert(testTableDao.updatePropertiesWhen(-1, properties, criteria))
        var entity = testTableDao.get(-1)!!
        assertEquals("name", entity.name)

        // 不满足Criteria条件
        criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.EQ, "non-exists")
        assert(!testTableDao.updatePropertiesWhen(-1, mapOf(TestTableKtorm::name.name to "name1"), criteria))
        entity = testTableDao.get(-1)!!
        assertEquals("name", entity.name)
    }

    @Test
    @Transactional
    open fun updateOnly() {
        val entity = testTableDao.get(-1)!!
        entity.name = "new-name"
        assert(testTableDao.updateOnly(entity, TestTableKtorm::name.name))
        assertEquals("new-name", testTableDao.get(-1)!!.name)
        assertEquals("name2", testTableDao.get(-2)!!.name)
    }

    @Test
    @Transactional
    open fun updateOnlyWhen() {
        var entity = testTableDao.get(-1)!!
        entity.name = "name"
        entity.weight = null

        // 满足Criteria条件
        var criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.EQ, "name1")
        assert(testTableDao.updateOnlyWhen(entity, criteria, TestTableKtorm::name.name))
        entity = testTableDao.get(-1)!!
        assertEquals("name", entity.name)
        assertEquals(56.5, entity.weight)

        // 不满足Criteria条件
        entity.name = "name1"
        criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.EQ, "non-exists")
        assert(!testTableDao.updateOnlyWhen(entity, criteria, TestTableKtorm::name.name))
        entity = testTableDao.get(-1)!!
        assertEquals("name", entity.name)
    }

    @Test
    @Transactional
    open fun updateExcludeProperties() {
        var entity = testTableDao.get(-1)!!
        entity.name = "name"
        entity.weight = null
        assert(testTableDao.updateExcludeProperties(entity, TestTableKtorm::weight.name))
        entity = testTableDao.get(-1)!!
        assertEquals("name", entity.name)
        assertEquals(56.5, entity.weight)
    }

    @Test
    @Transactional
    open fun updateExcludePropertiesWhen() {
        var entity = testTableDao.get(-1)!!
        entity.name = "name"
        entity.weight = null

        // 满足Criteria条件
        var criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.EQ, "name1")
        assert(testTableDao.updateExcludePropertiesWhen(entity, criteria, TestTableKtorm::weight.name))
        entity = testTableDao.get(-1)!!
        assertEquals("name", entity.name)
        assertEquals(56.5, entity.weight)

        // 不满足Criteria条件
        entity.name = "name1"
        criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.EQ, "non-exists")
        assert(!testTableDao.updateExcludePropertiesWhen(entity, criteria))
        entity = testTableDao.get(-1)!!
        assertEquals("name", entity.name)
    }

    @Test
    @Transactional
    open fun batchUpdate() {
        var entities = testTableDao.getByIds(setOf(-1, -2, -3))
        entities.forEach {
            it.name = "name"
        }
        val count = testTableDao.batchUpdate(entities)
        assertEquals(3, count)
        entities = testTableDao.getByIds(setOf(-1, -2, -3))
        assertEquals("name", entities[0].name)
        assertEquals("name", entities[1].name)
        assertEquals("name", entities[2].name)

        // 存在主键为null的实体
        assertFailsWith<IllegalArgumentException> { testTableDao.batchUpdate(listOf(TestTableKtorm {})) }
    }

    @Test
    @Transactional
    open fun batchUpdateWhen() {
        val entities = listOf(
            TestTableKtorm {
                id = -1
                name = "11"
            },
            TestTableKtorm {
                id = -2
                name = "12"
            }
        )

        // 满足Criteria条件
        var criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.EQ, "unexists")
        assertEquals(0, testTableDao.batchUpdateWhen(entities, criteria))

        // 不满足Criteria条件
        criteria = Criteria.of("name", OperatorEnum.EQ, "name2")
        assertEquals(1, testTableDao.batchUpdateWhen(entities, criteria))
    }

    @Test
    @Transactional
    open fun batchUpdateWhenByUpdatePayload() {
        class SearchPayload1 : SearchPayload() {
            var name: String? = null
            var weight: Double? = null
            var noExistProp: String? = "noExistProp"
            override var returnProperties: List<String>? = listOf("id", "name", "height")
        }

        class UpdatePayload1 : UpdatePayload<SearchPayload1>() {
            var name: String? = null
            var birthday: LocalDateTime? = null
        }

        val updatePayload1 = UpdatePayload1().apply {
            name = "name"
            nullProperties = listOf("weight")
        }


        // 无条件
        assertFailsWith<IllegalArgumentException> {
            testTableDao.batchUpdateWhen(updatePayload1)
        }
        val searchPayload1 = SearchPayload1()
        updatePayload1.searchPayload = searchPayload1
        assertFailsWith<IllegalArgumentException> {
            testTableDao.batchUpdateWhen(updatePayload1)
        }

        // 有指定nullProperties的值
        var count = testTableDao.batchUpdateWhen(updatePayload1) { column, _ ->
            if (column.name == TestTableKtorms.name.name) {
                column.ieq("nAme1")
            } else {
                null
            }
        }
        assertEquals(1, count)
        var entity = testTableDao.get(-1)!!
        assertEquals("name", entity.name)
        assert(entity.birthday != null)
        assertEquals(null, entity.weight)

        // 未指定nullProperties的值
        searchPayload1.name = "name2"
        updatePayload1.nullProperties = null
        count = testTableDao.batchUpdateWhen(updatePayload1)
        assertEquals(1, count)
        entity = testTableDao.get(-1)!!
        assertEquals("name", entity.name)
        assert(entity.birthday != null)
    }

    @Test
    @Transactional
    open fun batchUpdateProperties() {
        val criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.LIKE, "name1")
        val properties = mapOf(TestTableKtorm::active.name to false, TestTableKtorm::height.name to null)
        assertEquals(3, testTableDao.batchUpdateProperties(criteria, properties))
        criteria.addAnd(TestTableKtorm::active.name, OperatorEnum.EQ, false)
        criteria.addAnd(TestTableKtorm::height.name, OperatorEnum.IS_NULL, null)
        assertEquals(3, testTableDao.count(criteria))
    }

    @Test
    @Transactional
    open fun batchUpdateOnly() {
        val entities = listOf(
            TestTableKtorm {
                id = -1
                name = "11"
                height = 0
                weight = 0.0
            },
            TestTableKtorm {
                id = -2
                name = "11"
                height = 0
                weight = 0.0
            }
        )
        val properties = arrayOf(TestTableKtorm::id.name, TestTableKtorm::name.name, TestTableKtorm::weight.name)
        assertEquals(2, testTableDao.batchUpdateOnly(entities, 3, *properties))
        assertEquals(2, testTableDao.oneSearch(TestTableKtorm::name.name, "11").size)
        assertEquals(2, testTableDao.oneSearch(TestTableKtorm::weight.name, 0.0).size)
        assertEquals(0, testTableDao.oneSearch(TestTableKtorm::height.name, 0).size)
    }

    @Test
    @Transactional
    open fun batchUpdateOnlyWhen() {
        val entities = listOf(
            TestTableKtorm {
                id = -1
                name = "11"
                height = 0
                weight = 0.0
            },
            TestTableKtorm {
                id = -2
                name = "11"
                height = 0
                weight = 0.0
            }
        )
        var criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.LIKE_S, "name1")
        val properties = arrayOf(TestTableKtorm::id.name, TestTableKtorm::name.name, TestTableKtorm::weight.name)
        assertEquals(1, testTableDao.batchUpdateOnlyWhen(entities, criteria, 1, *properties))
        assertEquals(1, testTableDao.oneSearch(TestTableKtorm::name.name, "11").size)
        assertEquals(1, testTableDao.oneSearch(TestTableKtorm::weight.name, 0.0).size)
        assertEquals(0, testTableDao.oneSearch(TestTableKtorm::height.name, 0).size)
    }

    @Test
    @Transactional
    open fun batchUpdateExcludeProperties() {
        val entities = listOf(
            TestTableKtorm {
                id = -1
                name = "11"
                weight = 0.0
            },
            TestTableKtorm {
                id = -2
                name = "11"
                weight = 0.0
            }
        )
        assertEquals(2, testTableDao.batchUpdateExcludeProperties(entities, 1, TestTableKtorm::weight.name))
        assertEquals(2, testTableDao.oneSearch(TestTableKtorm::name.name, "11").size)
        assertEquals(0, testTableDao.oneSearch(TestTableKtorm::weight.name, 0.0).size)
    }

    @Test
    @Transactional
    open fun batchUpdateExcludePropertiesWhen() {
        val entities = listOf(
            TestTableKtorm {
                id = -1
                name = "11"
                weight = 0.0
            },
            TestTableKtorm {
                id = -2
                name = "11"
                weight = 0.0
            }
        )
        var criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.LIKE_S, "name1")
        assertEquals(1, testTableDao.batchUpdateExcludePropertiesWhen(entities, criteria, 1, TestTableKtorm::weight.name))
        assertEquals(1, testTableDao.oneSearch(TestTableKtorm::name.name, "11").size)
        assertEquals(0, testTableDao.oneSearch(TestTableKtorm::weight.name, 0.0).size)
    }
    //endregion Update


    //region Delete
    @Test
    @Transactional
    open fun deleteById() {
        assert(testTableDao.deleteById(-1))
        assert(testTableDao.get(-1) == null)
        assert(!testTableDao.deleteById(1))
    }

    @Test
    @Transactional
    open fun delete() {
        val entity = testTableDao.get(-1)
        assert(testTableDao.delete(entity!!))
        assert(testTableDao.get(-1) == null)

        // 主键为null
        assertFailsWith<IllegalStateException> { testTableDao.delete(TestTableKtorm {}) }
    }

    @Test
    @Transactional
    open fun batchDelete() {
        val ids = listOf(-1, -2)
        val count = testTableDao.batchDelete(ids)
        assertEquals(2, count)
        assert(testTableDao.inSearch(TestTableKtorm::id.name, ids).isEmpty())
    }

    @Test
    @Transactional
    open fun batchDeleteCriteria() {
        val criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.LIKE_E, "1")
        assertEquals(2, testTableDao.batchDeleteCriteria(criteria))
        assertEquals(0, testTableDao.count(criteria))
    }

    @Test
    @Transactional
    open fun batchDeleteWhen() {
        class SearchPayload1 : SearchPayload() {
            var name: String? = null
            var weight: Double? = null
            var noExistProp: String? = "noExistProp"
        }

        val searchPayload1 = SearchPayload1()

        // 无条件
        assertFailsWith<IllegalArgumentException> {
            testTableDao.batchDeleteWhen()
        }
        assertFailsWith<IllegalArgumentException> {
            testTableDao.batchDeleteWhen(searchPayload1)
        }
        assertFailsWith<IllegalArgumentException> {
            testTableDao.batchDeleteWhen { _, _ -> null }
        }

        // 仅whereConditionFactory
        var count = testTableDao.batchDeleteWhen { column, _ ->
            if (column.name == TestTableKtorms.name.name) {
                column.eq("name2")
            } else null
        }
        assertEquals(1, count)
        assertEquals(null, testTableDao.get(-2))

        // 仅SearchPayload项
        searchPayload1.name = "name1"
        count = testTableDao.batchDeleteWhen(searchPayload1)
        assertEquals(1, count)
        assertEquals(null, testTableDao.get(-1))

        // SearchPayload项 & whereConditionFactory
        searchPayload1.name = "me3"
        count = testTableDao.batchDeleteWhen(searchPayload1) { column, value ->
            if (column.name == TestTableKtorms.name.name) {
                column.like("%${value}")
            } else null
        }
        assertEquals(1, count)
        assertEquals(null, testTableDao.get(-3))
    }

    //endregion Delete

}