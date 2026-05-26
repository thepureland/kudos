package io.kudos.base.query

import io.kudos.base.query.enums.OperatorEnum
import java.io.Serializable
import java.lang.reflect.Array as JavaArray
import java.util.Collections

/**
 * Query-criteria container.
 *
 * Encapsulates multiple query conditions (WHERE clauses), supporting arbitrary combinations and
 * nesting of AND/OR logic.
 *
 * Core features:
 * 1. Condition composition: combines multiple conditions with AND and OR.
 * 2. Nested queries: supports nested Criteria for complex query logic.
 * 3. Condition filtering: automatically removes empty values and invalid conditions.
 * 4. Fluent chaining: provides a chainable API.
 *
 * Data structure:
 * - criterionGroups: stores all condition groups.
 *   - Criterion: a single condition (property, operator, value).
 *   - Criteria: nested query (AND relationship).
 *   - Array<*>: an OR group (elements inside are OR'd; the array as a whole is AND'd with others).
 *
 * Logical relationships:
 * - Top-level elements in criterionGroups are joined with AND.
 * - Elements inside an Array<*> are joined with OR.
 * - Arbitrary nesting is supported.
 *
 * Condition filtering:
 * - null values: added only when the operator's acceptNull is true.
 * - Empty strings: not added (unless the operator accepts null).
 * - Empty collections/maps/arrays: not added.
 * - Empty nested Criteria: not added.
 *
 * Use cases:
 * - Dynamic query construction.
 * - Encapsulating complex query logic.
 * - Query building in ORM frameworks.
 *
 * Notes:
 * - Condition values are filtered automatically to avoid generating invalid queries.
 * - toString is for debugging only and cannot be executed as SQL directly.
 * - Static factory methods are provided to create Criteria.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class Criteria : Serializable {

    /**
     * Elements in this list are joined with AND. Element types are: <br></br>
     * 1. Criterion
     * 2. Criteria
     * 3. Array (elements may be Criterion or Criteria; elements within an array are joined with OR).
     */
    private val criterionGroups = mutableListOf<Any>()

    constructor()

    constructor(property: String, operatorEnum: OperatorEnum, value: Any?) {
        addAnd(property, operatorEnum, value)
    }

    /**
     * Wraps a single query condition.
     *
     * @param criterion
     */
    constructor(criterion: Criterion) {
        addAnd(criterion)
    }


    //region and
    /**
     * Adds a single query condition.
     *
     * @param property property name
     * @param operatorEnum logical operator enum
     * @param value property value
     * @return the current Criteria
     */
    fun addAnd(property: String, operatorEnum: OperatorEnum, value: Any?): Criteria {
        return addAnd(Criterion(property, operatorEnum, value))
    }

    /**
     * Adds multiple query conditions joined with AND.
     *
     * @param criterions vararg conditions
     * @return the current Criteria
     */
    fun addAnd(vararg criterions: Criterion): Criteria {
        if (criterions.isNotEmpty()) {
            addCriterion(criterionGroups, *criterions)
        }
        return this
    }

    /**
     * Adds multiple nested query objects joined with AND.
     *
     * @param criterias vararg Criteria objects
     * @return the current Criteria
     */
    fun addAnd(vararg criterias: Criteria): Criteria {
        if (criterias.isNotEmpty()) {
            addCriteria(criterionGroups, *criterias)
        }
        return this
    }

    /**
     * Adds a condition and a nested query joined with AND.
     *
     * @param criterion query condition
     * @param criteria  nested query
     * @return the current Criteria
     */
    fun addAnd(criterion: Criterion, criteria: Criteria): Criteria {
        return addAnd(criterion).addAnd(criteria)
    }

    /**
     * Adds a nested query and a condition joined with AND.
     *
     * @param criteria  nested query
     * @param criterion query condition
     * @return the current Criteria
     */
    fun addAnd(criteria: Criteria, criterion: Criterion): Criteria {
        return addAnd(criteria).addAnd(criterion)
    }
    //endregion and

    //region or
    /**
     * Adds multiple query conditions joined with OR.
     *
     * @param criterions vararg conditions
     * @return the current Criteria
     */
    fun addOr(vararg criterions: Criterion): Criteria {
        if (criterions.isNotEmpty()) {
            addOrGroup(addCriterion(null, *criterions))
        }
        return this
    }

    /**
     * Adds multiple nested query objects joined with OR.
     *
     * @param criterias vararg Criteria objects
     * @return the current Criteria
     */
    fun addOr(vararg criterias: Criteria): Criteria {
        if (criterias.isNotEmpty()) {
            addOrGroup(addCriteria(null, *criterias))
        }
        return this
    }

    /**
     * Adds a condition and a nested query joined with OR.
     *
     * @param criterion query condition
     * @param criteria  nested query
     * @return the current Criteria
     */
    fun addOr(criterion: Criterion, criteria: Criteria): Criteria {
        val objList = addCriterion(null, criterion)
        addCriteria(objList, criteria)
        addOrGroup(objList)
        return this
    }

    /**
     * Adds a nested query and a condition joined with OR.
     *
     * @param criteria  nested query
     * @param criterion query condition
     * @return the current Criteria
     */
    fun addOr(criteria: Criteria, criterion: Criterion): Criteria {
        val objList = addCriteria(null, criteria)
        addCriterion(objList, criterion)
        addOrGroup(objList)
        return this
    }

    //endregion or

    /**
     * Whether this Criteria has no conditions.
     *
     * @return true if no conditions are present; otherwise false
     * @author K
     * @since 1.0.0
     */
    fun isEmpty(): Boolean {
        return criterionGroups.isEmpty()
    }

    /**
     * Adds query conditions to a list.
     *
     * Conditions are filtered by value validity before being added to the target list.
     *
     * Workflow:
     * 1. Choose the list: when list is null, create a new one; otherwise use the supplied list.
     * 2. Filter conditions: walk through each condition and keep only those with valid values.
     * 3. Validity rules:
     *    - Non-null, non-empty strings: valid.
     *    - Non-empty collections/maps: valid.
     *    - Non-empty object arrays / primitive arrays: valid.
     *    - Operator accepts null: valid (even when value is null).
     * 4. Append valid conditions to the list.
     *
     * Value-filtering rules:
     * - null values: added only when the operator's acceptNull is true.
     * - Empty strings: not added (unless the operator accepts null).
     * - Empty collections/maps/arrays: not added.
     * - Non-empty values: added.
     *
     * Use cases:
     * - Adding AND conditions: list = criterionGroups.
     * - Adding OR conditions: list = null, create a temporary list.
     *
     * @param list target list; if null a new one is created
     * @param criterions conditions to add
     * @return the list after additions
     */
    private fun addCriterion(list: MutableList<Any>?, vararg criterions: Criterion): MutableList<Any> {
        val resultList = list ?: ArrayList(criterions.size)
        criterions.filterTo(resultList, ::shouldAddCriterion)
        return resultList
    }

    /**
     * Determines whether a single condition is valid (i.e. should be added to the condition group).
     */
    private fun shouldAddCriterion(criterion: Criterion): Boolean {
        if (criterion.operator.acceptNull) {
            return true
        }
        val value = criterion.value ?: return false
        return when (value) {
            is String -> value.isNotEmpty()
            is Collection<*> -> value.isNotEmpty()
            is Map<*, *> -> value.isNotEmpty()
            is Array<*> -> value.isNotEmpty()
            else -> !value.javaClass.isArray || JavaArray.getLength(value) > 0
        }
    }

    /**
     * Adds nested Criteria objects to a list, keeping only non-empty ones.
     *
     * Workflow:
     * 1. Choose the list: when list is null, create a new one; otherwise use the supplied list.
     * 2. Filter Criteria: walk through each and keep only non-empty ones (those with conditions).
     * 3. Emptiness check: examines criterionGroups of the nested Criteria.
     * 4. Append non-empty Criteria to the list.
     *
     * Emptiness handling:
     * - If a Criteria's criterionGroups is empty, it is not added.
     * - Prevents empty nested queries and keeps the conditions concise.
     *
     * Use cases:
     * - Adding nested AND queries: list = criterionGroups.
     * - Adding nested OR queries: list = null, create a temporary list.
     *
     * @param list target list; if null a new one is created
     * @param criterias Criteria objects to add
     * @return the list after additions
     */
    private fun addCriteria(list: MutableList<Any>?, vararg criterias: Criteria): MutableList<Any> {
        val resultList = list ?: ArrayList(criterias.size)
        criterias.filterNotTo(resultList) { it.isEmpty() }
        return resultList
    }

    /**
     * Adds an OR group to the condition groups.
     *
     * Converts a list of OR-related conditions into an array and appends it to criterionGroups.
     *
     * Workflow:
     * 1. Check the list: do nothing if empty.
     * 2. Convert to array: the array type marks the group as OR.
     * 3. Append to criterionGroups.
     *
     * OR-group convention:
     * - Array-typed elements in criterionGroups represent OR.
     * - Elements within the array are joined with OR.
     * - The array as a whole is joined with surrounding elements using AND.
     *
     * Data structure:
     * - criterionGroups: List<Any>
     *   - Criterion: a single condition
     *   - Criteria: a nested query (AND)
     *   - Array<*>: an OR group (elements inside are OR'd)
     *
     * @param list list of OR-related conditions
     */
    private fun addOrGroup(list: List<*>) {
        if (list.isNotEmpty()) {
            criterionGroups.add(list.toTypedArray())
        }
    }

    /**
     * Returns a read-only view of the internal condition-group structure of this Criteria.
     *
     * The result is a `Collections.unmodifiableList` view -- it points to the same underlying data
     * and is not copied. The view can be iterated and indexed, but any mutation (add/clear/set, ...)
     * throws `UnsupportedOperationException`, preventing accidental corruption of the Criteria
     * internal state.
     *
     * Element types may be [Criterion] (single condition), [Criteria] (AND nesting), or `Array<*>`
     * (OR group).
     */
    fun getCriterionGroups(): List<Any> {
        return Collections.unmodifiableList(criterionGroups)
    }

    /**
     * Outputs the query conditions. <br></br>
     * Note: the output only conveys the condition structure and must not be executed as SQL!
     *
     * @return a string representation of the conditions
     */
    override fun toString(): String {
        return criterionGroups
            .mapNotNull { renderGroup(it) }
            .joinToString(" AND ")
    }

    private fun renderGroup(group: Any): String? {
        return when (group) {
            is Criterion -> group.toString()
            is Criteria -> group.toString()
            is Array<*> -> {
                if (group.isEmpty()) {
                    null
                } else {
                    val orClause = group.joinToString(" OR ") { it.toString() }
                    "($orClause)"
                }
            }
            else -> null
        }
    }


    companion object {

        /**
         * Creates a Criteria with a single query condition.
         *
         * @param property property name
         * @param operatorEnum logical operator enum
         * @param value property value
         * @return the new Criteria
         */
        fun of(property: String, operatorEnum: OperatorEnum, value: Any?): Criteria =
            Criteria(Criterion(property, operatorEnum, value))

        //region static and

        /**
         * Creates a Criteria from multiple conditions joined with AND.
         *
         * @param criterions vararg conditions
         * @return the new Criteria
         */
        fun and(vararg criterions: Criterion): Criteria = Criteria().addAnd(*criterions)

        /**
         * Creates a Criteria from multiple nested queries joined with AND.
         *
         * @param criterias vararg Criteria objects
         * @return the new Criteria
         */
        fun and(vararg criterias: Criteria): Criteria = Criteria().addAnd(*criterias)

        /**
         * Creates a Criteria from a condition and a nested query joined with AND.
         *
         * @param criterion query condition
         * @param criteria  nested query
         * @return the new Criteria
         */
        fun and(criterion: Criterion, criteria: Criteria): Criteria = Criteria().addAnd(criterion, criteria)

        /**
         * Creates a Criteria from a nested query and a condition joined with AND.
         *
         * @param criteria  nested query
         * @param criterion query condition
         * @return the new Criteria
         */
        fun and(criteria: Criteria, criterion: Criterion): Criteria = Criteria().addAnd(criteria, criterion)

        //endregion static and

        //region static or
        /**
         * Creates a Criteria from multiple conditions joined with OR.
         *
         * @param criterions vararg conditions
         * @return the new Criteria
         */
        fun or(vararg criterions: Criterion): Criteria = Criteria().addOr(*criterions)

        /**
         * Creates a Criteria from multiple nested queries joined with OR.
         *
         * @param criterias vararg Criteria objects
         * @return the new Criteria
         */
        fun or(vararg criterias: Criteria): Criteria = Criteria().addOr(*criterias)

        /**
         * Creates a Criteria from a condition and a nested query joined with OR.
         *
         * @param criterion query condition
         * @param criteria  nested query
         * @return the new Criteria
         */
        fun or(criterion: Criterion, criteria: Criteria): Criteria = Criteria().addOr(criterion, criteria)

        /**
         * Creates a Criteria from a nested query and a condition joined with OR.
         *
         * @param criteria  nested query
         * @param criterion query condition
         * @return the new Criteria
         */
        fun or(criteria: Criteria, criterion: Criterion): Criteria = Criteria().addOr(criteria, criterion)
        //endregion static or
    }



}
