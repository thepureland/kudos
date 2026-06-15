package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.ability.data.rdb.ktorm.kit.getDatabase
import io.kudos.ability.data.rdb.ktorm.table.TestTableKtorm
import io.kudos.ability.data.rdb.ktorm.table.TestTableKtormDao
import io.kudos.ability.data.rdb.ktorm.table.TestTableKtorms
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.model.payload.MutableListSearchPayload
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.sort.Order
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.ktorm.dsl.eq
import org.ktorm.dsl.forEach
import org.ktorm.dsl.select
import org.ktorm.dsl.where
import org.ktorm.schema.Column
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Complements [BaseReadOnlyDaoTest] with the result-type / paging / payload branches that the main
 * test class leaves uncovered:
 *  - get(id, returnType) returning null when no row matches
 *  - search / pagingSearch with returnItemClass equal to the table entity class
 *  - search(listSearchPayload, returnItemClass): null payload, MutableListSearchPayload override +
 *    restore, and the returnProperties guard failure
 *  - search(payload) paging fallback when pageNo is null (unpaged disallowed vs allowed) and the
 *    maxPageSize clamp
 *  - payload nullProperties -> IS NULL condition (getWherePropertyMap branch)
 *  - orSearch with a custom whereConditionFactory
 *  - count(searchPayload) single-argument overload
 *  - mapTo (defaults, defaultValues, extraColumns) and the instantiateResultItem error branches
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest
internal open class BaseReadOnlyDaoBranchTest {

    @Resource
    private lateinit var testTableDao: TestTableKtormDao

    //region get / search with returnItemClass

    @Test
    fun get_withReturnType_returnsNullWhenNoRowMatches() {
        assertNull(testTableDao.get(1, TestTableCacheItem::class),
            "a non-entity returnType must yield null (not an empty object) when the id does not exist")
    }

    @Test
    fun search_returnItemClassEqualToEntityClass_nullCriteria_behavesLikeAllSearch() {
        val all = testTableDao.search(null as Criteria?, TestTableKtorm::class)
        assertEquals(11, all.size)

        val criteria = Criteria.of(TestTableKtorm::name.name, OperatorEnum.EQ, "name1")
        val one = testTableDao.search(criteria, TestTableKtorm::class)
        assertEquals(1, one.size)
        assertEquals(-1, one.first().id)
    }

    @Test
    fun pagingSearch_returnItemClassEqualToEntityClass_usesEntityPath() {
        if (!isSupportPaging()) return
        val criteria = Criteria.of(TestTableKtorm::active.name, OperatorEnum.EQ, true)
        val page = testTableDao.pagingSearch(criteria, TestTableKtorm::class, 1, 4, Order.asc(TestTableKtorm::id.name))
        assertEquals(4, page.size)
        assertEquals(-11, page.first().id)
    }

    //endregion

    //region search(listSearchPayload, returnItemClass)

    @Test
    fun searchPayloadWithReturnItemClass_nullPayload_queriesAllRows() {
        val results = testTableDao.search(null as ListSearchPayload?, TestTableCacheItem::class)
        assertEquals(11, results.size)
        assertTrue(results.all { it is TestTableCacheItem })
    }

    @Test
    fun searchPayloadWithReturnItemClass_mutablePayload_overridesAndRestoresReturnEntityClass() {
        val payload = MutableListSearchPayload().apply {
            setReturnEntityClass(TestTableImmutableRecord::class)
            setCriterions(listOf(Criterion(TestTableKtorm::name.name, OperatorEnum.EQ, "name1")))
        }

        val results = testTableDao.search(payload, TestTableCacheItem::class)

        assertEquals(1, results.size)
        assertEquals(-1, results.first().id)
        assertEquals(TestTableImmutableRecord::class, payload.getReturnEntityClass(),
            "the original returnEntityClass must be restored after the call (try/finally contract)")
    }

