package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.ability.data.rdb.ktorm.table.TestTableKtorm
import io.kudos.ability.data.rdb.ktorm.table.TestTableKtormDao
import io.kudos.ability.data.rdb.ktorm.table.TestTableKtorms
import io.kudos.base.model.payload.MutableListSearchPayload
import io.kudos.base.model.payload.UpdatePayload
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.sort.Order
import io.kudos.base.support.logic.AndOrEnum
import io.kudos.test.common.init.EnableKudosTest
import org.ktorm.dsl.Query
import org.ktorm.dsl.forEach
import org.ktorm.schema.Table
import org.springframework.transaction.annotation.Transactional
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.isAccessible
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Coverage for code paths that are unreachable through regular Kotlin call sites:
 *  - private helper functions of [BaseReadOnlyDao] whose default-argument bridges are never used by
 *    the production call sites (every internal caller passes all arguments explicitly) — invoked via
 *    kotlin-reflect `callBy`, which exercises the `$default` synthetic and runs the real query logic
 *  - the unused private single-property IN-search helpers (`inSearchProperty` / `inSearchProperties`)
 *  - public functions with default arguments shadowed by a narrower overload (count / batchUpdateWhen)
 *  - `processWhere` rejecting a (type-system-defeating) null operator/value pair
 *  - the `filterOrdersBySortWhitelist` entity-name fallback when the table is not bound to an entity
 *  - the compiled (non-inlined) bodies of the reified inline shortcuts (`getAs` / `getByIdsAs` /
 *    `searchAs` / `pagingSearchAs`), which by Kotlin's design can only throw when invoked reflectively
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest
internal open class BaseDaoSyntheticBridgesTest {

    private val dao = TestTableKtormDao()

    /** Looks up a (possibly private) member function by name and value-parameter count. */
    private fun daoFunction(name: String, valueParamCount: Int): KFunction<*> =
        BaseReadOnlyDao::class.declaredMemberFunctions
            .first { fn -> fn.name == name && fn.parameters.count { it.kind == KParameter.Kind.VALUE } == valueParamCount }
            .apply { isAccessible = true }

    /** Builds a callBy argument map: instance + the given value-parameter assignments by name. */
    private fun argsOf(fn: KFunction<*>, instance: Any, vararg pairs: Pair<String, Any?>): Map<KParameter, Any?> {
        val map = mutableMapOf<KParameter, Any?>()
        map[fn.parameters.first { it.kind == KParameter.Kind.INSTANCE }] = instance
        pairs.forEach { (paramName, value) ->
            map[fn.parameters.first { it.name == paramName }] = value
        }
        return map
    }

    //region private default-argument bridges (callBy omits the optional params)

    @Test
    fun doSearchEntity_defaultFactory_queriesByEquality() {
        val fn = daoFunction("doSearchEntity", 4)
        val result = fn.callBy(argsOf(fn, dao,
            "propertyMap" to mapOf(TestTableKtorm::name.name to "name1"),
            "logic" to AndOrEnum.AND,
            "orders" to emptyArray<Order>(),
        )) as List<*>
        assertEquals(1, result.size)
        assertEquals(-1, (result.first() as TestTableKtorm).id)
    }

    @Test
    fun doSearchProperties_defaultFactory_returnsRequestedColumns() {
        val fn = daoFunction("doSearchProperties", 5)
        @Suppress("UNCHECKED_CAST")
        val result = fn.callBy(argsOf(fn, dao,
            "propertyMap" to mapOf(TestTableKtorm::name.name to "name1"),
            "logic" to AndOrEnum.AND,
            "returnProperties" to listOf(TestTableKtorm::id.name),
            "orders" to emptyArray<Order>(),
        )) as List<Map<String, *>>
        assertEquals(1, result.size)
        assertEquals(-1, result.first()[TestTableKtorm::id.name])
    }

    @Test
    fun searchEntityCriteria_defaultPaging_returnsAllRows() {
        val fn = daoFunction("searchEntityCriteria", 4)
        val result = fn.callBy(argsOf(fn, dao,
            "criteria" to null,
            "orders" to emptyArray<Order>(),
        )) as List<*>
        assertEquals(11, result.size)
    }

