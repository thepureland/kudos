package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.ability.data.rdb.ktorm.kit.getDatabase
import io.kudos.ability.data.rdb.ktorm.table.TestTableKtorm
import io.kudos.ability.data.rdb.ktorm.table.TestTableKtormDao
import io.kudos.ability.data.rdb.ktorm.table.TestTableKtorms
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.sort.Order
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.base.support.payload.ImmutableSearchPayload
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.ktorm.dsl.eq
import java.time.LocalDateTime
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * BaseReadOnlyDao测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
internal open class BaseReadOnlyDaoTest {

    @Resource
    private lateinit var testTableDao: TestTableKtormDao


    //region by id
    @Test
    fun getById() {
        val entity = testTableDao.get(-1)
        assertEquals("name1", entity!!.name)

        // 不存在指定主键对应的实体时
        assert(testTableDao.get(1) == null)
    }

    @Test
    fun getAs() {
        class Record {
            var name: String? = null
            var birthday: LocalDateTime? = null
            var active: Boolean? = null
            var noExistProp: String? = null
        }

        val record = testTableDao.getAs<Record>(-1)!!
        assertEquals("name1", record.name)
        assertEquals(true, record.active)
        assertEquals(null, record.noExistProp)

        // 不存在指定主键对应的实体时
        assert(testTableDao.get<Record>(1) == null)
    }

    @Test
    fun getByIds() {
        var entities = testTableDao.getByIds(setOf(-1, -2, -3))
        assertEquals(3, entities.size)

        entities = testTableDao.getByIds(setOf(-1, -2, -3), 2)
        assertEquals(3, entities.size)
    }

    /**
     * 测试 getByIds(ids, returnItemClass, countOfEachBatch)：按主键批量查询并指定返回类型。
     * - returnItemClass 为 null 时等价于 getByIds(ids, countOfEachBatch)，返回实体列表
     * - returnItemClass 为 TestTableKtorm 时返回实体列表
     * - returnItemClass 为自定义 PO（如 TestTableCacheItem）时返回该类型的实例列表
     */
    @Test
    fun getByIdsWithReturnItemClass() {
        // returnItemClass 为 null：应与 getByIds(-1, -2, -3) 结果一致
        val resultsAsEntity = testTableDao.getByIdsAs<TestTableKtorm>(setOf(-1, -2, -3))
        val resultsPlain = testTableDao.getByIdsAs<TestTableCacheItem>(setOf(-1, -2, -3))
        assertEquals(resultsPlain.size, resultsAsEntity.size)
        assertEquals(resultsPlain.map { it.id }.toSet(), resultsAsEntity.map { it.id }.toSet())
        assertEquals(3, resultsAsEntity.size)

        // returnItemClass 为 TestTableKtorm：应返回实体列表
        val resultsWithClass = testTableDao.getByIdsAs<TestTableCacheItem>(setOf(-1, -2, -3))
        assertEquals(3, resultsWithClass.size)
        assertEquals(setOf(-1, -2, -3), resultsWithClass.map { it.id }.toSet())

        // returnItemClass 为自定义 TestTableCacheItem：应返回 PO 列表
        val resultsAsCacheItem = testTableDao.getByIdsAs<TestTableCacheItem>(setOf(-1, -2, -3))
        assertEquals(3, resultsAsCacheItem.size)
        assertEquals(setOf(-1, -2, -3), resultsAsCacheItem.map { it.id }.toSet())
        assertEquals("name1", resultsAsCacheItem.find { it.id == -1 }?.name)

        // 指定 countOfEachBatch
        val resultsBatched = testTableDao.getByIdsAs<TestTableCacheItem>(setOf(-1, -2, -3), 2)
        assertEquals(3, resultsBatched.size)
    }
    //endregion by id


