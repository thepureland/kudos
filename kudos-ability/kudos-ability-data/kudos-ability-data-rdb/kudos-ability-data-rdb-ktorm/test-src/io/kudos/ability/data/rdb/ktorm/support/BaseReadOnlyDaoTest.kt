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
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.ktorm.dsl.eq
import java.time.LocalDateTime
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test cases for BaseReadOnlyDao.
 *
 * @author K
 * @author AI: Codex
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

        // When no entity matches the given primary key
        assert(testTableDao.get(1) == null)
    }

    @Test
    fun getAs() {
        /**
         * Projection object used in the getAs test.
         *
         * @author K
         * @author AI: Codex
         * @since 1.0.0
         */
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

        // When no entity matches the given primary key
        assert(testTableDao.get<Record>(1) == null)
    }

    @Test
    fun getAsImmutableDataClass() {
        val record = testTableDao.getAs<TestTableImmutableRecord>(-1)!!
        assertEquals(-1, record.id)
        assertEquals("name1", record.name)
        assertEquals(true, record.active)
        assertEquals(null, record.extraProp)
    }

    @Test
    fun getByIds() {
        var entities = testTableDao.getByIds(setOf(-1, -2, -3))
        assertEquals(3, entities.size)

        entities = testTableDao.getByIds(setOf(-1, -2, -3), 2)
        assertEquals(3, entities.size)
    }

    /**
     * Tests getByIds(ids, returnItemClass, countOfEachBatch): batch-fetch by primary key with an explicit result type.
     * - When returnItemClass is null, behaves like getByIds(ids, countOfEachBatch) and returns an entity list
     * - When returnItemClass is TestTableKtorm, returns an entity list
     * - When returnItemClass is a custom PO (e.g. TestTableCacheItem), returns instances of that type
     */
    @Test
    fun getByIdsWithReturnItemClass() {
        // returnItemClass null: should match getByIds(-1, -2, -3)
        val resultsAsEntity = testTableDao.getByIdsAs<TestTableKtorm>(setOf(-1, -2, -3))
        val resultsPlain = testTableDao.getByIdsAs<TestTableCacheItem>(setOf(-1, -2, -3))
        assertEquals(resultsPlain.size, resultsAsEntity.size)
        assertEquals(resultsPlain.map { it.id }.toSet(), resultsAsEntity.map { it.id }.toSet())
        assertEquals(3, resultsAsEntity.size)

        // returnItemClass = TestTableKtorm: should return an entity list
        val resultsWithClass = testTableDao.getByIdsAs<TestTableCacheItem>(setOf(-1, -2, -3))
        assertEquals(3, resultsWithClass.size)
        assertEquals(setOf(-1, -2, -3), resultsWithClass.map { it.id }.toSet())

        // returnItemClass = custom TestTableCacheItem: should return a PO list
        val resultsAsCacheItem = testTableDao.getByIdsAs<TestTableCacheItem>(setOf(-1, -2, -3))
        assertEquals(3, resultsAsCacheItem.size)
        assertEquals(setOf(-1, -2, -3), resultsAsCacheItem.map { it.id }.toSet())
        assertEquals("name1", resultsAsCacheItem.find { it.id == -1 }?.name)

        // With explicit countOfEachBatch
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

        // Case when value is null
        results = testTableDao.oneSearch(TestTableKtorm::weight, null)
        assertEquals(2, results.size)

        // Single condition, ascending
        results = testTableDao.oneSearch(TestTableKtorm::active, true, Order.asc(TestTableKtorm::name.name))
        assertEquals(8, results.size)
        assertEquals("name1", results.first().name)
        assertEquals("name9", results.last().name)

        // Single condition, descending
        results = testTableDao.oneSearch(TestTableKtorm::active, true, Order.desc(TestTableKtorm::name.name))
        assertEquals(8, results.size)
        assertEquals("name9", results.first().name)
        assertEquals("name1", results.last().name)

        // Multiple sort orders
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

        // Custom query logic
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
     * Tests search(criteria, returnItemClass, orders): Criteria query with an explicit result type.
     * - When returnItemClass is null, behaves like search(criteria, orders) and returns an entity list
     * - When returnItemClass is TestTableKtorm, returns an entity list
     * - When returnItemClass is a custom PO (e.g. TestTableCacheItem), returns instances of that type
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

        // returnItemClass null: should match search(criteria, order), returning an entity list
        val resultsAsEntity = testTableDao.searchAs<TestTableKtorm>(criteria, order)
        val resultsPlain = testTableDao.search(criteria, order)
        assertEquals(resultsPlain.size, resultsAsEntity.size)
        assertEquals(resultsPlain.map { it.id }, resultsAsEntity.map { it.id })
        assertEquals(-10, resultsAsEntity.first().id)

        // returnItemClass = TestTableKtorm: should return an entity list
        val resultsWithClass = testTableDao.searchAs<TestTableKtorm>(criteria, order)
        assertEquals(5, resultsWithClass.size)
        assertEquals(-10, resultsWithClass.first().id)

        // returnItemClass = custom TestTableCacheItem: should return PO instances populated from the query
        val resultsAsCacheItem = testTableDao.searchAs<TestTableCacheItem>(criteria)
        assertEquals(5, resultsAsCacheItem.size)
        assert(resultsAsCacheItem.any { it.id == -10 && it.name == "name10" })

        val immutableResults = testTableDao.searchAs<TestTableImmutableRecord>(criteria)
        assertEquals(5, immutableResults.size)
        assert(immutableResults.any { it.id == -10 && it.name == "name10" && it.extraProp == null })
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
     * Tests pagingSearch(criteria, returnItemClass, pageNo, pageSize, orders): paged query with an explicit result type.
     * - When returnItemClass is null, behaves like pagingSearch(criteria, pageNo, pageSize, orders)
     * - When returnItemClass is TestTableKtorm, returns a paged entity list
     * - When returnItemClass is a custom PO (e.g. TestTableCacheItem), returns a paged list of that type
     */
    @Test
    fun pagingSearchWithReturnItemClass() {
        if (isSupportPaging()) {
            val criteria = Criteria.of(TestTableKtorm::active.name, OperatorEnum.EQ, true)
            val pageNo = 1
            val pageSize = 4
            val order = Order.asc(TestTableKtorm::id.name)

            // returnItemClass null: should match pagingSearch(criteria, pageNo, pageSize, order)
            val resultsAsEntity = testTableDao.pagingSearchAs<TestTableCacheItem>(criteria, pageNo, pageSize, order)
            val resultsPlain = testTableDao.pagingSearchAs<TestTableCacheItem>(criteria, pageNo, pageSize, order)
            assertEquals(resultsPlain.size, resultsAsEntity.size)
            assertEquals(resultsPlain.map { it.id }, resultsAsEntity.map { it.id })
            assertEquals(4, resultsAsEntity.size)
            assertEquals(-11, resultsAsEntity.first().id)

            // returnItemClass = TestTableKtorm: should return a paged entity list
            val resultsWithClass = testTableDao.pagingSearchAs<TestTableCacheItem>(criteria, pageNo, pageSize, order)
            assertEquals(4, resultsWithClass.size)
            assertEquals(-11, resultsWithClass.first().id)

            // returnItemClass = custom TestTableCacheItem: should return a paged PO list
            val resultsAsCacheItem =
                testTableDao.pagingSearchAs<TestTableCacheItem>(criteria, pageNo, pageSize, order)
            assertEquals(4, resultsAsCacheItem.size)
            assertEquals(-11, resultsAsCacheItem.first().id)
        }
    }

    private fun isSupportPaging(): Boolean { // h2 can implement paging via PostgreSqlDialect
        val dialect = RdbKit.getDatabase().dialect
        return !dialect::class.java.name.contains($$"SqlDialectKt$detectDialectImplementation$1")
    }
    //endregion pagingSearch


    //region payload search

    @Test
    fun searchBySearchPayload() {
        // Specify multiple returnProperties
        /**
         * Search payload for searchBySearchPayload.
         *
         * @author K
         * @author AI: Codex
         * @since 1.0.0
         */
        class SearchPayload1 : ListSearchPayload() {
            var name: String? = null
            var weight: Double? = null
            var noExistProp: String? = "noExistProp"
            var returnPropertiesField: List<String>? = listOf("id", "name", "height")
            override fun getReturnProperties() = returnPropertiesField
            var returnEntityClassField: kotlin.reflect.KClass<*>? = null
            override fun getReturnEntityClass() = returnEntityClassField
        }

        // Only specify SearchPayload
        val searchPayload1 = SearchPayload1().apply {
            name = "name1"
            weight = 56.5
        }
        var result = testTableDao.search(searchPayload1)
        assertEquals(1, result.size)
        assert(result.first() is Map<*, *>)
        assertEquals(3, (result.first() as Map<*, *>).size)
        assertEquals(-1, (result.first() as Map<*, *>)["id"])

        // Specify a single returnProperties entry
        searchPayload1.returnPropertiesField = listOf("id")
        result = testTableDao.search(searchPayload1)
        assertEquals(1, result.size)
        assertEquals(-1, result.first())

        // returnProperties is null
        searchPayload1.returnPropertiesField = null
        result = testTableDao.search(searchPayload1)
        assertEquals(1, result.size)
        assert(result.first() is TestTableKtorm)
        assertEquals(-1, (result.first() as TestTableKtorm).id)

        // Explicitly specify the return entity type as a Ktorm interface-type PO (matches the table entity; must use Entity.create, not a reflective constructor)
        searchPayload1.returnEntityClassField = TestTableKtorm::class
        searchPayload1.pageNo = null
        searchPayload1.pageSize = null
        searchPayload1.orders = emptyList()
        searchPayload1.name = "name1"
        searchPayload1.weight = 56.5
        result = testTableDao.search(searchPayload1)
        assertEquals(1, result.size)
        assert(result.first() is TestTableKtorm)
        assertEquals(-1, (result.first() as TestTableKtorm).id)

        // Paging & sort (TestTableKtorm only has @Sortable on name and height; ascending by name is valid)
        searchPayload1.name = null
        searchPayload1.weight = null
        searchPayload1.pageNo = 1
        searchPayload1.pageSize = 3
        searchPayload1.orders = listOf(Order.asc(TestTableKtorm::name.name))
        result = testTableDao.search(searchPayload1)
        assertEquals(3, result.size)
        assert(result.first() is TestTableKtorm)
        assertEquals(-1, (result.first() as TestTableKtorm).id)

        // Specify a result wrapper class
        /**
         * Result projection object for searchBySearchPayload.
         *
         * @author K
         * @author AI: Codex
         * @since 1.0.0
         */
        class Result {
            var id: Int? = null
            var name: String? = null
            var weight: Double? = null
            var noExistProp: String? = "noExistProp"
        }
        searchPayload1.returnEntityClassField = Result::class
        result = testTableDao.search(searchPayload1)
        assertEquals(3, result.size)
        assert(result.first() is Result)
        assertEquals(-1, (result.first() as Result).id)

        // Specify a result wrapper class (type-safe entry point)
        val typedResult = testTableDao.search(searchPayload1, Result::class)
        assertEquals(3, typedResult.size)
        assertEquals(-1, typedResult.first().id)

        // Custom query logic (via factory)
        searchPayload1.name = "nAme1"
        searchPayload1.pageNo = null
        result = testTableDao.search(searchPayload1, whereConditionFactory = { column, value ->
            if (column.name == TestTableKtorms::name.name) {
                testTableDao.whereExpr(column, OperatorEnum.ILIKE_S, value)
            } else {
                null
            }
        })
        assertEquals(3, result.size)

        // Custom query logic (via both factory and SearchPayload; factory takes precedence)
        /**
         * Test payload for custom query logic.
         *
         * @author K
         * @author AI: Codex
         * @since 1.0.0
         */
        class SearchPayload2 : ListSearchPayload() {
            var name: String? = null
            var weight: Double? = null
            var noExistProp: String? = "noExistProp"
            var returnPropertiesField: List<String>? = listOf("id", "name", "height")
            override fun getReturnProperties() = returnPropertiesField
            override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(::name to OperatorEnum.ILIKE)
        }

        val searchPayload2 = SearchPayload2().apply {
            name = "nAme1"
            weight = 56.5
            pageNo = null
        }
        result = testTableDao.search(searchPayload2, whereConditionFactory = { column, value ->
            if (column.name == TestTableKtorms::name.name) {
                testTableDao.whereExpr(column, OperatorEnum.ILIKE_S, value)
            } else {
                null
            }
        })
        assertEquals(1, result.size)

        // Only specify whereConditionFactory
        result = testTableDao.search(whereConditionFactory = { column, _ ->
            when (column.name) {
                TestTableKtorms::name.name -> {
                    column.ilike("nAme1%")
                }

                TestTableKtorms::active.name -> {
                    column.eq(true)
                }

                else -> null
            }
        })
        assertEquals(2, result.size)

        // No conditions specified; behaves like allSearch()
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
        /**
         * Search payload for countByPayload.
         *
         * @author K
         * @author AI: Codex
         * @since 1.0.0
         */
        class SearchPayload1 : ListSearchPayload() {
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

    //region @Sortable (ListSearchPayload.orders: each item must be @Sortable on the table PO; otherwise WARN and ignore)

    /**
     * Test DAO exposing the internal sort-whitelist methods.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class TestTableDaoForSortAccess : TestTableKtormDao() {
        fun sortWhitelistForTest() = sortWhitelistFromPo()
        fun filterOrdersForTest(raw: List<Order>?) = filterOrdersBySortWhitelist(raw, sortWhitelistFromPo())
    }

    @Test
    fun sortWhitelistFromPo_collectsSortableAnnotationsOnEntity() {
        assertEquals(
            setOf(TestTableKtorm::name.name, TestTableKtorm::height.name),
            TestTableDaoForSortAccess().sortWhitelistForTest()
        )
    }

    @Test
    fun filterOrdersBySortWhitelist_stripsPropertiesWithoutSortable() {
        val dao = TestTableDaoForSortAccess()
        val filtered = dao.filterOrdersForTest(
            listOf(Order.desc(TestTableKtorm::weight.name), Order.asc(TestTableKtorm::name.name))
        )
        assertEquals(1, filtered.size)
        assertEquals(TestTableKtorm::name.name, filtered[0].property)
        assertEquals(true, filtered[0].isAscending())
    }

    /**
     * weight has no @Sortable, only name applies: results are sorted ascending by name; the first row is still name1.
     */
    @Test
    fun search_payloadOrders_keepsOnlySortableProperties() {
        /**
         * Test payload for the sort whitelist.
         *
         * @author K
         * @author AI: Codex
         * @since 1.0.0
         */
        class P : ListSearchPayload() {
            var active: Boolean? = null
        }
        val p = P().apply {
            active = true
            pageNo = 1
            pageSize = 8
            orders = listOf(
                Order.desc(TestTableKtorm::weight.name),
                Order.asc(TestTableKtorm::name.name)
            )
        }
        @Suppress("UNCHECKED_CAST")
        val result = testTableDao.search(p) as List<TestTableKtorm>
        assertEquals(8, result.size)
        assertEquals("name1", result.first().name)
    }

    /**
     * Requests only weight sorting: weight has no @Sortable, so the client sort is fully ignored (no ORDER BY from
     * the payload), and 8 rows are still returned.
     */
    @Test
    fun search_payloadOrders_whenOnlyNonSortableRequested_noOrderByFromPayload() {
        /**
         * Test payload for non-whitelisted sort.
         *
         * @author K
         * @author AI: Codex
         * @since 1.0.0
         */
        class P : ListSearchPayload() {
            var active: Boolean? = null
        }
        val p = P().apply {
            active = true
            pageNo = 1
            pageSize = 8
            orders = listOf(Order.desc(TestTableKtorm::weight.name))
        }
        val result = testTableDao.search(p)
        assertEquals(8, result.size)
    }

    //endregion @Sortable

}

/**
 * Custom cache-item PO used in the returnItemClass tests for search(criteria, returnItemClass, orders).
 * Its properties match the columns of test_table_ktorm to ease result mapping.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class TestTableCacheItem {
    var id: Int? = null
    var name: String? = null
    var weight: Double? = null
    var height: Int? = null
    val extraProp: String? = null
}

/**
 * Immutable projection object.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal data class TestTableImmutableRecord(
    val id: Int,
    val name: String,
    val weight: Double?,
    val height: Int?,
    val active: Boolean,
    val extraProp: String? = null
)
