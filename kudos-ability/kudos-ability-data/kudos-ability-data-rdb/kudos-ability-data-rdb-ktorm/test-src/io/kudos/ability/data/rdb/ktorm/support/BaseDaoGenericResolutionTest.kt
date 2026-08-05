package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.ability.data.rdb.ktorm.table.TestTableKtorm
import io.kudos.ability.data.rdb.ktorm.table.TestTableKtormDao
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.logic.AndOrEnum
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the generic-resolution and expression-building parts of [BaseReadOnlyDao] that
 * do not need a database:
 *  - `table()` must reject DAOs whose table generic type is not a Kotlin `object` singleton
 *    (a plain class would silently create disconnected table instances per call).
 *  - `processWhere` called with its default arguments (`ignoreNull=false`, no factory) builds
 *    the expected expressions, including the null-value → IS NULL defaulting and the
 *    "no expression produced" null result.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class BaseDaoGenericResolutionTest {

    /** Deliberately a class (NOT an object) to trigger the table() singleton guard. */
    internal class NonObjectTable : IntIdTable<TestTableKtorm>("test_table_ktorm")

    /** DAO bound to the non-object table type above. */
    internal class BadTableDao : BaseReadOnlyDao<Int, TestTableKtorm, NonObjectTable>() {
        fun tableForTest(): NonObjectTable = table()
    }

    /** Exposes the protected processWhere with its default arguments for direct testing. */
    internal class ProcessWhereDao : TestTableKtormDao() {
        fun processWhereWithDefaults(propertyMap: Map<String, Pair<OperatorEnum, *>>, andOr: AndOrEnum) =
            processWhere(propertyMap, andOr)
    }

    @Test
    fun table_rejectsNonObjectTableType() {
        val ex = assertFailsWith<IllegalArgumentException> { BadTableDao().tableForTest() }
        assertTrue(
            ex.message!!.contains("must be an object singleton"),
            "the error must explain the object-singleton requirement, got: ${ex.message}"
        )
        assertTrue(ex.message!!.contains("NonObjectTable"), "the error must name the offending table class")
    }

    @Test
    fun processWhere_defaultArguments_buildAndCombinedExpression() {
        val expr = ProcessWhereDao().processWhereWithDefaults(
            mapOf(
                "name" to (OperatorEnum.EQ to "name1"),
                "height" to (OperatorEnum.GT to 100),
            ),
            AndOrEnum.AND,
        )
        assertNotNull(expr, "two valid conditions must combine into a non-null expression")
    }

    @Test
    fun processWhere_defaultArguments_nullValueBecomesIsNull() {
        // With the default ignoreNull=false and no factory, a null value must produce IS NULL
        // rather than dropping the condition.
        val expr = ProcessWhereDao().processWhereWithDefaults(
            mapOf("weight" to (OperatorEnum.EQ to null)),
            AndOrEnum.OR,
        )
        assertNotNull(expr)
    }

    @Test
    fun processWhere_defaultArguments_emptyMapYieldsNull() {
        val expr = ProcessWhereDao().processWhereWithDefaults(emptyMap(), AndOrEnum.AND)
        assertNull(expr, "no conditions and no factory must yield null so callers skip the where clause")
    }
}