    @Test
    fun searchPropertiesCriteria_defaultPaging_returnsAllMatches() {
        val fn = daoFunction("searchPropertiesCriteria", 5)
        @Suppress("UNCHECKED_CAST")
        val result = fn.callBy(argsOf(fn, dao,
            "criteria" to Criteria.of(TestTableKtorm::name.name, OperatorEnum.LIKE_S, "name1"),
            "returnProperties" to listOf(TestTableKtorm::id.name),
            "orders" to emptyArray<Order>(),
        )) as List<Map<String, *>>
        assertEquals(3, result.size)
    }

    @Test
    fun prepareQuery_defaultPaging_buildsUnlimitedQuery() {
        val fn = daoFunction("prepareQuery", 5)
        val query = fn.callBy(argsOf(fn, dao,
            "criteria" to null,
            "returnColumnMap" to ColumnHelper.columnOf(TestTableKtorms, TestTableKtorm::id.name),
            "orders" to emptyArray<Order>(),
        )) as Query
        var rows = 0
        query.forEach { _ -> rows++ }
        assertEquals(11, rows)
    }

    @Test
    fun searchByPayload_allDefaults_returnsQueryAndColumnMetadata() {
        val fn = daoFunction("searchByPayload", 3)
        val result = fn.callBy(argsOf(fn, dao)) as List<*>
        assertEquals(3, result.size)
        assertIs<Query>(result[0])
        @Suppress("UNCHECKED_CAST")
        val returnProps = result[1] as Set<String>
        assertTrue(TestTableKtorm::name.name in returnProps)
    }

    @Test
    fun count_twoArgOverload_defaultFactory_viaCallBy() {
        val fn = daoFunction("count", 2)
        assertEquals(11, fn.callBy(argsOf(fn, dao, "searchPayload" to null)))
    }

    @Test
    @Transactional
    open fun batchUpdateWhen_twoArgOverload_defaultFactory_viaCallBy() {
        class UP : UpdatePayload<MutableListSearchPayload>() {
            var name: String? = null
        }

        val payload = UP().apply {
            name = "bridge-renamed"
            searchPayload = MutableListSearchPayload().apply {
                setCriterions(listOf(Criterion(TestTableKtorm::name.name, OperatorEnum.EQ, "name1")))
            }
        }
        val fn = BaseCrudDao::class.declaredMemberFunctions
            .first { f -> f.name == "batchUpdateWhen" && f.parameters.count { it.kind == KParameter.Kind.VALUE } == 2 }
            .apply { isAccessible = true }
        val count = fn.callBy(argsOf(fn, dao, "updatePayload" to payload))
        assertEquals(1, count)
        assertEquals("bridge-renamed", dao.get(-1)!!.name)
    }

    //endregion

    //region unused private IN-search helpers

    @Test
    fun inSearchProperty_privateHelper_returnsSingleColumnValues() {
        val fn = daoFunction("inSearchProperty", 4)
        val result = fn.callBy(argsOf(fn, dao,
            "property" to TestTableKtorm::name.name,
            "values" to listOf("name1", "name2"),
            "returnProperty" to TestTableKtorm::id.name,
            "orders" to emptyArray<Order>(),
        )) as List<*>
        assertEquals(setOf(-1, -2), result.toSet())
    }

    @Test
    fun inSearchProperties_privateHelper_returnsColumnMaps() {
        val fn = daoFunction("inSearchProperties", 4)
        @Suppress("UNCHECKED_CAST")
        val result = fn.callBy(argsOf(fn, dao,
            "property" to TestTableKtorm::name.name,
            "values" to listOf("name1", "name2"),
            "returnProperties" to listOf(TestTableKtorm::id.name, TestTableKtorm::name.name),
            "orders" to emptyArray<Order>(),
        )) as List<Map<String, *>>
        assertEquals(2, result.size)
        assertEquals(setOf("name1", "name2"), result.map { it[TestTableKtorm::name.name] }.toSet())
    }

    //endregion

    //region defensive branches

    @Test
    fun processWhere_nullOperatorValuePair_failsFastWithPropertyName() {
        val raw = HashMap<String, Pair<OperatorEnum, *>?>()
        raw[TestTableKtorm::name.name] = null
        @Suppress("UNCHECKED_CAST")
        val corrupted = raw as Map<String, Pair<OperatorEnum, *>>

        val ex = assertFailsWith<IllegalArgumentException> {
            ProcessWhereExposingDao().processWhereForTest(corrupted, AndOrEnum.AND)
        }
        assertTrue(ex.message!!.contains("missing a query operator"), "got: ${ex.message}")
        assertTrue(ex.message!!.contains(TestTableKtorm::name.name))
    }

