package io.kudos.ms.auth.core.platform.authz.condition

import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * junit test for [DefaultConditionEvaluator]. Pure logic — no Spring context needed.
 *
 * The contract under test is three-valued: `true` and `false` mean the condition was judged, and
 * [UndecidableConditionException] means it could not be. The distinction is what lets the decision
 * point keep a DENY in force when its evidence goes missing while dropping an ALLOW — an evaluator
 * that collapses "could not judge" into `false` silently lifts every conditional restriction,
 * which is the one behaviour a permission system must not have.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class DefaultConditionEvaluatorTest {

    private val evaluator = DefaultConditionEvaluator()

    @Test
    fun ipCidr() {
        assertTrue(evaluator.evaluate("ip=10.0.0.0/8", mapOf("ip" to "10.1.2.3")))
        assertTrue(evaluator.evaluate("ip=10.0.0.0/8", mapOf("ip" to "10.255.255.255")))
        assertFalse(evaluator.evaluate("ip=10.0.0.0/8", mapOf("ip" to "192.168.1.1")))
        assertTrue(evaluator.evaluate("ip=192.168.1.0/24", mapOf("ip" to "192.168.1.42")))
        assertFalse(evaluator.evaluate("ip=192.168.1.0/24", mapOf("ip" to "192.168.2.42")))
        // /0 covers everything; the mask arithmetic has an edge there worth pinning.
        assertTrue(evaluator.evaluate("ip=0.0.0.0/0", mapOf("ip" to "8.8.8.8")))
    }

    @Test
    fun ipExactMatch() {
        assertTrue(evaluator.evaluate("ip=10.1.2.3", mapOf("ip" to "10.1.2.3")))
        assertFalse(evaluator.evaluate("ip=10.1.2.3", mapOf("ip" to "10.1.2.4")))
    }

    @Test
    fun timeRange() {
        // Ranges are built relative to the current clock so the assertions hold whenever the suite
        // runs — including near midnight, where the window wraps and exercises the other branch.
        val now = LocalTime.now()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        fun at(minutesFromNow: Long): String = now.plusMinutes(minutesFromNow).format(formatter)

        assertTrue(evaluator.evaluate("time=${at(-90)}-${at(90)}", emptyMap()), "now is inside the window")
        assertFalse(evaluator.evaluate("time=${at(60)}-${at(120)}", emptyMap()), "now is before the window")
        assertFalse(evaluator.evaluate("time=${at(-120)}-${at(-60)}", emptyMap()), "now is after the window")
    }

    @Test
    fun customAttributeEquality() {
        assertTrue(evaluator.evaluate("tenant=acme", mapOf("tenant" to "acme")))
        assertFalse(evaluator.evaluate("tenant=acme", mapOf("tenant" to "other")))
    }

    @Test
    fun everyClauseMustHold() {
        val context = mapOf("ip" to "10.1.2.3", "tenant" to "acme")
        assertTrue(evaluator.evaluate("ip=10.0.0.0/8;tenant=acme", context))
        assertFalse(evaluator.evaluate("ip=10.0.0.0/8;tenant=other", context))
    }

    /**
     * Missing evidence is *undecidable*, not false. Returning false here was the defect the
     * three-valued contract exists to fix: for a DENY binding, "could not check the restriction"
     * became "the restriction does not apply" — and the context is entirely the caller's to supply.
     */
    @Test
    fun missingContextAttributeIsUndecidable() {
        assertFailsWith<UndecidableConditionException> { evaluator.evaluate("ip=10.0.0.0/8", emptyMap()) }
        assertFailsWith<UndecidableConditionException> { evaluator.evaluate("tenant=acme", emptyMap()) }
    }

    /** A rule that cannot be parsed cannot be judged — same channel as missing evidence. */
    @Test
    fun malformedOrUnparseableClausesAreUndecidable() {
        assertFailsWith<UndecidableConditionException> { evaluator.evaluate("garbage", emptyMap()) }
        assertFailsWith<UndecidableConditionException> { evaluator.evaluate("=novalue", emptyMap()) }
        assertFailsWith<UndecidableConditionException> {
            evaluator.evaluate("ip=not-an-address", mapOf("ip" to "10.1.2.3"))
        }
        assertFailsWith<UndecidableConditionException> {
            evaluator.evaluate("ip=10.0.0.0/33", mapOf("ip" to "10.1.2.3"))
        }
        assertFailsWith<UndecidableConditionException> { evaluator.evaluate("time=not-a-range", emptyMap()) }
    }

    /** …while evidence that is present and simply does not match stays a clean, judged false. */
    @Test
    fun presentButMismatchedEvidenceIsAJudgedFalse() {
        assertFalse(evaluator.evaluate("ip=10.0.0.0/8", mapOf("ip" to "192.168.1.1")))
        assertFalse(evaluator.evaluate("tenant=acme", mapOf("tenant" to "other")))
    }
}
