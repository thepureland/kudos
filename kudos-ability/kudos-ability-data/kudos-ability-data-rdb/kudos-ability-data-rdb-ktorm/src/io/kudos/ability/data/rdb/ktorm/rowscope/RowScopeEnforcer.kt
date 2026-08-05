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
