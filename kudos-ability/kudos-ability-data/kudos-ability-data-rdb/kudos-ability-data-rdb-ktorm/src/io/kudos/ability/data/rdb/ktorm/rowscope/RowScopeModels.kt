package io.kudos.ability.data.rdb.ktorm.rowscope

import kotlin.reflect.KClass


/**
 * One entity's declared participation in row filtering, however it was declared — by annotation or
 * by an [IRowScopePolicyProvider]. The filter only ever sees this shape, so the two declaration
 * routes cannot diverge in meaning.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class RowScopePolicy(

    /** The entity this applies to. */
    val entityClass: KClass<*>,

    /** dimension → the column carrying that dimension's value on this entity. */
    val dimensionColumns: Map<String, String> = emptyMap(),

    /** The column identifying the row's creator, for the SELF policy; null = SELF cannot apply. */
    val selfColumn: String? = null,

) {
    /** Whether this entity participates at all. */
    val participates: Boolean
        get() = dimensionColumns.isNotEmpty() || selfColumn != null
}


/**
 * Supplies row-scope declarations for entities whose source you do not own.
 *
 * The annotation covers the normal case; this covers a third-party module's tables, where the only
 * alternative would be forking the entity to add an annotation to it.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IRowScopePolicyProvider {

    /**
     * @return the policies this provider contributes; entities absent from the result are untouched
     */
    fun policies(): List<RowScopePolicy>
}


/**
 * What the row filter needs to know about the current subject, expressed so that
 * `kudos-ability-data-rdb-ktorm` never depends on the authorization module.
 *
 * The dependency runs the other way, exactly as for the URL-enforcement ports: `kudos-ms-auth`
 * implements this, and an application with a different authorization backend implements it instead
 * — the filter does not change either way.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IRowScopeResolver {

    /**
     * The current subject's row scope, or null when there is no subject at all.
     *
     * Returning null is **not** "no restriction" — it means "nobody is asking", which the filter
     * treats as an error unless the caller declared system authority. Unrestricted access is
     * expressed by [RowScope.unrestricted].
     */
    fun currentScope(): RowScope?
}


/**
 * The resolved row scope of whoever is asking.
 *
 * Mirrors the authorization module's `DataScopeVo` without sharing its type, so the data layer and
 * the auth layer stay decoupled.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class RowScope(

    /** True ⇒ no row restriction at all. */
    val unrestricted: Boolean,

    /** True ⇒ rows created by this subject are visible regardless of the dimension predicates. */
    val self: Boolean,

    /** The subject's id, needed to compare against an entity's creator column. */
    val principalId: String?,

    /**
     * dimension → the values the subject may reach. A dimension **absent** from this map does not
     * restrict; a dimension present with an empty set is normalised away by the resolver, because
     * "restricted to nothing" and "not restricted" must never be confused at the filter.
     */
    val dimensions: Map<String, Set<String>> = emptyMap(),

) {
    companion object {

        /** No restriction — what a platform administrator resolves to. */
        @JvmStatic
        fun unrestricted(principalId: String?): RowScope =
            RowScope(unrestricted = true, self = false, principalId = principalId)
    }
}
