package io.kudos.ms.auth.common.datascope.vo.response

import java.io.Serializable

/**
 * Resolved data-scope for a user: the effective, most-permissive row-visibility policy computed
 * across all of the user's roles.
 *
 * Consuming business services apply it as a row filter:
 *   - [all] == true → no restriction; return every row.
 *   - otherwise     → return rows whose value on each restricted dimension is in [dimensions], OR
 *                     (if [self]) rows the user created. When nothing concrete resolves the resolver
 *                     falls back to self-only rather than to "see nothing" — a misconfiguration
 *                     should narrow access, not manufacture an empty screen nobody can explain.
 *
 * **Why a map rather than a set of org ids.** Organization is the common way to slice rows but not
 * the only one — region, brand, project and business line all occur, and the storage
 * (`auth_role_scope`) and registration (`IScopeDimensionProvider`) sides are already dimension-aware.
 * If the *resolution* side stayed org-shaped, a second dimension could be configured and would then
 * silently fail to reach any consumer: the one failure mode worse than not supporting a feature.
 *
 * **Multiple dimensions are ANDed at the consumer.** Each dimension narrows independently — a role
 * scoped to `org in (A)` and `region in (EMEA)` means rows in org A *and* region EMEA. Values within
 * one dimension are ORed. The resolver produces the values; the consumer composes the predicate,
 * because only it knows which column carries which dimension.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class DataScopeVo(

    /** True ⇒ no row restriction (the broadest scope across the user's roles was ALL). */
    val all: Boolean,

    /** True ⇒ the user may additionally see rows they created (a SELF scope contributed). */
    val self: Boolean,

    /**
     * Dimension name → the values the user may reach on it, already expanded through that
     * dimension's hierarchy (an org grant carries its subtree). Empty when [all] is true.
     */
    val dimensions: Map<String, Set<String>> = emptyMap(),

) : Serializable {

    /**
     * The org ids the user may access — the built-in dimension, kept as a named accessor because it
     * is what most callers actually mean and spelling it `dimensions["org"]` at every call site
     * would invite typos that fail silently (a missing key reads as "no restriction configured").
     */
    val orgIds: Set<String>
        get() = dimensions[DIMENSION_ORG].orEmpty()

    /**
     * The values granted on one dimension.
     *
     * An empty result means *this dimension does not restrict* — it is not the same as "restricted
     * to nothing". A consumer must therefore skip a dimension it finds empty rather than emitting an
     * impossible predicate.
     *
     * @param dimension the dimension name
     * @return the granted values, or empty when the dimension places no restriction
     */
    fun valuesOf(dimension: String): Set<String> = dimensions[dimension].orEmpty()

    /** Whether [dimension] restricts anything at all for this user. */
    fun restricts(dimension: String): Boolean = !all && dimensions[dimension]?.isNotEmpty() == true

    companion object {
        private const val serialVersionUID = 1L

        /** The built-in dimension name, matching `auth_role_scope.dimension`'s default. */
        const val DIMENSION_ORG = "org"

        /** No restriction. */
        @JvmStatic
        fun all(): DataScopeVo = DataScopeVo(all = true, self = false)

        /** Only own-created rows. */
        @JvmStatic
        fun selfOnly(): DataScopeVo = DataScopeVo(all = false, self = true)

        /** Restricted to explicit org ids — the common single-dimension case. */
        @JvmStatic
        fun ofOrgs(orgIds: Set<String>, self: Boolean = false): DataScopeVo =
            DataScopeVo(all = false, self = self, dimensions = mapOf(DIMENSION_ORG to orgIds))
    }
}
