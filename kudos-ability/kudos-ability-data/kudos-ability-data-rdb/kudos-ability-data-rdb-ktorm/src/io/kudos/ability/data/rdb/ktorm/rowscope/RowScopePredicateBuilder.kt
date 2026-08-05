package io.kudos.ability.data.rdb.ktorm.rowscope

import io.kudos.base.logger.LogFactory
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.inList
import org.ktorm.dsl.or
import org.ktorm.schema.Column
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.Table
import kotlin.reflect.KClass


/**
 * Turns "who is asking" plus "how this entity is sliced" into a WHERE fragment.
 *
 * The composition rules are the contract, not an implementation detail:
 *
 * | input | fragment |
 * |---|---|
 * | scope is unrestricted | none — the query is untouched |
 * | one restricted dimension | `col IN (values)` — values within a dimension are **OR**ed |
 * | several dimensions | **AND**ed together — each one narrows independently |
 * | `self` | **OR**ed with the whole of the above: `(dim1 AND dim2) OR creator = me` |
 * | a dimension the entity does not declare | skipped |
 *
 * That last rule is the one worth defending: scope means "which rows of *this kind of thing*", and
 * an entity with no region column has no region-ness to restrict. Failing closed instead would make
 * every unrelated entity vanish the moment somebody configures a second dimension. The risk it
 * leaves — an entity that *should* declare a dimension and forgot — is surfaced by
 * [RowScopeRegistry.auditPartialDeclarations] rather than by breaking unrelated queries.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object RowScopePredicateBuilder {

    private val log = LogFactory.getLog(this::class)

    /**
     * Builds the fragment restricting [table]'s rows for [scope].
     *
     * @param table the Ktorm table being queried
     * @param policy the entity's declaration
     * @param scope the subject's resolved scope
     * @return the predicate, or null when nothing should be appended
     */
    fun build(table: Table<*>, policy: RowScopePolicy, scope: RowScope): ColumnDeclaring<Boolean>? {
        if (scope.unrestricted || !policy.participates) return null

        val dimensionPredicates: List<ColumnDeclaring<Boolean>> = policy.dimensionColumns.mapNotNull { (dimension, columnName) ->
            val values = scope.dimensions[dimension]
            // Absent ⇒ this dimension does not restrict. Empty would mean "restricted to nothing",
            // which the resolver normalises away precisely so the two cannot be confused here.
            if (values.isNullOrEmpty()) return@mapNotNull null
            val column = table.columnByName(columnName)
            if (column == null) {
                // A declaration pointing at a column that does not exist is a configuration error,
                // and the safe reading of an unenforceable rule is not "allow everything".
                throw RowScopeUnresolvedException(
                    "Entity ${policy.entityClass.simpleName} declares dimension '${dimension}' on column " +
                        "'${columnName}', which ${table.tableName} does not have.",
                )
            }
            column.inList(list = values.toList())
        }

        val restriction = dimensionPredicates.reduceOrNull { acc, next -> acc and next }
        val selfPredicate = selfPredicateOf(table, policy, scope)

        return when {
            restriction != null && selfPredicate != null -> restriction or selfPredicate
            restriction != null -> restriction
            selfPredicate != null -> selfPredicate
            // The subject is restricted, but nothing about this entity is restrictable by their
            // scope. Leaving it unfiltered is the honest reading of "this entity is not sliced that
            // way" — and is why partial declarations are audited at startup.
            else -> null
        }
    }

    private fun selfPredicateOf(table: Table<*>, policy: RowScopePolicy, scope: RowScope): ColumnDeclaring<Boolean>? {
        if (!scope.self) return null
        val columnName = policy.selfColumn ?: return null
        val principalId = scope.principalId ?: return null
        val column = table.columnByName(columnName)
        if (column == null) {
            log.warn(
                "Entity ${policy.entityClass.simpleName} declares its creator column as '${columnName}', " +
                    "which ${table.tableName} does not have; the SELF policy cannot apply.",
            )
            return null
        }
        return column.eq(principalId)
    }

    @Suppress("UNCHECKED_CAST")
    private fun Table<*>.columnByName(name: String): Column<Any>? =
        columns.firstOrNull { it.name.equals(name, ignoreCase = true) } as Column<Any>?

    /** Renders a predicate for the shadow-mode log, where a SQL-ish string beats an object dump. */
    fun describe(policy: RowScopePolicy, scope: RowScope): String {
        if (scope.unrestricted) return "(unrestricted)"
        val parts = policy.dimensionColumns.mapNotNull { (dimension, column) ->
            scope.dimensions[dimension]?.takeIf { it.isNotEmpty() }?.let { "${column} IN (${it.size} value(s))" }
        }
        val self = if (scope.self && policy.selfColumn != null) "${policy.selfColumn} = '${scope.principalId}'" else null
        val restriction = parts.joinToString(" AND ").ifEmpty { null }
        return listOfNotNull(restriction?.let { "(${it})" }, self).joinToString(" OR ").ifEmpty { "(no restriction)" }
    }
}