    //region oneSearch
    @Test
    fun oneSearch() {
        var results = testTableDao.oneSearch(TestTableKtorm::name, "name1")
        assertEquals(1, results.size)
        assertEquals(-1, results.first().id)

        // value为null的情况
        results = testTableDao.oneSearch(TestTableKtorm::weight, null)
        assertEquals(2, results.size)

        // 单条件升序
        results = testTableDao.oneSearch(TestTableKtorm::active, true, Order.asc(TestTableKtorm::name.name))
        assertEquals(8, results.size)
        assertEquals("name1", results.first().name)
        assertEquals("name9", results.last().name)

        // 单条件降序
        results = testTableDao.oneSearch(TestTableKtorm::active, true, Order.desc(TestTableKtorm::name.name))
        assertEquals(8, results.size)
        assertEquals("name9", results.first().name)
        assertEquals("name1", results.last().name)

        // 多个排序条件
        val orders = arrayOf(Order.asc(TestTableKtorm::height.name), Order.desc(TestTableKtorm::name.name))
        results = testTableDao.oneSearch(TestTableKtorm::active, true, *orders)
        assertEquals(8, results.size)
        assertEquals("name5", results.first().name)
        assertEquals("name4", results[5].name)
    }

    @Test
    fun oneSearchProperty() {
        var results = testTableDao.oneSearchProperty(TestTableKtorm::name, "name1", TestTableKtorm::id)
        assertEquals(1, results.size)
        assertEquals(-1, results.first())
        results = testTableDao.oneSearchProperty(TestTableKtorm::weight, null, TestTableKtorm::id)
        assertEquals(2, results.size)
    }

    @Test
    fun oneSearchProperties() {
        val returnProperties = listOf(TestTableKtorm::name, TestTableKtorm::id)
        val results = testTableDao.oneSearchProperties(TestTableKtorm::name, "name1", returnProperties)
        assertEquals(1, results.size)
        assertEquals("name1", results.first()[TestTableKtorm::name.name])
    }
    //endregion oneSearch


    //region allSearch
    @Test
    fun allSearch() {
        val result = testTableDao.allSearch()
        assertEquals(11, result.size)
    }

    @Test
    fun allSearchProperty() {
        val results = testTableDao.allSearchProperty(TestTableKtorm::id, Order.desc(TestTableKtorm::id.name))
        assertEquals(11, results.size)
        assertEquals(Integer.valueOf(-1), results[0])
        assertEquals(Integer.valueOf(-11), results[10])
    }

    @Test
    fun allSearchProperties() {
        val returnProperties = listOf(TestTableKtorm::name, TestTableKtorm::id)
        val results = testTableDao.allSearchProperties(returnProperties)
        assertEquals(11, results.size)
    }
    //endregion allSearch


    //region andSearch
    @Test
    fun andSearch() {
        val propertyMap: Map<KProperty1<TestTableKtorm, *>, Any?> =
            mapOf(TestTableKtorm::name to "name5", TestTableKtorm::weight to null)
        var results = testTableDao.andSearch(propertyMap)
        assertEquals(1, results.size)

        // 自定义查询逻辑
        results = testTableDao.andSearch(
            propertyMap,
            whereConditionFactory = { column, _ ->
                if (column.name == TestTableKtorms.name.name) {
                    column.ilike("%Me5")
                } else null
            }
        )
        assertEquals(1, results.size)
        assertEquals("name5", results.first().name)
    }

