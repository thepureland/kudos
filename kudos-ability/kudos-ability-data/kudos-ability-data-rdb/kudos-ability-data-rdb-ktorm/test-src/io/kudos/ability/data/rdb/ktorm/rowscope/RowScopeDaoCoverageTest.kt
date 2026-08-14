package io.kudos.ability.data.rdb.ktorm.rowscope

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.model.payload.UpdatePayload
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.ktorm.dsl.eq
import org.springframework.transaction.annotation.Transactional
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue


/** Plain (non-entity) projection type, to exercise the "returnItemClass" query paths. */
internal class ScopedOrderView {
    var id: String? = null
    var title: String? = null
    var orgId: String? = null
    var regionCode: String? = null
}

/**
 * Payloads are top-level rather than nested in the test class because the DAO reads them through
 * commons-beanutils, which cannot see the accessors of a private nested class.
 *
 * [isUnpagedSearchAllowed] is overridden so the payload search skips its paging branch — ktorm's
 * core dialect cannot render limit/offset, and paging is not what these tests are about.
 */
internal open class ScopedOrderSearchPayload : ListSearchPayload() {
    var orgId: String? = null
    var regionCode: String? = null
    var returnPropertiesField: List<String>? = null
    override fun getReturnProperties(): List<String>? = returnPropertiesField
    override fun isUnpagedSearchAllowed(): Boolean = true
}

internal class ScopedOrderUpdatePayload : UpdatePayload<ScopedOrderSearchPayload>() {
    var title: String? = null
}


