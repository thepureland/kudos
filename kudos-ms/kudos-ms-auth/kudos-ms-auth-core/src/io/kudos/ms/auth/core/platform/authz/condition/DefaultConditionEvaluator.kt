package io.kudos.ms.auth.core.platform.authz.condition

import java.time.LocalTime


/**
 * The built-in condition evaluator: a deliberately small language, not an expression engine.
 *
 * A condition is a semicolon-separated list of `key=value` clauses, **all** of which must hold:
 *
 * ```
 * ip=10.0.0.0/8
 * time=09:00-18:00
 * ip=10.0.0.0/8;time=09:00-18:00
 * tenant=acme;region=eu
 * ```
 *
 * | key | meaning | context attribute |
 * |---|---|---|
 * | `ip` | client address inside a CIDR block (IPv4) or an exact match | `ip` |
 * | `time` | local time-of-day within `HH:mm-HH:mm`; a range that wraps midnight is supported | evaluated against the clock |
 * | anything else | the context attribute equals the value (string comparison) | same name |
 *
 * **False and undecidable are kept apart**, per the [IConditionEvaluator] contract. A clause whose
 * evidence is present and simply does not match returns `false`. A clause that cannot be judged —
 * malformed, an unparseable pattern, or a context attribute nobody supplied — throws
 * [UndecidableConditionException] instead, so the decision point can keep a DENY in force while
 * dropping an ALLOW. An earlier version returned `false` for both, which read as fail-closed and
 * was: every conditional DENY silently stopped applying the moment its evidence went missing.
 * Applications needing richer rules declare their own [IConditionEvaluator] bean, which takes
 * precedence over this one.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
open class DefaultConditionEvaluator : IConditionEvaluator {

    override fun evaluate(condition: String, context: Map<String, Any?>): Boolean =
        condition.split(CLAUSE_SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .all { clause -> evaluateClause(clause, context) }

    private fun evaluateClause(clause: String, context: Map<String, Any?>): Boolean {
        val separatorAt = clause.indexOf('=')
        if (separatorAt <= 0) {
            throw UndecidableConditionException("malformed condition clause '${clause}'")
        }
        val key = clause.substring(0, separatorAt).trim()
        val value = clause.substring(separatorAt + 1).trim()
        return when (key) {
            KEY_IP -> matchesIp(value, context[KEY_IP]?.toString())
            KEY_TIME -> matchesTimeRange(value, LocalTime.now())
            else -> {
                val actual = context[key]?.toString()
                    ?: throw UndecidableConditionException(
                        "condition asks for context attribute '${key}', which was not supplied",
                    )
                actual == value
            }
        }
    }

    /** Exact address match, or containment in an IPv4 CIDR block. */
    private fun matchesIp(pattern: String, actual: String?): Boolean {
        if (actual.isNullOrBlank()) {
            throw UndecidableConditionException("condition asks for the caller's ip, which was not supplied")
        }
        if (!pattern.contains('/')) {
            // An exact-match pattern that is not an address can never match anything — that is a
            // rule the operator wrote and this evaluator cannot enforce, not a judged false.
            if (toIpv4Long(pattern) == null) {
                throw UndecidableConditionException("unparseable address pattern '${pattern}'")
            }
            return pattern == actual
        }
        val (network, bitsText) = pattern.split('/', limit = 2)
        val bits = bitsText.toIntOrNull()
            ?.takeIf { it in 0..32 }
            ?: throw UndecidableConditionException("unparseable CIDR block '${pattern}'")
        val networkValue = toIpv4Long(network)
            ?: throw UndecidableConditionException("unparseable network address in '${pattern}'")
        val actualValue = toIpv4Long(actual)
            ?: throw UndecidableConditionException("unparseable caller address '${actual}'")
        // Shifting by 32 is a no-op on Int in the JVM, so a /0 block is special-cased.
        if (bits == 0) return true
        val mask = (-1L shl (32 - bits)) and 0xFFFFFFFFL
        return (networkValue and mask) == (actualValue and mask)
    }

    private fun toIpv4Long(address: String): Long? {
        val parts = address.trim().split('.')
        if (parts.size != 4) return null
        var result = 0L
        for (part in parts) {
            val octet = part.toIntOrNull() ?: return null
            if (octet !in 0..255) return null
            result = (result shl 8) or octet.toLong()
        }
        return result
    }

    /** `HH:mm-HH:mm`; ranges that wrap past midnight (`22:00-02:00`) are inclusive of both ends. */
    private fun matchesTimeRange(pattern: String, now: LocalTime): Boolean {
        val bounds = pattern.split('-')
        if (bounds.size != 2) {
            throw UndecidableConditionException("unparseable time range '${pattern}'")
        }
        val from = runCatching { LocalTime.parse(bounds[0].trim()) }.getOrNull()
            ?: throw UndecidableConditionException("unparseable time range '${pattern}'")
        val to = runCatching { LocalTime.parse(bounds[1].trim()) }.getOrNull()
            ?: throw UndecidableConditionException("unparseable time range '${pattern}'")
        return if (!from.isAfter(to)) {
            !now.isBefore(from) && !now.isAfter(to)
        } else {
            !now.isBefore(from) || !now.isAfter(to)
        }
    }

    companion object {
        private const val CLAUSE_SEPARATOR = ";"
        private const val KEY_IP = "ip"
        private const val KEY_TIME = "time"
    }
}
