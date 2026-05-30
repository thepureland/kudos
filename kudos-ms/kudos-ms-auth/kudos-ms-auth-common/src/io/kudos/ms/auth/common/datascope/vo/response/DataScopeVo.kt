package io.kudos.ms.auth.common.datascope.vo.response

import java.io.Serializable

/**
 * Resolved data-scope for a user: the effective, most-permissive row-visibility policy computed
 * across all of the user's roles.
 *
 * Consuming business services apply it as a row filter:
 *   - [all] == true        → no restriction; return every row.
 *   - otherwise            → return rows whose org is in [orgIds], OR (if [self]) rows the user
 *                            created. If both [orgIds] is empty and [self] is false the user can
 *                            see no org-scoped rows (a deliberately restrictive misconfiguration
 *                            outcome — the resolver falls back to self-only rather than empty when
 *                            nothing else matches).
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

    /** Explicit org ids the user may access (empty when [all] is true). */
    val orgIds: Set<String>,

) : Serializable {

    companion object {
        private const val serialVersionUID = 1L

        /** No restriction. */
        @JvmStatic
        fun all(): DataScopeVo = DataScopeVo(all = true, self = false, orgIds = emptySet())

        /** Only own-created rows. */
        @JvmStatic
        fun selfOnly(): DataScopeVo = DataScopeVo(all = false, self = true, orgIds = emptySet())
    }
}