/**
 * The coverage matrix for row-level filtering: the DAO entry points [RowScopeEndToEndTest] does not
 * reach, asserted against the same real table and real SQL.
 *
 * This test exists because the gap it closes was invisible by inspection. The scope was applied by
 * `readSequence()`, which covers the entity-sequence queries and quietly missed every query that
 * builds its own `where` — the projection reads, `get(id, returnType)`, `update(entity)`, and the
 * `ListSearchPayload` search that the service layer actually calls most. From a call site, a filter
 * that covers most methods reads exactly like one that covers all of them, which is why the end-to-end
 * test passing was not evidence that the boundary held.
 *
 * The rule going forward: one assertion per public entry point, and a new DAO method arrives with a
 * new line here. Grouping is by query family only, so a failure names the family that broke.
 *
 * Fixture (`test_scoped_order`, seeded in data.sql) — see [RowScopeEndToEndTest] for the full matrix.
 * Every test below restricts the subject to `org-a`, which makes so-1 and so-2 visible and hides
 * so-3, so-4 and so-5.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest
internal open class RowScopeDaoCoverageTest {

    @Resource
    private lateinit var dao: TestScopedOrderDao

    private class StubResolver(private val scope: RowScope?) : IRowScopeResolver {
        override fun currentScope(): RowScope? = scope
    }

    /** Subject restricted to `org-a`: so-1 and so-2 are visible, the other three are not. */
    private fun restrictToOrgA() = enforce(
        RowScope(unrestricted = false, self = false, principalId = "alice", dimensions = mapOf("org" to setOf("org-a")))
    )

    private fun enforce(scope: RowScope?) {
        RowScopeEnforcer.install(
            RowScopeEnforcer(
                RowScopeRegistry(),
                StubResolver(scope),
                RowScopeProperties().apply { enabled = true; shadowMode = false },
            ),
        )
    }

    /** The enforcer is a static holder; leaking a restricted scope would filter unrelated suites. */
    @AfterTest
    fun uninstall() = RowScopeEnforcer.install(null)

    /** Reads the row back with filtering off, to tell "was not written" from "cannot be seen". */
    private fun unfiltered(id: String): TestScopedOrder? {
        RowScopeEnforcer.install(null)
        return dao.get(id)
    }

    //region reads: by id

    @Test
    @Transactional
    open fun getByIdAsProjection_cannotReachOutsideTheScope() {
        // The projection overload builds its own query instead of going through readSequence(), and
        // so ran unfiltered while the get(id) right next to it was filtered.
        restrictToOrgA()
        assertEquals("so-1", dao.get("so-1", ScopedOrderView::class)?.id)
        assertNull(dao.get("so-3", ScopedOrderView::class), "get(id, returnType) must be scoped like get(id)")
    }

    @Test
    @Transactional
    open fun getByIds_dropsRowsOutsideTheScope() {
        restrictToOrgA()
        assertContentEquals(
            listOf("so-1", "so-2"),
            dao.getByIds(listOf("so-1", "so-2", "so-3")).map { it.id }.sorted(),
        )
    }

    @Test
    @Transactional
    open fun getByIdsAsProjection_dropsRowsOutsideTheScope() {
        restrictToOrgA()
        val rows = dao.getByIds(listOf("so-1", "so-2", "so-3"), ScopedOrderView::class)
        assertContentEquals(listOf("so-1", "so-2"), rows.mapNotNull { it.id }.sorted())
    }

    //endregion

    //region reads: projections and property lists

    @Test
    @Transactional
    open fun allSearchProperty_returnsOnlyVisibleRows() {
        restrictToOrgA()
        assertEquals(2, dao.allSearchProperty(TestScopedOrder::title).size)
    }

    @Test
    @Transactional
    open fun allSearchProperties_returnsOnlyVisibleRows() {
        restrictToOrgA()
        assertEquals(2, dao.allSearchProperties(listOf(TestScopedOrder::id, TestScopedOrder::title)).size)
    }

    @Test
    @Transactional
    open fun oneSearch_cannotSelectAnotherOrg() {
        restrictToOrgA()
        assertEquals(2, dao.oneSearch(TestScopedOrder::orgId, "org-a").size)
        assertTrue(dao.oneSearch(TestScopedOrder::orgId, "org-b").isEmpty(), "asking for another org yields nothing")
    }

    @Test
    @Transactional
    open fun oneSearchProperty_cannotSelectAnotherOrg() {
        restrictToOrgA()
        assertTrue(dao.oneSearchProperty(TestScopedOrder::orgId, "org-b", TestScopedOrder::title).isEmpty())
    }

    @Test
    @Transactional
    open fun oneSearchProperties_cannotSelectAnotherOrg() {
        restrictToOrgA()
        assertTrue(dao.oneSearchProperties(TestScopedOrder::orgId, "org-b", listOf(TestScopedOrder::title)).isEmpty())
    }

    @Test
    @Transactional
    open fun andSearchAndOrSearch_cannotSelectAnotherOrg() {
        restrictToOrgA()
        assertTrue(dao.andSearch(mapOf(TestScopedOrder::orgId to "org-b")).isEmpty())
        assertTrue(dao.orSearch(mapOf(TestScopedOrder::orgId to "org-b")).isEmpty())
    }

    @Test
    @Transactional
    open fun inSearch_dropsRowsOutsideTheScope() {
        restrictToOrgA()
        val ids = listOf("so-1", "so-2", "so-3")
        assertEquals(2, dao.inSearchById(ids).size)
        assertEquals(2, dao.inSearchPropertyById(ids, TestScopedOrder::title).size)
        assertEquals(2, dao.inSearchPropertiesById(ids, listOf("id", "title")).size)
    }

    @Test
    @Transactional
    open fun searchByCriteria_projectionPaths_dropRowsOutsideTheScope() {
        restrictToOrgA()
        val criteria = Criteria(TestScopedOrder::regionCode.name, OperatorEnum.IS_NOT_NULL, null)
        assertEquals(2, dao.search(criteria, ScopedOrderView::class).size)
        assertEquals(2, dao.searchProperty(criteria, TestScopedOrder::title).size)
        assertEquals(2, dao.searchProperties(criteria, listOf(TestScopedOrder::id)).size)
    }

    //endregion

    //region reads: payload — the entry point the service layer actually uses

    @Test
    @Transactional
    open fun searchByPayload_returnsOnlyVisibleRows() {
        // The most-used query in the framework, and the one that ran completely unfiltered.
        restrictToOrgA()
        assertEquals(2, dao.search(ScopedOrderSearchPayload()).size)
    }

    @Test
    @Transactional
    open fun searchByPayloadAsProjection_returnsOnlyVisibleRows() {
        restrictToOrgA()
        assertEquals(2, dao.search(ScopedOrderSearchPayload(), ScopedOrderView::class).size)
    }

    @Test
    @Transactional
    open fun searchByPayload_cannotSelectAnotherOrg() {
        restrictToOrgA()
        assertTrue(dao.search(ScopedOrderSearchPayload().apply { orgId = "org-b" }).isEmpty())
    }

    @Test
    @Transactional
    open fun searchMaps_returnsOnlyVisibleRows() {
        restrictToOrgA()
        assertEquals(2, dao.searchMaps(ScopedOrderSearchPayload().apply { returnPropertiesField = listOf("id", "title") }).size)
    }

    @Test
    @Transactional
    open fun searchValues_returnsOnlyVisibleRows() {
        restrictToOrgA()
        assertEquals(2, dao.searchValues(ScopedOrderSearchPayload().apply { returnPropertiesField = listOf("title") }).size)
    }

    @Test
    @Transactional
    open fun searchMapsWithAWhereFactory_returnsOnlyVisibleRows() {
        // A custom where generator narrows the query; it must not be able to widen it past the scope.
        restrictToOrgA()
        val rows = dao.searchMaps(ScopedOrderSearchPayload().apply { returnPropertiesField = listOf("id") }) { column, _ ->
            if (column.name == "region_code") column.eq("emea") else null
        }
        assertEquals(1, rows.size, "only so-1 is emea and in scope")
    }

    @Test
    @Transactional
    open fun pagingSearchWithTotalByCriteria_pageAndTotalAreBothScoped() {
        // The total is the half that leaks quietly: a correct page beside a total counted over every
        // tenant's rows still tells the caller how much they cannot see.
        restrictToOrgA()
        val page = dao.pagingSearchWithTotal(null, 1, 10)
        assertEquals(2, page.data.size)
        assertEquals(2, page.totalCount)
    }

    @Test
    @Transactional
    open fun pagingSearchWithTotalByPayload_pageAndTotalAreBothScoped() {
        restrictToOrgA()
        val page = dao.pagingSearchWithTotal(ScopedOrderSearchPayload(), ScopedOrderView::class)
        assertEquals(2, page.data.size)
        assertEquals(2, page.totalCount)
    }

    @Test
    @Transactional
    open fun countByPayload_countsOnlyVisibleRows() {
        // A count that ignored the scope leaks the size of what the subject cannot see, and makes
        // every paginated screen disagree with its own page count.
        restrictToOrgA()
        assertEquals(2, dao.count(ListSearchPayload()))
    }

    //endregion

    //region reads: aggregates

    @Test
    @Transactional
    open fun aggregates_seeOnlyVisibleRows() {
        // Of the five titles, only so-1's and so-2's are in scope; so-4's ('org-c …') would win the
        // max outright if the aggregate ran unfiltered.
        restrictToOrgA()
        assertEquals("org-a emea, by alice", dao.max(TestScopedOrder::title, null))
        assertEquals("org-a apac, by bob", dao.min(TestScopedOrder::title, null))
    }

    //endregion

    //region writes

    @Test
    @Transactional
    open fun update_cannotTouchARowOutsideTheScope() {
        // Ktorm's EntitySequence.update() writes WHERE id = ?, so this is the write that most easily
        // reaches a row the subject cannot even read.
        restrictToOrgA()
        assertFalse(dao.update(TestScopedOrder { id = "so-3"; title = "hacked" }))
        assertEquals("org-b emea, by bob", unfiltered("so-3")!!.title)

        restrictToOrgA()
        assertTrue(dao.update(TestScopedOrder { id = "so-1"; title = "renamed" }), "a visible row is still updatable")
        assertEquals("renamed", unfiltered("so-1")!!.title)
    }

    @Test
    @Transactional
    open fun updateProperties_cannotTouchARowOutsideTheScope() {
        restrictToOrgA()
        assertFalse(dao.updateProperties("so-3", mapOf(TestScopedOrder::title.name to "hacked")))
        assertEquals("org-b emea, by bob", unfiltered("so-3")!!.title)
    }

    @Test
    @Transactional
    open fun updateOnly_cannotTouchARowOutsideTheScope() {
        restrictToOrgA()
        val entity = TestScopedOrder { id = "so-3"; title = "hacked" }
        assertFalse(dao.updateOnly(entity, TestScopedOrder::title.name))
        assertEquals("org-b emea, by bob", unfiltered("so-3")!!.title)
    }

    @Test
    @Transactional
    open fun batchUpdateByPayload_onlyTouchesVisibleRows() {
        restrictToOrgA()
        val payload = ScopedOrderUpdatePayload().apply {
            title = "rewritten"
            searchPayload = ScopedOrderSearchPayload().apply { regionCode = "emea" }
        }
        // Four rows are emea; only so-1 is emea *and* in scope.
        assertEquals(1, dao.batchUpdateWhen(payload))
        assertEquals("rewritten", unfiltered("so-1")!!.title)
        assertEquals("org-b emea, by bob", unfiltered("so-3")!!.title)
    }

    //endregion

    //region writes: the values being stored, not just the rows being touched

    @Test
    @Transactional
    open fun insert_cannotCreateARowInAnotherOrg() {
        // Filtering has nothing to narrow here — the row does not exist yet — so this is a check on
        // the values. Without it the row is created, invisible to its creator, and entirely real to
        // org-b.
        restrictToOrgA()
        val error = assertFailsWith<RowScopeViolationException> {
            dao.insert(TestScopedOrder { id = "new-1"; title = "smuggled"; orgId = "org-b" })
        }
        assertTrue(error.message!!.contains("org-b"), error.message!!)
        assertNull(unfiltered("new-1"), "nothing may have been written")
    }

    @Test
    @Transactional
    open fun insert_fillsTheOrgWhenTheSubjectHasExactlyOne() {
        // The ergonomics that make this adoptable: one org means the caller never writes org_id.
        restrictToOrgA()
        dao.insert(TestScopedOrder { id = "new-2"; title = "mine" })
        assertEquals("org-a", unfiltered("new-2")!!.orgId)
    }

    @Test
    @Transactional
    open fun insert_intoAnOrgTheSubjectHoldsIsAllowed() {
        restrictToOrgA()
        dao.insert(TestScopedOrder { id = "new-3"; title = "mine"; orgId = "org-a" })
        assertEquals("org-a", unfiltered("new-3")!!.orgId)
    }

    @Test
    @Transactional
    open fun batchInsert_cannotCreateRowsInAnotherOrg() {
        restrictToOrgA()
        assertFailsWith<RowScopeViolationException> {
            dao.batchInsert(listOf(
                TestScopedOrder { id = "new-4"; title = "ok"; orgId = "org-a" },
                TestScopedOrder { id = "new-5"; title = "smuggled"; orgId = "org-b" },
            ))
        }
        assertNull(unfiltered("new-5"))
    }

    @Test
    @Transactional
    open fun update_cannotMoveARowOutOfTheSubjectsScope() {
        // The mirror of the insert hole: the scoped WHERE proves so-1 is visible, and says nothing
        // about what it may become. From org-a's side this would look exactly like a delete.
        restrictToOrgA()
        val error = assertFailsWith<RowScopeViolationException> {
            dao.updateProperties("so-1", mapOf(TestScopedOrder::orgId.name to "org-b"))
        }
        assertTrue(error.message!!.contains("org-b"), error.message!!)
        assertEquals("org-a", unfiltered("so-1")!!.orgId, "the row must not have moved")
    }

    @Test
    @Transactional
    open fun batchUpdateProperties_cannotMoveRowsOutOfTheSubjectsScope() {
        restrictToOrgA()
        val criteria = Criteria.of(TestScopedOrder::regionCode.name, OperatorEnum.EQ, "emea")
        assertFailsWith<RowScopeViolationException> {
            dao.batchUpdateProperties(criteria, mapOf(TestScopedOrder::orgId.name to "org-b"))
        }
        assertEquals("org-a", unfiltered("so-1")!!.orgId)
    }

    @Test
    @Transactional
    open fun writesAreUnrestrictedWhenTheFeatureIsOffOrTheSubjectIsSystem() {
        // Adding the dependency must not change what a write may do, and a data import must stay
        // able to create rows anywhere.
        RowScopeEnforcer.install(null)
        dao.insert(TestScopedOrder { id = "new-6"; title = "off"; orgId = "org-z" })
        assertEquals("org-z", unfiltered("new-6")!!.orgId)

        enforce(scope = null)
        DataScopeContext.runAsSystem {
            dao.insert(TestScopedOrder { id = "new-7"; title = "system"; orgId = "org-z" })
        }
        assertEquals("org-z", unfiltered("new-7")!!.orgId)
    }

    //endregion

    //region writes

    @Test
    @Transactional
    open fun batchDeleteByIds_onlyRemovesVisibleRows() {
        restrictToOrgA()
        assertEquals(2, dao.batchDelete(listOf("so-1", "so-2", "so-3")))
        assertEquals("org-b emea, by bob", unfiltered("so-3")!!.title)
    }

    @Test
    @Transactional
    open fun batchDeleteByPayload_onlyRemovesVisibleRows() {
        restrictToOrgA()
        // Four rows are emea; only so-1 is emea *and* in scope. (An empty payload is refused
        // outright — see the unconditional-delete guard in BaseCrudDao.)
        assertEquals(1, dao.batchDeleteWhen(ScopedOrderSearchPayload().apply { regionCode = "emea" }))
        assertEquals("org-b emea, by bob", unfiltered("so-3")!!.title)
    }

    //endregion

    //region the feature switched off, and system authority, must reach every path alike

    @Test
    @Transactional
    open fun withNoEnforcerInstalled_everyPathSeesEverything() {
        // Adding the dependency, or leaving the feature off, must not change a single result.
        RowScopeEnforcer.install(null)
        assertEquals(5, dao.allSearch().size)
        assertEquals(5, dao.search(ScopedOrderSearchPayload()).size)
        assertEquals(5, dao.count(ListSearchPayload()))
        assertEquals(5, dao.allSearchProperty(TestScopedOrder::title).size)
    }

    @Test
    @Transactional
    open fun systemAuthorityBypassesFilteringOnEveryPath() {
        // A scheduled job must reach the same rows through the payload search as through allSearch.
        // Before, the two disagreed — not because of authority, but because only one was filtered.
        enforce(scope = null)
        DataScopeContext.runAsSystem {
            assertEquals(5, dao.allSearch().size)
            assertEquals(5, dao.search(ScopedOrderSearchPayload()).size)
            assertEquals(5, dao.count(ListSearchPayload()))
        }
    }

    //endregion
}