    @Test
    fun andSearchProperty() {
        val criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.EQ, "name5")
            .addAnd(TestTableKtorm::weight.name, OperatorEnum.IS_NULL, null)
        val results = testTableDao.searchProperty(criteria, TestTableKtorm::name)
        assertEquals("name5", results.first())
    }

    @Test
    fun andSearchProperties() {
        val criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.EQ, "name1")
            .addAnd(TestTableKtorm::active.name, OperatorEnum.EQ, true)
        val returnProperties = listOf(TestTableKtorm::name, TestTableKtorm::id)
        val results = testTableDao.searchProperties(criteria, returnProperties)
        assertEquals(1, results.size)
        assertEquals("name1", results.first()[TestTableKtorm::name.name])
    }
    //endregion andSearch


    //region orSearch
    @Test
    fun orSearch() {
        val propertyMap: Map<KProperty1<TestTableKtorm, *>, Any?> =
            mapOf(TestTableKtorm::name to "name5", TestTableKtorm::weight to null)
        val results = testTableDao.orSearch(propertyMap)
        assertEquals(2, results.size)
    }

    @Test
    fun orSearchProperty() {
        val criteria = Criteria.or(
            Criterion(TestTableKtorm::name.name, OperatorEnum.EQ, "name5"),
            Criterion(TestTableKtorm::weight.name, OperatorEnum.IS_NULL, null)
        )
        val results =
            testTableDao.searchProperty(criteria, TestTableKtorm::name, Order.desc(TestTableKtorm::id.name))
        assertEquals(2, results.size)
        assertEquals("name5", results.first())
    }

    @Test
    fun orSearchProperties() {
        val criteria = Criteria.or(
            Criterion(TestTableKtorm::name.name, OperatorEnum.EQ, "name5"),
            Criterion(TestTableKtorm::weight.name, OperatorEnum.IS_NULL, null)
        )
        val returnProperties = listOf(TestTableKtorm::name, TestTableKtorm::id)
        val results =
            testTableDao.searchProperties(criteria, returnProperties, Order.desc(TestTableKtorm::id.name))
        assertEquals(2, results.size)
        assertEquals("name5", results.first()[TestTableKtorm::name.name])
    }
    //endregion orSearch


    //region inSearch
    @Test
    fun inSearch() {
        val ids = listOf(-3, -2, -1, null)
        val results = testTableDao.inSearch(TestTableKtorm::id, ids, Order.desc(TestTableKtorm::id.name))
        assertEquals(3, results.size)
        assertEquals("name1", results.first().name)
    }

    @Test
    fun inSearchProperty() {
        val ids = listOf(-3, -2, -1)
        val criteria = Criteria.of(TestTableKtorm::id.name, OperatorEnum.IN, ids)
        val results = testTableDao.searchProperty(criteria, TestTableKtorm::name, Order.desc(TestTableKtorm::id.name))
        assertEquals(3, results.size)
        assertEquals("name1", results.first())
    }

    @Test
    fun inSearchProperties() {
        val ids = listOf(-3, -2, -1)
        val criteria = Criteria.of(TestTableKtorm::id.name, OperatorEnum.IN, ids)
        val returnProperties = listOf(TestTableKtorm::name, TestTableKtorm::id, TestTableKtorm::active)
        val results = testTableDao.searchProperties(criteria, returnProperties, Order.desc(TestTableKtorm::id.name))
        assertEquals(3, results.size)
        assertEquals("name1", results.first()[TestTableKtorm::name.name])
    }

    @Test
    fun inSearchById() {
        val ids = listOf(-3, -2, -1)
        val results = testTableDao.inSearchById(ids)
        assertEquals(3, results.size)
    }

    @Test
    fun inSearchPropertyById() {
        val ids = listOf(-3, -2, -1)
        val results =
            testTableDao.inSearchPropertyById(ids, TestTableKtorm::name, Order.desc(TestTableKtorm::id.name))
        assertEquals(3, results.size)
        assertEquals("name1", results.first())
    }

    @Test
    fun inSearchPropertyByIdNullableProperty() {
        val ids = listOf(-11, -1)
        val results = testTableDao.inSearchPropertyById(ids, TestTableKtorm::weight, Order.asc(TestTableKtorm::id.name))
        assertEquals(2, results.size)
        assert(results.contains(null))
        assert(results.contains(56.5))
    }

    @Test
    fun inSearchPropertiesById() {
        val ids = listOf(-3, -2, -1)
        val returnProperties = listOf(TestTableKtorm::name.name, TestTableKtorm::id.name)
        val results = testTableDao.inSearchPropertiesById(ids, returnProperties, Order.desc(TestTableKtorm::id.name))
        assertEquals(3, results.size)
        assertEquals("name1", results.first()[TestTableKtorm::name.name])
    }
    //endregion inSearch


    //region search Criteria
    @Test
    fun search() {
        val inIds = Criterion(TestTableKtorm::id.name, OperatorEnum.IN, listOf(-2, -4, -6, -7))
        val eqActive = Criterion(TestTableKtorm::active.name, OperatorEnum.EQ, true)
        val andCriteria = Criteria.and(inIds, eqActive)
        val likeName = Criterion(TestTableKtorm::name.name, OperatorEnum.LIKE_S, "name1")
        val orCriteria: Criteria = Criteria.or(likeName, andCriteria)
        val noNull = Criterion(TestTableKtorm::weight.name, OperatorEnum.IS_NOT_NULL, null)
        val criteria: Criteria = Criteria.and(orCriteria, noNull)
        val results = testTableDao.search(criteria, Order.desc(TestTableKtorm::weight.name))
        assertEquals(5, results.size)
        assertEquals(-10, results.first().id)
    }

    /**
     * 测试 search(criteria, returnItemClass, orders)：按 Criteria 查询并指定返回类型。
     * - returnItemClass 为 null 时等价于 search(criteria, orders)，返回实体列表
     * - returnItemClass 为 TestTableKtorm 时返回实体列表
     * - returnItemClass 为自定义 PO（如 TestTableCacheItem）时返回该类型的实例列表
     */
    @Test
    fun searchWithReturnItemClass() {
        val inIds = Criterion(TestTableKtorm::id.name, OperatorEnum.IN, listOf(-2, -4, -6, -7))
        val eqActive = Criterion(TestTableKtorm::active.name, OperatorEnum.EQ, true)
        val andCriteria = Criteria.and(inIds, eqActive)
        val likeName = Criterion(TestTableKtorm::name.name, OperatorEnum.LIKE_S, "name1")
        val orCriteria: Criteria = Criteria.or(likeName, andCriteria)
        val noNull = Criterion(TestTableKtorm::weight.name, OperatorEnum.IS_NOT_NULL, null)
        val criteria: Criteria = Criteria.and(orCriteria, noNull)
        val order = Order.desc(TestTableKtorm::weight.name)

        // returnItemClass 为 null：应与 search(criteria, order) 结果一致，返回实体列表
        val resultsAsEntity = testTableDao.searchAs<TestTableKtorm>(criteria, order)
        val resultsPlain = testTableDao.search(criteria, order)
        assertEquals(resultsPlain.size, resultsAsEntity.size)
        assertEquals(resultsPlain.map { it.id }, resultsAsEntity.map { it.id })
        assertEquals(-10, resultsAsEntity.first().id)

        // returnItemClass 为 TestTableKtorm：应返回实体列表
        val resultsWithClass = testTableDao.searchAs<TestTableKtorm>(criteria, order)
        assertEquals(5, resultsWithClass.size)
        assertEquals(-10, resultsWithClass.first().id)

        // returnItemClass 为自定义 TestTableCacheItem：应返回 PO 实例列表，属性从查询结果映射
        val resultsAsCacheItem = testTableDao.searchAs<TestTableCacheItem>(criteria)
        assertEquals(5, resultsAsCacheItem.size)
        assert(resultsAsCacheItem.any { it.id == -10 && it.name == "name10" })
    }

    @Test
    fun searchProperty() {
        // ILIKE_S，IS_NOT_NULL，GT
        var criteria = Criteria.and(
            Criterion(TestTableKtorm::name.name, OperatorEnum.ILIKE_S, "Name1"),
            Criterion(TestTableKtorm::weight.name, OperatorEnum.IS_NOT_NULL, null),
            Criterion(TestTableKtorm::height.name, OperatorEnum.GT, 160)
        )
        var results =
            testTableDao.searchProperty(criteria, TestTableKtorm::id, Order.desc(TestTableKtorm::weight.name))
        assertEquals(2, results.size)
        assertEquals(-10, results.first())

        // IEQ
        criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.IEQ, "Name1")
        results = testTableDao.searchProperty(criteria, TestTableKtorm::id)
        assertEquals(1, results.size)

        // GT_P
        criteria = Criteria.of(TestTableKtorm::height.name, OperatorEnum.GT_P, TestTableKtorm::weight.name)
        results = testTableDao.searchProperty(criteria, TestTableKtorm::id)
        assertEquals(9, results.size)

        // NE_P
        criteria = Criteria.of(TestTableKtorm::height.name, OperatorEnum.NE_P, TestTableKtorm::weight.name)
        results = testTableDao.searchProperty(criteria, TestTableKtorm::id)
        assertEquals(9, results.size)
    }

    @Test
    fun searchPropertyNullableProperty() {
        val criteria = Criteria.of(TestTableKtorm::active.name, OperatorEnum.EQ, true)
        val results = testTableDao.searchProperty(criteria, TestTableKtorm::weight, Order.asc(TestTableKtorm::id.name))
        assertEquals(8, results.size)
        assert(results.contains(null))
    }

    @Test
    fun searchProperties() {
        val inIds = Criterion(TestTableKtorm::id.name, OperatorEnum.IN, listOf(-2, -4, -6, -7))
        val eqActive = Criterion(TestTableKtorm::active.name, OperatorEnum.EQ, true)
        val andCriteria = Criteria.and(inIds, eqActive)
        val likeName = Criterion(TestTableKtorm::name.name, OperatorEnum.LIKE_S, "name1")
        val orCriteria: Criteria = Criteria.or(likeName, andCriteria)
        val noNull = Criterion(TestTableKtorm::weight.name, OperatorEnum.IS_NOT_NULL, null)
        val criteria: Criteria = Criteria.and(orCriteria, noNull)
        val returnProperties = listOf(TestTableKtorm::active, TestTableKtorm::name)
        val results = testTableDao.searchProperties(criteria, returnProperties, Order.desc(TestTableKtorm::weight.name))
        assertEquals(5, results.size)
        assertEquals("name10", results.first()[TestTableKtorm::name.name])
        assertEquals(false, results.first()[TestTableKtorm::active.name])
    }
    //endregion search Criteria


    //region pagingSearch
    @Test
    fun pagingSearch() {
        if (isSupportPaging()) {
            val criteria = Criteria.of(TestTableKtorm::active.name, OperatorEnum.EQ, true)
            val entities = testTableDao.pagingSearch(criteria, 1, 4, Order.asc(TestTableKtorm::id.name))
            assertEquals(4, entities.size)
            assertEquals(-11, entities.first().id)
        }
    }

    @Test
    fun pagingReturnProperty() {
        if (isSupportPaging()) {
            val criteria = Criteria.of(TestTableKtorm::active.name, OperatorEnum.EQ, true)
            val results =
                testTableDao.pagingReturnProperty(
                    criteria,
                    TestTableKtorm::id,
                    1,
                    4,
                    Order.asc(TestTableKtorm::id.name)
                )
            assertEquals(4, results.size)
            assertEquals(-11, results.first())
        }
    }

    @Test
    fun pagingReturnPropertyNullableProperty() {
        if (isSupportPaging()) {
            val criteria = Criteria.of(TestTableKtorm::active.name, OperatorEnum.EQ, true)
            val results =
                testTableDao.pagingReturnProperty(
                    criteria,
                    TestTableKtorm::weight,
                    1,
                    4,
                    Order.asc(TestTableKtorm::id.name)
                )
            assertEquals(4, results.size)
            assert(results.contains(null))
        }
    }

    @Test
    fun pagingReturnProperties() {
        if (isSupportPaging()) {
            val criteria = Criteria.of(TestTableKtorm::active.name, OperatorEnum.EQ, true)
            val returnProperties = listOf(TestTableKtorm::id, TestTableKtorm::name)
            val results =
                testTableDao.pagingReturnProperties(
                    criteria,
                    returnProperties,
                    1,
                    4,
                    Order.asc(TestTableKtorm::id.name)
                )
            assertEquals(4, results.size)
            assertEquals(-11, results.first()[TestTableKtorm::id.name])
        }
    }

    /**
     * 测试 pagingSearch(criteria, returnItemClass, pageNo, pageSize, orders)：分页查询并指定返回类型。
     * - returnItemClass 为 null 时等价于 pagingSearch(criteria, pageNo, pageSize, orders)，返回分页实体列表
     * - returnItemClass 为 TestTableKtorm 时返回分页实体列表
     * - returnItemClass 为自定义 PO（如 TestTableCacheItem）时返回分页的该类型实例列表
     */
    @Test
    fun pagingSearchWithReturnItemClass() {
        if (isSupportPaging()) {
            val criteria = Criteria.of(TestTableKtorm::active.name, OperatorEnum.EQ, true)
            val pageNo = 1
            val pageSize = 4
            val order = Order.asc(TestTableKtorm::id.name)

            // returnItemClass 为 null：应与 pagingSearch(criteria, pageNo, pageSize, order) 结果一致
            val resultsAsEntity = testTableDao.pagingSearchAs<TestTableCacheItem>(criteria, pageNo, pageSize, order)
            val resultsPlain = testTableDao.pagingSearchAs<TestTableCacheItem>(criteria, pageNo, pageSize, order)
            assertEquals(resultsPlain.size, resultsAsEntity.size)
            assertEquals(resultsPlain.map { it.id }, resultsAsEntity.map { it.id })
            assertEquals(4, resultsAsEntity.size)
            assertEquals(-11, resultsAsEntity.first().id)

            // returnItemClass 为 TestTableKtorm：应返回分页实体列表
            val resultsWithClass = testTableDao.pagingSearchAs<TestTableCacheItem>(criteria, pageNo, pageSize, order)
            assertEquals(4, resultsWithClass.size)
            assertEquals(-11, resultsWithClass.first().id)

            // returnItemClass 为自定义 TestTableCacheItem：应返回分页的 PO 列表
            val resultsAsCacheItem =
                testTableDao.pagingSearchAs<TestTableCacheItem>(criteria, pageNo, pageSize, order)
            assertEquals(4, resultsAsCacheItem.size)
            assertEquals(-11, resultsAsCacheItem.first().id)
        }
    }

    private fun isSupportPaging(): Boolean { // h2可以用PostgreSqlDialect来实现分页
        val dialect = RdbKit.getDatabase().dialect
        return !dialect::class.java.name.contains($$"SqlDialectKt$detectDialectImplementation$1")
    }
    //endregion pagingSearch


    //region payload search

    @Test
    fun searchBySearchPayload() {
        // 指定returnProperties, 多个属性
        class SearchPayload1 : ListSearchPayload() {
            var name: String? = null
            var weight: Double? = null
            var noExistProp: String? = "noExistProp"
            override var returnProperties: List<String>? = listOf("id", "name", "height")
        }

        // 仅指定SearchPayload
        val searchPayload1 = SearchPayload1().apply {
            name = "name1"
            weight = 56.5
        }
        var result = testTableDao.search(searchPayload1)
        assertEquals(1, result.size)
        assert(result.first() is Map<*, *>)
        assertEquals(3, (result.first() as Map<*, *>).size)
        assertEquals(-1, (result.first() as Map<*, *>)["id"])

        // 指定returnProperties, 单个属性
        searchPayload1.returnProperties = listOf("id")
        result = testTableDao.search(searchPayload1)
        assertEquals(1, result.size)
        assertEquals(-1, result.first())

        // returnProperties为null
        searchPayload1.returnProperties = null
        result = testTableDao.search(searchPayload1)
        assertEquals(1, result.size)
        assert(result.first() is TestTableKtorm)
        assertEquals(-1, (result.first() as TestTableKtorm).id)

        // 分页 & 排序
        searchPayload1.name = null
        searchPayload1.weight = null
        searchPayload1.pageNo = 1
        searchPayload1.pageSize = 3
        searchPayload1.orders = listOf(Order.asc(TestTableKtorm::name.name))
        result = testTableDao.search(searchPayload1)
        assertEquals(3, result.size)
        assert(result.first() is TestTableKtorm)
        assertEquals(-1, (result.first() as TestTableKtorm).id)

        // 指定结果封装类
        class Result {
            var id: Int? = null
            var name: String? = null
            var weight: Double? = null
            var noExistProp: String? = "noExistProp"
        }
        searchPayload1.returnEntityClass = Result::class
        result = testTableDao.search(searchPayload1)
        assertEquals(3, result.size)
        assert(result.first() is Result)
        assertEquals(-1, (result.first() as Result).id)

        // 指定结果封装类（类型安全入口）
        val typedResult = testTableDao.search(searchPayload1, Result::class)
        assertEquals(3, typedResult.size)
        assertEquals(-1, typedResult.first().id)

        // 自定义查询逻辑（通过工厂）
        searchPayload1.name = "nAme1"
        searchPayload1.pageNo = null
        result = testTableDao.search(searchPayload1) { column, value ->
            if (column.name == TestTableKtorms::name.name) {
                testTableDao.whereExpr(column, OperatorEnum.ILIKE_S, value)
            } else {
                null
            }
        }
        assertEquals(3, result.size)

        // 自定义查询逻辑（通过工厂+通过SearchPayload，工厂方式优先）
        class SearchPayload2 : ListSearchPayload() {
            var name: String? = null
            var weight: Double? = null
            var noExistProp: String? = "noExistProp"
            override var returnProperties: List<String>? = listOf("id", "name", "height")
            override val operators: Map<KProperty0<*>, OperatorEnum> = mapOf(SearchPayload2::name to OperatorEnum.ILIKE)
        }

        val searchPayload2 = SearchPayload2().apply {
            name = "nAme1"
            weight = 56.5
            pageNo = null
        }
        result = testTableDao.search(searchPayload2) { column, value ->
            if (column.name == TestTableKtorms::name.name) {
                testTableDao.whereExpr(column, OperatorEnum.ILIKE_S, value)
            } else {
                null
            }
        }
        assertEquals(1, result.size)

        // 仅指定whereConditionFactory
        result = testTableDao.search { column, _ ->
            when (column.name) {
                TestTableKtorms::name.name -> {
                    column.ilike("nAme1%")
                }

                TestTableKtorms::active.name -> {
                    column.eq(true)
                }

                else -> null
            }
        }
        assertEquals(2, result.size)

        // 不指定任何条件，相当于allSearch()
        result = testTableDao.search()
        assertEquals(11, result.size)
    }

    //endregion payload search


    //region aggregate
    @Test
    fun count() {
        assertEquals(11, testTableDao.count())
        val criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.LIKE_S, "name1")
        assertEquals(3, testTableDao.count(criteria))
    }

    @Test
    fun countByPayload() {
        class SearchPayload1 : ImmutableSearchPayload() {
            var name: String? = null
            var weight: Double? = null
            var noExistProp: String? = "noExistProp"
        }

        val searchPayload1 = SearchPayload1().apply {
            name = "nAme1"
        }
        val result = testTableDao.count(searchPayload1) { column, value ->
            if (column.name == TestTableKtorms::name.name) {
                testTableDao.whereExpr(column, OperatorEnum.ILIKE_S, value)
            } else {
                null
            }
        }
        assertEquals(3, result)
    }

    @Test
    fun sum() {
        assertEquals(1382, testTableDao.sum(TestTableKtorm::height))
        val criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.LIKE_S, "name1")
        assertEquals(122.5, testTableDao.sum(TestTableKtorm::weight, criteria))
        assertEquals(445.74, testTableDao.sum(TestTableKtorm::weight))
    }

    @Test
    fun avg() {
        val criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.LIKE_S, "name1")
        assertEquals(61.25, testTableDao.avg(TestTableKtorm::weight, criteria))
    }

    @Test
    fun max() {
        val criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.LIKE_S, "name1")
        val result = testTableDao.max(TestTableKtorm::weight, criteria)
        assertEquals(66.0, result)
    }

    @Test
    fun min() {
        val criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.LIKE_S, "name1")
        val result = testTableDao.min(TestTableKtorm::weight, criteria)
        assertEquals(56.5, result)
    }
    //endregion aggregate

}

/**
 * 自定义缓存项 PO，用于 search(criteria, returnItemClass, orders) 的 returnItemClass 测试。
 * 属性与 test_table_ktorm 表列对应，便于结果映射。
 */
internal class TestTableCacheItem {
    var id: Int? = null
    var name: String? = null
    var weight: Double? = null
    var height: Int? = null
    val extraProp: String? = null
}