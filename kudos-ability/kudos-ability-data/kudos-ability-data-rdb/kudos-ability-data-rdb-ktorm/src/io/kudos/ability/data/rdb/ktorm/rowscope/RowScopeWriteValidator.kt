package io.kudos.ability.data.rdb.ktorm.rowscope


/**
 * Thrown when a write would put a row outside the writer's own scope.
 *
 * Separate from [RowScopeUnresolvedException], which means "nobody is asking": this one means
 * somebody is asking and the answer is no. Applications that map framework exceptions to HTTP
 * usually want this one as 403 rather than 400 — the request is well-formed, it is just not allowed.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class RowScopeViolationException(message: String) : IllegalStateException(message)


/**
 * Decides whether the values a write is about to store keep the row inside the subject's scope.
 *
 * **Why this is a separate mechanism from the row filter.** Filtering answers "which existing rows
 * may I touch" and becomes a `WHERE` clause. An `INSERT` has no existing row to filter, and an
 * `UPDATE`'s `SET` list is not covered by its `WHERE`: both decide what the row's scope columns will
 * *become*, which is a question about values, answered in memory before any SQL is built. A filter
 * cannot express it, which is why row scope covered reads, updates-by-id and deletes and still left
 * these two open:
 *
 * - `insert(Order { orgId = "org-b" })` from a subject restricted to `org-a` creates a live row in
 *   another org — one the creator cannot then read, update or delete, but which is entirely real to
 *   `org-b`'s users.
 * - `updateProperties(id, mapOf("orgId" to "org-b"))` on a row the subject *can* see moves it out of
 *   their own scope: from their side indistinguishable from a delete, from `org-b`'s side a row that
 *   appeared from nowhere.
 *
 * The rule both cases reduce to: **you may not write a value that would place the row outside your
 * own scope.**
 *
 * **The unset case is where this lives or dies.** Requiring every caller to set `orgId` explicitly on
 * every insert would be correct and would get worked around, so a value the caller left unset is
 * filled in from the subject's scope when that is unambiguous — one allowed value, one obvious
 * answer. Several allowed values is a genuine question only the caller can answer, so it is asked
 * rather than guessed.
 *
 * Pure and stateless: no Spring, no database, no context lookups. Everything it needs is passed in,
 * so the rules can be tested directly. The switches (feature off, shadow mode, system authority,
 * unrestricted subject, undeclared entity) live in [RowScopeEnforcer] with the read side's, so the
 * two cannot drift apart.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object RowScopeWriteValidator {

    /**
     * Checks the values an INSERT will store, and supplies defaults for the scope columns it left
     * out.
     *
     * @param policy the entity's declaration
     * @param scope the subject's resolved scope
     * @param values column name → the value the caller is writing; a column absent from the map and
     *   a column mapped to null are treated the same, as "not specified"
     * @return column name → the value to use where the caller specified none; empty when the caller
     *   specified everything
     * @throws RowScopeViolationException when a specified value is outside the subject's scope, or
     *   when one was not specified and cannot be inferred
     */
    fun defaultsForInsert(
        policy: RowScopePolicy,
        scope: RowScope,
        values: Map<String, Any?>,
    ): Map<String, Any?> {
        val defaults = mutableMapOf<String, Any?>()

        policy.dimensionColumns.forEach { (dimension, columnName) ->
            val allowed = scope.dimensions[dimension]
            // Absent or empty means this dimension does not restrict the subject — the same reading
            // the filter takes, so a subject who can read every org can create in every org.
            if (allowed.isNullOrEmpty()) return@forEach

            when (val written = valueOf(values, columnName)) {
                null -> {
                    val only = allowed.singleOrNull() ?: throw RowScopeViolationException(
                        "Cannot insert into ${policy.entityClass.simpleName}: column [$columnName] was " +
                            "not given a value, and the subject is allowed ${allowed.size} values for " +
                            "dimension '$dimension' (${allowed.sorted()}), so which one the row belongs " +
                            "to is not something this can decide. Set [$columnName] explicitly."
                    )
                    defaults[columnName] = only
                }

                else -> requireAllowed(written, allowed, columnName, dimension, policy, "insert into")
            }
        }

        selfColumnDefault(policy, scope, values)?.let { defaults[it.first] = it.second }
        return defaults
    }

    /**
     * Checks the values an UPDATE will store. Unlike an insert, nothing is filled in: a column the
     * update does not mention keeps whatever the row already had, and the row was already proven
     * visible by the statement's scoped `WHERE`.
     *
     * @param policy the entity's declaration
     * @param scope the subject's resolved scope
     * @param values column name → the value being assigned; columns absent from the map are untouched
     * @throws RowScopeViolationException when an assignment would move the row out of the
     *   subject's scope
     */
    fun checkUpdate(
        policy: RowScopePolicy,
        scope: RowScope,
        values: Map<String, Any?>,
    ) {
        policy.dimensionColumns.forEach { (dimension, columnName) ->
            val allowed = scope.dimensions[dimension]
            if (allowed.isNullOrEmpty()) return@forEach
            if (!mentions(values, columnName)) return@forEach

            val written = valueOf(values, columnName) ?: throw RowScopeViolationException(
                "Cannot update ${policy.entityClass.simpleName}: setting [$columnName] to null would " +
                    "move the row out of dimension '$dimension' entirely, leaving it visible to " +
                    "nobody who is restricted by that dimension — including the caller. Run it under " +
                    "DataScopeContext.runAsSystem { } if that is genuinely intended."
            )
            requireAllowed(written, allowed, columnName, dimension, policy, "update")
        }

        val selfColumn = policy.selfColumn ?: return
        val principalId = scope.principalId ?: return
        if (!mentions(values, selfColumn)) return
        val written = valueOf(values, selfColumn)
        if (written?.toString() != principalId) {
            throw RowScopeViolationException(
                "Cannot update ${policy.entityClass.simpleName}: setting [$selfColumn] to " +
                    "[${written ?: "null"}] reassigns the row to another principal, which for a " +
                    "SELF-scoped entity hands it away. Transfers of ownership belong under " +
                    "DataScopeContext.runAsSystem { }."
            )
        }
    }

    /**
     * The creator column's value for an insert: forced to the subject, never taken from the caller.
     *
     * Who created a row is a fact about the request, not an input to it — a caller who could set it
     * freely could attribute their row to anyone, and on a SELF-scoped entity hand themselves access
     * to somebody else's data or hide their own writes in someone else's name.
     *
     * @return the column and value to fill, or null when there is nothing to do
     */
    private fun selfColumnDefault(
        policy: RowScopePolicy,
        scope: RowScope,
        values: Map<String, Any?>,
    ): Pair<String, Any?>? {
        val selfColumn = policy.selfColumn ?: return null
        val principalId = scope.principalId ?: return null
        val written = valueOf(values, selfColumn)
        if (written != null && written.toString() != principalId) {
            throw RowScopeViolationException(
                "Cannot insert into ${policy.entityClass.simpleName}: [$selfColumn] was set to " +
                    "[$written], but a row's creator is whoever is making the request " +
                    "([$principalId]). Leave it unset, or run under DataScopeContext.runAsSystem { } " +
                    "to import rows on another principal's behalf."
            )
        }
        return selfColumn to principalId
    }

    private fun requireAllowed(
        written: Any,
        allowed: Set<String>,
        columnName: String,
        dimension: String,
        policy: RowScopePolicy,
        operation: String,
    ) {
        if (written.toString() in allowed) return
        throw RowScopeViolationException(
            "Cannot $operation ${policy.entityClass.simpleName}: [$columnName] = [$written] is outside " +
                "the subject's '$dimension' scope (${allowed.sorted()}). The row would exist but be " +
                "invisible to the very caller creating it."
        )
    }

    /** Column names are compared the way the database compares them, not case-sensitively. */
    private fun mentions(values: Map<String, Any?>, columnName: String): Boolean =
        values.keys.any { it.equals(columnName, ignoreCase = true) }

    private fun valueOf(values: Map<String, Any?>, columnName: String): Any? =
        values.entries.firstOrNull { it.key.equals(columnName, ignoreCase = true) }?.value
}
