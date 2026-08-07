package io.kudos.ms.auth.core.platform.authz.condition


/**
 * A condition that could not be judged — as distinct from one that was judged and found false.
 *
 * The distinction is load-bearing, and collapsing it is a security defect. "The caller is outside
 * the office network" and "nobody told me the caller's network" are different facts, and only the
 * first may make a conditional DENY stand aside. When both were reported as `false`, a DENY binding
 * vanished whenever its evidence went missing — a malformed expression, an unconfigured context
 * header, a programmatic `decide()` call that supplied no context at all — which inverted the
 * evaluator's own rule that an unenforceable restriction must never widen access.
 *
 * The decision point handles this asymmetrically by effect: an ALLOW whose condition is undecidable
 * is absent (the caller could not produce the evidence, so no access is conferred), a DENY whose
 * condition is undecidable **applies** (the restriction could not be checked, so it is not lifted).
 * Fail-closed on both sides, where "closed" means the opposite thing for each.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
class UndecidableConditionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