    @Test
    fun searchPayloadWithReturnItemClass_rejectsNonEmptyReturnProperties() {
        val payload = MutableListSearchPayload().apply {
            setReturnProperties(listOf("id"))
        }
        val ex = assertFailsWith<IllegalArgumentException> {
            testTableDao.search(payload, TestTableCacheItem::class)
        }
        assertTrue(ex.message!!.contains("returnProperties"),
            "the guard must explain why returnProperties conflicts with returnItemClass")
    }

    @Test
    fun searchPayload_namedArguments_skippingFactory_appliesOverrideClass() {
        val payload = MutableListSearchPayload().apply {
            setCriterions(listOf(Criterion(TestTableKtorm::name.name, OperatorEnum.EQ, "name1")))
        }
        val results = testTableDao.search(
            listSearchPayload = payload,
            returnItemClassOverride = TestTableCacheItem::class,
        )
        assertEquals(1, results.size)
        assertTrue(results.first() is TestTableCacheItem)
    }

    //endregion

    //region paging fallback / clamp

    @Test
    fun searchPayload_nullPageNo_unpagedDisallowed_fallsBackToPageOne() {
        // ListSearchPayload.isUnpagedSearchAllowed() defaults to false and pageSize defaults to 10,
        // so a null pageNo must NOT return all 11 rows.
        val payload = MutableListSearchPayload() // no conditions, pageNo = null
        val results = testTableDao.search(payload)
        assertEquals(10, results.size, "null pageNo with unpaged disallowed must fall back to page 1 / size 10")
    }

    @Test
    fun searchPayload_nullPageNo_unpagedAllowed_returnsAllRows() {
        class UnpagedPayload : ListSearchPayload() {
            override fun isUnpagedSearchAllowed() = true
        }
        val results = testTableDao.search(UnpagedPayload())
        assertEquals(11, results.size, "unpaged-allowed payload with null pageNo must skip the limit entirely")
    }

    @Test
    fun searchPayload_pageSizeClampedByMaxPageSize() {
        class ClampedPayload : ListSearchPayload() {
            override fun getMaxPageSize() = 2
        }
        val payload = ClampedPayload().apply {
            pageNo = 1
            pageSize = 10
        }
        val results = testTableDao.search(payload)
        assertEquals(2, results.size, "pageSize must be clamped to getMaxPageSize()")
    }

    //endregion

    //region nullProperties / orSearch factory / count(payload)

    @Test
    fun searchPayload_nullProperties_buildIsNullConditions() {
        val payload = MutableListSearchPayload().apply {
            setNullProperties(listOf(TestTableKtorm::weight.name))
        }
        @Suppress("UNCHECKED_CAST")
        val results = testTableDao.search(payload) as List<TestTableKtorm>
        assertEquals(2, results.size)
        assertEquals(setOf(-5, -11), results.map { it.id }.toSet(),
            "nullProperties entries must be queried as IS NULL even though the bean value is absent")
    }

    @Test
    fun orSearch_withWhereConditionFactory_combinesWithOr() {
        val propertyMap: Map<kotlin.reflect.KProperty1<TestTableKtorm, *>, Any?> =
            mapOf(TestTableKtorm::name to "name5", TestTableKtorm::weight to 56.5)
        val results = testTableDao.orSearch(propertyMap, whereConditionFactory = { column, value ->
            if (column.name == TestTableKtorms.name.name) {
                column ieq (value as String)
            } else null
        })
        // name ieq 'NAME5' OR weight = 56.5  ->  rows -5 and -1
        assertEquals(setOf(-5, -1), results.map { it.id }.toSet())
    }

    @Test
    fun count_singleArgumentPayloadOverload() {
        val payload = MutableListSearchPayload().apply {
            setCriterions(listOf(Criterion(TestTableKtorm::name.name, OperatorEnum.LIKE_S, "name1")))
        }
        assertEquals(3, testTableDao.count(payload))
        assertEquals(11, testTableDao.count(null as MutableListSearchPayload?))
    }

    //endregion

    //region mapTo / instantiateResultItem

