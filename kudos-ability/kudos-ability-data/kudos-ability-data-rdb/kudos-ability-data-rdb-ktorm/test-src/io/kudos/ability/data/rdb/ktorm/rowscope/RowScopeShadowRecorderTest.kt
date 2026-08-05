package io.kudos.ability.data.rdb.ktorm.rowscope

import org.ktorm.schema.Table
import org.ktorm.schema.varchar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for [RowScopeShadowRecorder] and for what the enforcer feeds it.
 *
 * The properties that matter are about the *list being trustworthy*: repeated observations must
 * merge rather than bury everything else, and a truncated list must say so rather than look complete.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class RowScopeShadowRecorderTest {

    private object Orders : Table<Nothing>("biz_order") {
        val id = varchar("id").primaryKey()
        val orgId = varchar("org_id")
    }

    @DataScoped(dimension = "org", column = "org_id")
    private interface ScopedOrder

    private class StubResolver(val scope: RowScope?) : IRowScopeResolver {
        override fun currentScope(): RowScope? = scope
    }

    @Test
    fun repeatedObservationsMergeAndCount() {
        // A hot path must not bury every other finding under thousands of identical rows — the count
        // is also what tells "one endpoint I forgot" from "something running constantly".
        val recorder = RowScopeShadowRecorder()
        repeat(5) { recorder.record(RowScopeShadowRecorder.Kind.WOULD_FILTER, "Order", "biz_order", "org_id IN (2)") }

        val finding = recorder.findings().single()
        assertEquals(5, finding.count)
        assertTrue(!finding.lastSeen.isBefore(finding.firstSeen))
    }

    @Test
    fun differentKindsAndDetailsStaySeparate() {
        val recorder = RowScopeShadowRecorder()
        recorder.record(RowScopeShadowRecorder.Kind.WOULD_FILTER, "Order", "biz_order", "org_id IN (2)")
        recorder.record(RowScopeShadowRecorder.Kind.WOULD_FAIL, "Order", "biz_order", "no subject")
        assertEquals(2, recorder.findings().size)
    }

    @Test
    fun aTruncatedListSaysSo() {
        // The property worth testing: silence here would read as "nothing left to do".
        val recorder = RowScopeShadowRecorder(capacity = 2)
        repeat(5) { i -> recorder.record(RowScopeShadowRecorder.Kind.WOULD_FILTER, "E$i", "t$i", "d$i") }
        assertEquals(2, recorder.findings().size)
        assertEquals(3, recorder.droppedCount())
    }

    @Test
    fun clearingResetsBothTheListAndTheDropCount() {
        val recorder = RowScopeShadowRecorder(capacity = 1)
        repeat(3) { i -> recorder.record(RowScopeShadowRecorder.Kind.WOULD_FILTER, "E$i", "t$i", "d$i") }
        recorder.clear()
        assertTrue(recorder.findings().isEmpty())
        assertEquals(0, recorder.droppedCount())
    }

    @Test
    fun theEnforcerFeedsBothKindsInShadowModeOnly() {
        val recorder = RowScopeShadowRecorder()
        val properties = RowScopeProperties().apply { enabled = true; shadowMode = true }
        val restricted = RowScope(false, self = false, principalId = "u1", dimensions = mapOf("org" to setOf("o1")))

        RowScopeEnforcer(RowScopeRegistry(), StubResolver(restricted), properties, recorder)
            .predicateFor(Orders, ScopedOrder::class)
        RowScopeEnforcer(RowScopeRegistry(), StubResolver(null), properties, recorder)
            .predicateFor(Orders, ScopedOrder::class)

        val kinds = recorder.findings().map { it.kind }.toSet()
        assertEquals(
            setOf(RowScopeShadowRecorder.Kind.WOULD_FILTER, RowScopeShadowRecorder.Kind.WOULD_FAIL),
            kinds,
            "both the 'verify this predicate' and the 'this call needs system authority' findings",
        )

        // Enforcing mode records nothing: the list is a migration aid, and once filtering is real the
        // queries themselves are the truth.
        val enforcing = RowScopeShadowRecorder()
        RowScopeEnforcer(
            RowScopeRegistry(),
            StubResolver(restricted),
            RowScopeProperties().apply { enabled = true; shadowMode = false },
            enforcing,
        ).predicateFor(Orders, ScopedOrder::class)
        assertTrue(enforcing.findings().isEmpty())
    }
}
