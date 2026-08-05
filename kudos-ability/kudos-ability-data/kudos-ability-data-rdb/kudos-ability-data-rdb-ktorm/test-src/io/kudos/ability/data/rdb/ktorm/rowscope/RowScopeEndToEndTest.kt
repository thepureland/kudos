package io.kudos.ability.data.rdb.ktorm.rowscope

import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.springframework.transaction.annotation.Transactional
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue


/**
 * End-to-end test for row-level data-scope filtering: a real table, a real DAO, real SQL.
 *
 * The unit tests next door pin the predicate shape and the switch logic; this one answers the
 * question they cannot — **do rows actually disappear**, on the read paths *and* on the write ones.
 * A boundary that only holds for `search` is not a boundary: whatever a subject cannot see, they
 * must also be unable to update or delete.
 *
 * Fixture (`test_scoped_order`, seeded in data.sql):
 *
 * | id | org | region | creator |
 * |---|---|---|---|
 * | so-1 | org-a | emea | alice |
 * | so-2 | org-a | apac | bob |
 * | so-3 | org-b | emea | bob |
 * | so-4 | org-c | emea | alice |
 * | so-5 | (none) | emea | alice |
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest
internal open class RowScopeEndToEndTest {

    @Resource
    private lateinit var scopedDao: TestScopedOrderDao

    @Resource
    private lateinit var unscopedDao: TestUnscopedOrderDao

    private class StubResolver(var scope: RowScope?) : IRowScopeResolver {
        override fun currentScope(): RowScope? = scope
    }

    /**
     * Installs an enforcer for the duration of one test. The enforcer is a static holder, so the
     * teardown below matters: leaking a restricted scope into the next test would filter unrelated
     * suites and be a nightmare to attribute.
     */
    private fun enforce(scope: RowScope?, shadow: Boolean = false) {
        RowScopeEnforcer.install(
            RowScopeEnforcer(
                RowScopeRegistry(),
                StubResolver(scope),
                RowScopeProperties().apply { enabled = true; shadowMode = shadow },
            ),
        )
    }

    @AfterTest
    fun uninstall() = RowScopeEnforcer.install(null)

    private fun idsOf(rows: List<TestScopedOrder>) = rows.map { it.id }.sorted()

    private fun allOrders(): List<TestScopedOrder> = scopedDao.allSearch()

    private fun restrictedTo(
        orgs: Set<String>? = null,
        regions: Set<String>? = null,
        self: Boolean = false,
        principal: String = "alice",
    ) = RowScope(
        unrestricted = false,
        self = self,
        principalId = principal,
        dimensions = buildMap {
            orgs?.let { put("org", it) }
            regions?.let { put("region", it) }
        },
    )

    // -------------------- the switches --------------------

    @Test
    @Transactional
    open fun disabledLeavesEveryQueryAlone() {
        // No enforcer installed at all — the state every existing test in the framework runs in,
        // and the reason those tests passing proves this change did not alter query building.
        assertEquals(5, allOrders().size)
    }

    @Test
    @Transactional
    open fun shadowModeReturnsEverythingButWouldHaveFiltered() {
        enforce(restrictedTo(orgs = setOf("org-a")), shadow = true)
        assertEquals(5, allOrders().size, "shadow mode must never change a result set")
    }

    @Test
    @Transactional
    open fun anUnrestrictedSubjectSeesEverything() {
        enforce(RowScope.unrestricted("admin"))
        assertEquals(5, allOrders().size)
    }

    // -------------------- read paths --------------------

    @Test
    @Transactional
    open fun oneDimensionFiltersRows() {
        enforce(restrictedTo(orgs = setOf("org-a")))
        assertContentEquals(listOf("so-1", "so-2"), idsOf(allOrders()))
    }

    @Test
    @Transactional
    open fun valuesWithinADimensionAreOred() {
        enforce(restrictedTo(orgs = setOf("org-a", "org-b")))
        assertContentEquals(listOf("so-1", "so-2", "so-3"), idsOf(allOrders()))
    }

    @Test
    @Transactional
    open fun dimensionsAreAndedWithEachOther() {
        // org-a AND emea — so-2 is org-a but apac, so-3 is emea but org-b. Only so-1 satisfies both.
        enforce(restrictedTo(orgs = setOf("org-a", "org-b"), regions = setOf("emea")))
        assertContentEquals(listOf("so-1", "so-3"), idsOf(allOrders()))
    }

    @Test
    @Transactional
    open fun selfIsOredWithTheRestriction() {
        // Restricted to org-a, plus anything alice created. so-4 and so-5 are outside org-a and
        // reachable only through SELF — which is exactly the point of the policy.
        enforce(restrictedTo(orgs = setOf("org-a"), self = true, principal = "alice"))
        assertContentEquals(listOf("so-1", "so-2", "so-4", "so-5"), idsOf(allOrders()))
    }

    @Test
    @Transactional
    open fun theFilterSurvivesAnExplicitCriteria() {
        // The caller's own condition must be ANDed with the scope, not replaced by it.
        enforce(restrictedTo(orgs = setOf("org-a")))
        val criteria = Criteria("regionCode", OperatorEnum.EQ, "emea")
        assertContentEquals(listOf("so-1"), idsOf(scopedDao.search(criteria)))
    }

    @Test
    @Transactional
    open fun countIsFilteredToo() {
        // A count that ignored the scope would leak the size of what the subject cannot see, and
        // would make every paginated screen disagree with itself.
        enforce(restrictedTo(orgs = setOf("org-a")))
        assertEquals(2, scopedDao.count(null as Criteria?))
    }

    @Test
    @Transactional
    open fun getByIdCannotReachOutsideTheScope() {
        // The easiest way around a condition filter is to stop using conditions.
        enforce(restrictedTo(orgs = setOf("org-a")))
        assertNotNull(scopedDao.get("so-1"))
        assertNull(scopedDao.get("so-3"), "an out-of-scope row must not be reachable by primary key")
    }

    @Test
    @Transactional
    open fun anUndeclaredEntityIsUntouched() {
        // Same table, no annotations — opting in is what turns filtering on.
        enforce(restrictedTo(orgs = setOf("org-a")))
        assertEquals(5, unscopedDao.allSearch().size)
    }

    // -------------------- write paths --------------------

    @Test
    @Transactional
    open fun deleteByIdCannotReachOutsideTheScope() {
        enforce(restrictedTo(orgs = setOf("org-a")))
        assertTrue(!scopedDao.deleteById("so-3"), "deleting an invisible row must not succeed")

        RowScopeEnforcer.install(null)
        assertNotNull(scopedDao.get("so-3"), "and the row must still be there")
    }

    @Test
    @Transactional
    open fun deleteByCriteriaOnlyTouchesVisibleRows() {
        enforce(restrictedTo(orgs = setOf("org-a")))
        // Region-wide delete: without the scope this would take so-3, so-4 and so-5 as well.
        val deleted = scopedDao.batchDeleteCriteria(Criteria("regionCode", OperatorEnum.EQ, "emea"))
        assertEquals(1, deleted, "only so-1 is both emea and in scope")

        RowScopeEnforcer.install(null)
        assertContentEquals(listOf("so-2", "so-3", "so-4", "so-5"), idsOf(allOrders()))
    }

    @Test
    @Transactional
    open fun updateByCriteriaOnlyTouchesVisibleRows() {
        enforce(restrictedTo(orgs = setOf("org-a")))
        val updated = scopedDao.batchUpdateProperties(
            Criteria("regionCode", OperatorEnum.EQ, "emea"),
            mapOf("title" to "rewritten"),
        )
        assertEquals(1, updated)

        RowScopeEnforcer.install(null)
        assertEquals("rewritten", scopedDao.get("so-1")!!.title)
        assertTrue(scopedDao.get("so-3")!!.title != "rewritten", "an invisible row must not be rewritten")
    }

    // -------------------- the missing-subject rule --------------------

    @Test
    @Transactional
    open fun aScopedQueryWithNoSubjectFailsLoudly() {
        enforce(scope = null)
        val error = assertFailsWith<RowScopeUnresolvedException> { allOrders() }
        assertTrue(error.message!!.contains("runAsSystem"), error.message!!)
    }

    @Test
    @Transactional
    open fun declaredSystemAuthorityRunsUnfiltered() {
        enforce(scope = null)
        val rows = DataScopeContext.runAsSystem { allOrders() }
        assertEquals(5, rows.size, "a scheduled job may legitimately read everything — once it says so")
    }

    @Test
    @Transactional
    open fun anUndeclaredEntityNeedsNoSubject() {
        // The loud failure has to be scoped to entities that opted in, or every query in the system
        // would suddenly require a logged-in user.
        enforce(scope = null)
        assertEquals(5, unscopedDao.allSearch().size)
    }
}
