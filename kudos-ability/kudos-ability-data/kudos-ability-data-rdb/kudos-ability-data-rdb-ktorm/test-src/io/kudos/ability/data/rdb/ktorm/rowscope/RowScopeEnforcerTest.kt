package io.kudos.ability.data.rdb.ktorm.rowscope

import org.ktorm.schema.Table
import org.ktorm.schema.varchar
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue


/**
 * junit test for [RowScopeEnforcer] — the switch logic that decides whether a query is touched.
 *
 * Every branch here is a safety decision rather than a convenience: what happens when the feature is
 * off, when nobody is logged in, when the caller declared system authority, and when the deployment
 * is still in shadow mode. Getting any of them backwards either leaks rows or empties a screen.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class RowScopeEnforcerTest {

    private object Orders : Table<Nothing>("biz_order") {
        val id = varchar("id").primaryKey()
        val orgId = varchar("org_id")
    }

    @DataScoped(dimension = "org", column = "org_id")
    private interface ScopedOrder

    private interface UnscopedDict

    private class StubResolver(var scope: RowScope?) : IRowScopeResolver {
        override fun currentScope(): RowScope? = scope
    }

    private fun enforcer(
        resolver: IRowScopeResolver?,
        enabled: Boolean = true,
        shadow: Boolean = false,
    ): RowScopeEnforcer = RowScopeEnforcer(
        RowScopeRegistry(),
        resolver,
        RowScopeProperties().apply { this.enabled = enabled; this.shadowMode = shadow },
    )

    private val restricted = RowScope(false, self = false, principalId = "u1", dimensions = mapOf("org" to setOf("o1")))

    @Test
    fun disabledMeansNoQueryIsEverTouched() {
        val e = enforcer(StubResolver(restricted), enabled = false)
        assertNull(e.predicateFor(Orders, ScopedOrder::class), "adding the dependency must change nothing")
    }

    @Test
    fun enabledAndEnforcingAppendsThePredicate() {
        val e = enforcer(StubResolver(restricted))
        assertNotNull(e.predicateFor(Orders, ScopedOrder::class))
    }

    @Test
    fun shadowModeLogsButDoesNotFilter() {
        val e = enforcer(StubResolver(restricted), shadow = true)
        assertNull(e.predicateFor(Orders, ScopedOrder::class), "shadow mode must not change any result set")
    }

    @Test
    fun anUndeclaredEntityIsNeverFiltered() {
        val e = enforcer(StubResolver(restricted))
        assertNull(e.predicateFor(Orders, UnscopedDict::class))
    }

    @Test
    fun noSubjectOnAScopedEntityFailsLoudly() {
        // The decision this test exists for: "no subject" must not read as "no restriction", or any
        // path that merely loses the context silently reads everything.
        val e = enforcer(StubResolver(null))
        val error = assertFailsWith<RowScopeUnresolvedException> { e.predicateFor(Orders, ScopedOrder::class) }
        assertTrue(error.message!!.contains("runAsSystem"), "the error must say how to fix it: ${error.message}")
    }

    @Test
    fun noSubjectOnAnUndeclaredEntityIsFine() {
        // The loud failure must be scoped to entities that actually opted in, or every query in the
        // system would need a subject.
        val e = enforcer(StubResolver(null))
        assertNull(e.predicateFor(Orders, UnscopedDict::class))
    }

    @Test
    fun declaredSystemAuthorityBypassesFiltering() {
        val e = enforcer(StubResolver(null))
        DataScopeContext.runAsSystem {
            assertNull(e.predicateFor(Orders, ScopedOrder::class), "a scheduled job may run unfiltered")
        }
        // …and only inside the block.
        assertFailsWith<RowScopeUnresolvedException> { e.predicateFor(Orders, ScopedOrder::class) }
    }

    @Test
    fun shadowModeAlsoSoftensTheMissingSubjectFailure() {
        // Otherwise turning the feature on in shadow mode — the safe first step — would itself be
        // the outage it exists to prevent.
        val e = enforcer(StubResolver(null), shadow = true)
        assertNull(e.predicateFor(Orders, ScopedOrder::class))
    }

    @Test
    fun anUnrestrictedSubjectIsLeftAlone() {
        // Platform administrators need no special case anywhere: they simply resolve to unrestricted.
        val e = enforcer(StubResolver(RowScope.unrestricted("admin")))
        assertNull(e.predicateFor(Orders, ScopedOrder::class))
    }

    @Test
    fun noResolverAtAllBehavesLikeNoSubject() {
        val e = enforcer(null)
        assertFailsWith<RowScopeUnresolvedException> { e.predicateFor(Orders, ScopedOrder::class) }
    }
}
