package io.kudos.ability.data.rdb.ktorm.rowscope

import io.kudos.base.logger.LogFactory
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.Table
import kotlin.reflect.KClass


/**
 * Decides, for one query, whether a row-scope predicate should be appended and what it is.
 *
 * All the switches live here rather than in the DAO, so the DAO's job stays "ask, then AND it in"
 * and the policy for enabled / shadow / missing-subject cannot be re-litigated per call site.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
open class RowScopeEnforcer(
    private val registry: RowScopeRegistry,
    private val resolver: IRowScopeResolver?,
    private val properties: RowScopeProperties,
    /** Collects shadow findings so the migration work-list is readable, not just greppable. */
    private val recorder: RowScopeShadowRecorder? = null,
) {

    private val log = LogFactory.getLog(this::class)

    /**
     * The predicate restricting [table] for the current subject, or null when the query should be
     * left as written.
     *
     * @param table the Ktorm table being queried
     * @param entityClass the entity bound to it, which carries the declaration
     * @return the predicate to AND in, or null
     * @throws RowScopeUnresolvedException when a scoped entity is queried with no subject and no
     *   declared system authority — see [DataScopeContext] for why this is loud rather than silent
     */
    open fun predicateFor(table: Table<*>, entityClass: KClass<*>): ColumnDeclaring<Boolean>? {
        if (!properties.enabled) return null

        val policy = registry.policyOf(entityClass)
        // An entity nobody declared is not filtered, so nothing below it needs to run — including
        // the missing-subject check, which must not fail queries on unrelated tables.
        if (!policy.participates) return null

        if (DataScopeContext.isSystem()) return null

        val scope = resolver?.currentScope()
        if (scope == null) {
            val message = "Row-scoped entity ${entityClass.simpleName} was queried with no subject and no " +
                "declared system authority. Wrap the call in DataScopeContext.runAsSystem { } (or annotate " +
                "it @SystemScoped) if it legitimately runs without a logged-in user."
            if (properties.shadowMode) {
                log.warn("[row-scope][shadow] would fail: ${message}")
                recorder?.record(
                    RowScopeShadowRecorder.Kind.WOULD_FAIL,
                    entityClass.simpleName ?: entityClass.toString(),
                    table.tableName,
                    "no subject and no declared system authority",
                )
                return null
            }
            throw RowScopeUnresolvedException(message)
        }

        val predicate = RowScopePredicateBuilder.build(table, policy, scope)
        if (properties.shadowMode) {
            if (predicate != null) {
                val described = RowScopePredicateBuilder.describe(policy, scope)
                // WARN, not DEBUG: these lines are the migration work-list, and a level nobody has
                // switched on produces an empty one.
                log.warn(
                    "[row-scope][shadow] would append to ${table.tableName} for principal " +
                        "${scope.principalId}: ${described}",
                )
                recorder?.record(
                    RowScopeShadowRecorder.Kind.WOULD_FILTER,
                    entityClass.simpleName ?: entityClass.toString(),
                    table.tableName,
                    described,
                )
            }
            return null
        }
        return predicate
    }

    /**
     * The scope-column values an INSERT should add, or an empty map when the write may proceed as
     * written.
     *
     * Writes go through the same switches as reads — feature off, entity undeclared, system
     * authority, unrestricted subject, missing subject, shadow mode — deliberately here rather than
     * in a second component, so the two sides cannot answer the same question differently.
     *
     * @param table the Ktorm table being written to
     * @param entityClass the entity bound to it, which carries the declaration
     * @param values column name → the value the caller is writing
     * @return column name → value to add for scope columns the caller left unset
     * @throws RowScopeViolationException when the row would land outside the subject's scope
     * @throws RowScopeUnresolvedException on the same terms as [predicateFor]
     */
    open fun defaultsForInsert(
        table: Table<*>,
        entityClass: KClass<*>,
        values: Map<String, Any?>,
    ): Map<String, Any?> = onWrite(table, entityClass, "insert") { policy, scope ->
        RowScopeWriteValidator.defaultsForInsert(policy, scope, values)
    } ?: emptyMap()

    /**
     * Refuses an UPDATE whose assignments would move the row out of the subject's scope. See
     * [defaultsForInsert] for why the switches live here.
     *
     * @param table the Ktorm table being written to
     * @param entityClass the entity bound to it
     * @param values column name → the value being assigned
     * @throws RowScopeViolationException when an assignment would move the row out of scope
     */
    open fun checkUpdate(table: Table<*>, entityClass: KClass<*>, values: Map<String, Any?>) {
        onWrite(table, entityClass, "update") { policy, scope ->
            RowScopeWriteValidator.checkUpdate(policy, scope, values)
        }
    }

    /**
     * Runs [check] under the same conditions [predicateFor] appends a filter under, or returns null
     * when this write is none of row scope's business.
     *
     * In shadow mode the check still runs, but a violation is recorded and swallowed rather than
     * thrown — the point of shadow mode is to find out what enforcement *would* reject before it
     * starts rejecting, and a write path is exactly where an unexpected rejection hurts most.
     */
    private fun <R> onWrite(
        table: Table<*>,
        entityClass: KClass<*>,
        operation: String,
        check: (RowScopePolicy, RowScope) -> R,
    ): R? {
        if (!properties.enabled) return null
        val policy = registry.policyOf(entityClass)
        if (!policy.participates) return null
        if (DataScopeContext.isSystem()) return null

        val scope = resolver?.currentScope()
        if (scope == null) {
            val message = "Row-scoped entity ${entityClass.simpleName} was written with no subject and " +
                "no declared system authority. Wrap the call in DataScopeContext.runAsSystem { } if it " +
                "legitimately runs without a logged-in user."
            if (properties.shadowMode) {
                log.warn("[row-scope][shadow] would fail: ${message}")
                recorder?.record(
                    RowScopeShadowRecorder.Kind.WOULD_FAIL,
                    entityClass.simpleName ?: entityClass.toString(),
                    table.tableName,
                    "no subject and no declared system authority on $operation",
                )
                return null
            }
            throw RowScopeUnresolvedException(message)
        }
        if (scope.unrestricted) return null

        if (!properties.shadowMode) return check(policy, scope)

        return try {
            check(policy, scope)
            null
        } catch (e: RowScopeViolationException) {
            log.warn("[row-scope][shadow] would reject $operation: ${e.message}")
            recorder?.record(
                RowScopeShadowRecorder.Kind.WOULD_REJECT,
                entityClass.simpleName ?: entityClass.toString(),
                table.tableName,
                e.message ?: operation,
            )
            null
        }
    }

    companion object {
        /**
         * The active enforcer, or null when the module is not wired (plain unit tests, tooling).
         *
         * A static holder because DAOs are constructed in many ways across the framework and
         * threading a bean through every one of them would be a larger change than the feature.
         * Set once by the auto-configuration.
         */
        @Volatile
        @JvmStatic
        var current: RowScopeEnforcer? = null
            internal set

        /** Installs the enforcer; called by the auto-configuration. */
        @JvmStatic
        fun install(enforcer: RowScopeEnforcer?) {
            current = enforcer
        }
    }
}
