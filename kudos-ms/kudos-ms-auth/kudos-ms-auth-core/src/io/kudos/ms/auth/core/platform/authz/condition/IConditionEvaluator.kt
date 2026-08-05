package io.kudos.ms.auth.core.platform.authz.condition


/**
 * Decides whether a permission binding's condition holds for the request at hand.
 *
 * This is the seam where attribute-based rules enter without a policy engine entering with them.
 * The high-value ABAC cases in a back-office system are environmental — "only from the office
 * network", "only during business hours", "only for this brand" — and they are expressible as a
 * small restricted expression evaluated against a context map. A general expression language would
 * buy the remaining cases at the price of an unbounded, hard-to-explain and hard-to-cost evaluation
 * step on every request; see the non-goals in the module README.
 *
 * **The contract is three-valued, and the third value is the exception channel.** The return value
 * answers one question only: *was the condition judged, and did it hold?* When the condition cannot
 * be judged at all — the expression is malformed, or the context lacks the evidence it asks for —
 * the implementation must throw [UndecidableConditionException] rather than return `false`.
 * The two outcomes diverge at the decision point: a condition judged false makes the binding absent
 * whatever its effect, while an undecidable one removes an ALLOW but **keeps a DENY in force**.
 * An implementation that returns `false` for "could not judge" silently lifts every conditional
 * restriction whose evidence goes missing — see [UndecidableConditionException] for why that is the
 * failure mode this contract exists to prevent.
 *
 * Implementations must be side-effect free and fast — this runs on every authorization check.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IConditionEvaluator {

    /**
     * @param condition the raw expression stored on the binding; never blank (callers skip blanks)
     * @param context the request attributes
     * @return true when the condition was judged and holds; false when it was judged and does not
     * @throws UndecidableConditionException when the condition cannot be judged at all
     */
    fun evaluate(condition: String, context: Map<String, Any?>): Boolean
}