    @Test
    fun filterOrdersBySortWhitelist_unboundEntityTable_logsUnknownAndFiltersAll() {
        val filterDao = SortExposingDao()
        // Inject a table whose entity type resolves to Nothing -> BaseTable.entityClass == null
        val tableField = BaseReadOnlyDao::class.java.getDeclaredField("table")
        tableField.isAccessible = true
        tableField.set(filterDao, UnboundEntityTable("test_table_ktorm"))

        assertEquals(emptySet(), filterDao.sortWhitelistForTest(),
            "an entity-less table cannot expose @Sortable properties")
        val filtered = filterDao.filterForTest(listOf(Order.desc(TestTableKtorm::weight.name)), emptySet())
        assertTrue(filtered.isEmpty(), "every order must be rejected against an empty whitelist")
    }

    //endregion

    //region reified inline shortcuts (compiled stubs)

    @Test
    fun reifiedInlineShortcuts_cannotBeInvokedReflectively() {
        // The compiled (non-inlined) bodies of reified inline functions contain a reification marker
        // that throws UnsupportedOperationException by Kotlin's design. Locking this documents that the
        // shortcuts are compile-time-only sugar; their real behavior is tested via the inlined call
        // sites in BaseReadOnlyDaoTest.
        fun assertReifiedStubThrows(block: () -> Unit) {
            val ex = assertFailsWith<InvocationTargetException>(block = block)
            assertIs<UnsupportedOperationException>(ex.cause,
                "the reification marker must reject a non-inlined invocation")
        }

        val daoClass = BaseReadOnlyDao::class.java

        val getAs = daoClass.declaredMethods.first { it.name == "getAs" }
        getAs.isAccessible = true
        assertReifiedStubThrows { getAs.invoke(dao, -1) }

        val getByIdsAsDefault = daoClass.declaredMethods.first { it.name == "getByIdsAs\$default" }
        getByIdsAsDefault.isAccessible = true
        // mask bit 1 -> countOfEachBatch omitted, evaluating its default value (1000) before the marker throws
        assertReifiedStubThrows { getByIdsAsDefault.invoke(null, dao, setOf(-1), 0, 2, null) }

        val searchAsDefault = daoClass.declaredMethods.first { it.name == "searchAs\$default" }
        searchAsDefault.isAccessible = true
        // mask bit 0 -> criteria omitted, evaluating its default value (null) before the marker throws
        assertReifiedStubThrows { searchAsDefault.invoke(null, dao, null, emptyArray<Order>(), 1, null) }

        val pagingSearchAs = daoClass.declaredMethods.first { it.name == "pagingSearchAs" }
        pagingSearchAs.isAccessible = true
        assertReifiedStubThrows { pagingSearchAs.invoke(dao, null, 1, 4, emptyArray<Order>()) }
    }

    //endregion

    @Test
    fun entityClassResolution_isCachedAndMatchesTableEntity() {
        // Sanity companion to the bridge tests: the DAO resolves the entity class from its generics.
        val exposingDao = ProcessWhereExposingDao()
        assertNotNull(exposingDao.entityClassForTest())
        assertEquals(TestTableKtorm::class, exposingDao.entityClassForTest())
    }

    /** Exposes protected members for the defensive-branch tests. */
    private class ProcessWhereExposingDao : TestTableKtormDao() {
        fun processWhereForTest(map: Map<String, Pair<OperatorEnum, *>>, andOr: AndOrEnum) = processWhere(map, andOr)
        fun entityClassForTest() = entityClass()
    }

    /** Exposes the protected sort helpers for the unbound-table fallback test. */
    private class SortExposingDao : TestTableKtormDao() {
        fun sortWhitelistForTest() = sortWhitelistFromPo()
        fun filterForTest(raw: List<Order>?, whitelist: Set<String>) = filterOrdersBySortWhitelist(raw, whitelist)
    }
}

/**
 * Table type bound to `Nothing`, which ktorm's `BaseTable` resolves to a null `entityClass` — used
 * to reach the "unknown entity" logging fallback in `filterOrdersBySortWhitelist`.
 */
private class UnboundEntityTable(name: String) : Table<Nothing>(name)
