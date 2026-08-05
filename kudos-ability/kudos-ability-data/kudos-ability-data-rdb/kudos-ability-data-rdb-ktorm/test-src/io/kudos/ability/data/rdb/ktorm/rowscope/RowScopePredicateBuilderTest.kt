package io.kudos.ability.data.rdb.ktorm.rowscope

import org.ktorm.schema.Table
import org.ktorm.schema.varchar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue


/**
 * junit test for [RowScopePredicateBuilder].
 *
 * The composition rules are the contract other people will rely on, and each of them is a place
 * where the obvious-looking alternative silently widens or silently empties a result set. They are
 * pinned here rather than left to a reading of the builder.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class RowScopePredicateBuilderTest {

    private object Orders : Table<Nothing>("biz_order") {
        val id = varchar("id").primaryKey()
        val orgId = varchar("org_id")
        val regionCode = varchar("region_code")
        val createUserId = varchar("create_user_id")
    }

    /** An entity with no org column at all, for the "dimension does not apply" case. */
    private object Dicts : Table<Nothing>("sys_dict") {
        val id = varchar("id").primaryKey()
        val code = varchar("code")
    }

    private val orderPolicy = RowScopePolicy(
        entityClass = Orders::class,
        dimensionColumns = mapOf("org" to "org_id", "region" to "region_code"),
        selfColumn = "create_user_id",
    )

    private fun sqlOf(expr: org.ktorm.schema.ColumnDeclaring<Boolean>?): String =
        expr?.asExpression().toString()

    @Test
    fun unrestrictedScopeAppendsNothing() {
        val predicate = RowScopePredicateBuilder.build(Orders, orderPolicy, RowScope.unrestricted("u1"))
        assertNull(predicate, "a platform administrator's query must be left exactly as written")
    }

    @Test
    fun anEntityThatDeclaresNothingIsNeverFiltered() {
        val policy = RowScopePolicy(Dicts::class)
        assertTrue(!policy.participates)
        val scope = RowScope(unrestricted = false, self = false, principalId = "u1", dimensions = mapOf("org" to setOf("o1")))
        assertNull(RowScopePredicateBuilder.build(Dicts, policy, scope))
    }

    @Test
    fun oneDimensionBecomesAnInList() {
        val scope = RowScope(false, self = false, principalId = "u1", dimensions = mapOf("org" to setOf("o1", "o2")))
        val sql = sqlOf(RowScopePredicateBuilder.build(Orders, orderPolicy, scope))
        assertTrue(sql.contains("org_id"), sql)
        assertTrue(sql.contains("in", ignoreCase = true), sql)
        assertTrue(!sql.contains("region_code"), "an unrestricted dimension must not appear: $sql")
    }

    @Test
    fun severalDimensionsAreAnded() {
        // Each dimension narrows independently: org A *and* region EMEA, never "or".
        val scope = RowScope(
            false, self = false, principalId = "u1",
            dimensions = mapOf("org" to setOf("o1"), "region" to setOf("emea")),
        )
        val sql = sqlOf(RowScopePredicateBuilder.build(Orders, orderPolicy, scope))
        assertTrue(sql.contains("org_id"), sql)
        assertTrue(sql.contains("region_code"), sql)
        assertTrue(sql.contains("and", ignoreCase = true), sql)
    }

    @Test
    fun selfIsOredWithTheWholeRestriction() {
        // (dim1 AND dim2) OR creator = me — not AND, or a user could never see their own row from
        // outside their org, which is the entire point of the SELF policy.
        val scope = RowScope(
            false, self = true, principalId = "u1",
            dimensions = mapOf("org" to setOf("o1")),
        )
        val sql = sqlOf(RowScopePredicateBuilder.build(Orders, orderPolicy, scope))
        assertTrue(sql.contains("or", ignoreCase = true), sql)
        assertTrue(sql.contains("create_user_id"), sql)
    }

    @Test
    fun selfAloneIsTheWholePredicate() {
        val scope = RowScope(false, self = true, principalId = "u1")
        val sql = sqlOf(RowScopePredicateBuilder.build(Orders, orderPolicy, scope))
        assertTrue(sql.contains("create_user_id"), sql)
        assertTrue(!sql.contains("org_id"), sql)
    }

    @Test
    fun aDimensionTheEntityDoesNotDeclareIsSkipped() {
        // Scope means "which rows of this kind of thing"; an entity with no brand column has no
        // brand-ness. Failing closed here would make every unrelated entity vanish the moment a
        // second dimension is configured.
        val scope = RowScope(false, self = false, principalId = "u1", dimensions = mapOf("brand" to setOf("acme")))
        assertNull(RowScopePredicateBuilder.build(Orders, orderPolicy, scope))
    }

    @Test
    fun anEmptyValueSetDoesNotRestrict() {
        // "Not restricted" and "restricted to nothing" must never be confused: the second would
        // hide every row behind an impossible predicate.
        val scope = RowScope(false, self = false, principalId = "u1", dimensions = mapOf("org" to emptySet()))
        assertNull(RowScopePredicateBuilder.build(Orders, orderPolicy, scope))
    }

    @Test
    fun aDeclarationPointingAtAMissingColumnFailsLoudly() {
        // An unenforceable rule must not read as "allow everything" — that is how a renamed column
        // silently turns a protected table into an open one.
        val broken = RowScopePolicy(Orders::class, mapOf("org" to "no_such_column"))
        val scope = RowScope(false, self = false, principalId = "u1", dimensions = mapOf("org" to setOf("o1")))
        val error = assertFailsWith<RowScopeUnresolvedException> {
            RowScopePredicateBuilder.build(Orders, broken, scope)
        }
        assertTrue(error.message!!.contains("no_such_column"), error.message!!)
    }

    @Test
    fun describeRendersSomethingAnOperatorCanRead() {
        val scope = RowScope(
            false, self = true, principalId = "u1",
            dimensions = mapOf("org" to setOf("o1", "o2")),
        )
        val text = RowScopePredicateBuilder.describe(orderPolicy, scope)
        assertTrue(text.contains("org_id"), text)
        assertTrue(text.contains("create_user_id"), text)
        assertEquals("(unrestricted)", RowScopePredicateBuilder.describe(orderPolicy, RowScope.unrestricted("u1")))
    }

    @Test
    fun systemContextIsReentrantAndCleansUp() {
        assertTrue(!DataScopeContext.isSystem())
        DataScopeContext.runAsSystem {
            assertTrue(DataScopeContext.isSystem())
            DataScopeContext.runAsSystem {
                assertTrue(DataScopeContext.isSystem())
            }
            assertTrue(DataScopeContext.isSystem(), "a nested block must not clear the marker early")
        }
        assertTrue(!DataScopeContext.isSystem(), "and the marker must not leak past the outermost block")
    }

    @Test
    fun registryReadsAnnotationsAndAuditsPartialDeclarations() {
        val registry = RowScopeRegistry()
        val policy = registry.policyOf(AnnotatedOrder::class)
        assertEquals(mapOf("org" to "org_id"), policy.dimensionColumns)
        assertEquals("create_user_id", policy.selfColumn)
        assertNotNull(registry.policyOf(Dicts::class))
        assertTrue(!registry.policyOf(Dicts::class).participates)

        // Declaring org but not region is the failure worth catching: the entity looks protected.
        val findings = registry.auditPartialDeclarations(setOf("org", "region"))
        assertTrue(findings.any { it.contains("AnnotatedOrder") && it.contains("region") }, findings.toString())
    }

    @DataScoped(dimension = "org", column = "org_id")
    @DataScopedSelf(column = "create_user_id")
    private interface AnnotatedOrder
}