    @Test
    fun mapTo_withDefaultArguments_populatesPlainBean() {
        val dao = MapToDao()
        val item = dao.firstRowAs(-1, TestTableCacheItem::class)!!
        assertEquals(-1, item.id)
        assertEquals("name1", item.name)
        assertNull(item.extraProp)
        assertNull(dao.firstRowAs(1, TestTableCacheItem::class), "no matching row must map to null")
    }

    @Test
    fun mapTo_withDefaultValuesAndExtraColumns() {
        @Suppress("UNCHECKED_CAST")
        val extras = mapOf("renamedHeight" to (TestTableKtorms.height as Column<Any>))
        val record = MapToDao().firstRowAsWithExtras(
            -1,
            MapToExtrasRecord::class,
            defaults = mapOf("tag" to "fallback-tag"),
            extras = extras,
        )!!
        assertEquals(-1, record.id)
        assertEquals("name1", record.name)
        assertEquals("fallback-tag", record.tag, "non-column constructor params must come from defaultValues")
        assertEquals(168, record.renamedHeight, "extraColumns must be read from the row and win over getColumns")
    }

    @Test
    fun instantiateResultItem_optionalParamWithNullColumnValue_usesKotlinDefault() {
        // Row -5 has weight NULL; the data-class default must win over the null column value.
        val record = testTableDao.get(-5, OptionalDefaultRecord::class)!!
        assertEquals(-5, record.id)
        assertEquals(99.9, record.weight, "null column value on an optional param must fall back to the Kotlin default")
    }

    @Test
    fun instantiateResultItem_missingRequiredParams_throwsWithParamNames() {
        val ex = assertFailsWith<IllegalArgumentException> {
            testTableDao.get(-1, MissingRequiredRecord::class)
        }
        assertTrue(ex.message!!.contains("[mandatory]"),
            "the error must name the constructor parameters that received no query result, got: ${ex.message}")
    }

    @Test
    fun instantiateResultItem_classWithoutUsableConstructor_throws() {
        val ex = assertFailsWith<IllegalArgumentException> {
            testTableDao.get(-1, NoPrimaryCtor::class)
        }
        assertTrue(ex.message!!.contains("neither a no-arg constructor nor a primary constructor"),
            "got: ${ex.message}")
    }

    //endregion

    private fun isSupportPaging(): Boolean { // h2 can implement paging via PostgreSqlDialect
        val dialect = RdbKit.getDatabase().dialect
        return !dialect::class.java.name.contains($$"SqlDialectKt$detectDialectImplementation$1")
    }

    /** Exposes the protected mapTo for direct testing against real query rows. */
    private class MapToDao : TestTableKtormDao() {
        fun <R : Any> firstRowAs(id: Int, destClass: KClass<R>): R? {
            val query = querySource().select().where { TestTableKtorms.id eq id }
            query.forEach { row -> return mapTo(row, destClass) }
            return null
        }

        fun <R : Any> firstRowAsWithExtras(
            id: Int,
            destClass: KClass<R>,
            defaults: Map<String, Any>,
            extras: Map<String, Column<Any>>,
        ): R? {
            val query = querySource().select().where { TestTableKtorms.id eq id }
            query.forEach { row -> return mapTo(row, destClass, defaults, extras) }
            return null
        }
    }
}

/** Data-class projection whose optional param must fall back to its Kotlin default when the column is NULL. */
internal data class OptionalDefaultRecord(
    val id: Int,
    val weight: Double? = 99.9,
)

/** Data-class projection with a required non-null param that matches no table column. */
internal data class MissingRequiredRecord(
    val id: Int,
    val mandatory: String,
)

/** Projection exercising mapTo defaultValues (tag) + extraColumns (renamedHeight). */
internal data class MapToExtrasRecord(
    val id: Int,
    val name: String,
    val tag: String,
    val renamedHeight: Int?,
)

/** Class with only a secondary constructor (no primary, no no-arg) — must be rejected with a clear error. */
internal class NoPrimaryCtor {
    var id: Int? = null

    constructor(seed: Int) {
        id = seed
    }
}
