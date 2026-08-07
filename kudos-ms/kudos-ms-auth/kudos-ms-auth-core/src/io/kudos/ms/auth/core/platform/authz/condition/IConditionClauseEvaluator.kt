package io.kudos.ms.auth.core.platform.authz.condition

/**
 * Extends the built-in ABAC language one clause at a time.
 *
 * Implementations can add operators such as numeric comparison, set membership or a domain lookup
 * without replacing [DefaultConditionEvaluator] and losing its built-in IP, time and equality
 * semantics. Beans are consulted in Spring order; the first one supporting a clause owns it.
 *
 * @author K
 * @author AI: Codex
 */
interface IConditionClauseEvaluator {

    /** Whether this evaluator understands the complete raw clause. Must be side-effect free. */
    fun supports(clause: String): Boolean

    /**
     * Evaluates a supported clause.
     *
     * Return false only when the evidence was present and did not match. Throw
     * [UndecidableConditionException] when required evidence is missing or cannot be interpreted.
     */
    fun evaluate(clause: String, context: Map<String, Any?>): Boolean
}
