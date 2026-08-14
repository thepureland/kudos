package io.kudos.ability.data.memdb.redis.dao.support

import io.kudos.ability.data.memdb.redis.consts.CacheKey
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.data.redis.core.RedisTemplate

/**
 * Translates a Criteria into Redis Set/ZSet queries to obtain the set of ids satisfying the criteria.
 * - Numeric (Number) property values or range-type operators: use the ZSet index (zset:property).
 * - Others: use the Set index (set:property:value).
 * Logical relations: AND between groups; OR within a group's array.
 *
 * @param indexKeyPrefix Secondary index key prefix.
 * @param redisTemplate Redis operation template.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class CriteriaRedisResolver(
    private val indexKeyPrefix: String,
    private val redisTemplate: RedisTemplate<Any, Any?>
) {

    private companion object {
        /**
         * ZSet score lower bound. **Do not use [Double.MIN_VALUE]** — in Java/Kotlin it is the smallest **positive** double,
         * and using it as the lower bound would exclude all members with negative scores. `-Double.MAX_VALUE` is the most negative value.
         */
        private const val SCORE_MIN: Double = -Double.MAX_VALUE
        private const val SCORE_MAX: Double = Double.MAX_VALUE
    }

    /**
     * Parses the Criteria and returns the set of matching ids.
     * @return Set of ids satisfying the conditions; returns null if the criteria is empty (meaning "all", to be handled by the caller via HKEYS, etc.).
     */
    fun resolveToIds(criteria: Criteria?): Set<String>? {
        if (criteria == null || criteria.isEmpty()) return null
        val groups = criteria.getCriterionGroups()
        if (groups.isEmpty()) return null
        var result: Set<String>? = null
        for (group in groups) {
            val groupIds = when (group) {
                is Criterion -> idsForCriterion(group)
                is Array<*> -> idsForOrGroup(group)
                is Criteria -> resolveToIds(group) ?: return null
                else -> null
            }
            if (groupIds == null) continue
            result = result?.intersect(groupIds) ?: groupIds
            if (result.isEmpty()) return emptySet()
        }
        return result ?: emptySet()
    }

    /**
     * Handles "OR within a group" semantics: takes the union of the id sets of each element in the array.
     *
     * @param array Array of [Criterion]s or nested [Criteria]s in the same group.
     * @return The union; an empty union returns null (so the upper layer treats it as "no constraint").
     * @author K
     * @since 1.0.0
     */
    private fun idsForOrGroup(array: Array<*>): Set<String>? {
        var union = emptySet<String>()
        for (elem in array) {
            val ids = when (elem) {
                is Criterion -> idsForCriterion(elem)
                is Criteria -> resolveToIds(elem)
                else -> null
            }
            if (ids != null) union = union.union(ids)
        }
        return union.ifEmpty { null }
    }

    /**
     * Parses a single [Criterion] into a set of ids.
     * When value is null, the result depends on whether the operator accepts null (does not accept → empty set; accepts → null means "no constraint").
     *
     * Index selection:
     *  - Range operators always need the ZSet index (only it can answer a score range).
     *  - Equality goes to the Set index, which is where `filterableProperties` live — **regardless of the
     *    value's type**. Routing on "the value looks numeric" alone would send `type = 1` to the ZSet index and
     *    silently return nothing whenever `type` is declared filterable but not sortable, which is the natural
     *    way to declare an enum-like numeric column.
     *  - A numeric value whose Set index key is absent falls back to the ZSet index, so properties declared only
     *    in `sortableProperties` still answer equality queries.
     *
     * @param c Single condition.
     * @return The set of ids matching this condition.
     * @author K
     * @since 1.0.0
     */
    private fun idsForCriterion(c: Criterion): Set<String>? {
        val value = c.value ?: run {
            if (c.operator.acceptNull) return null
            return emptySet()
        }
        if (isRangeOperator(c.operator)) return idsFromZSet(c.property, c.operator, value)
        if (!isNumericValue(value)) return idsFromSet(c.property, c.operator, value)
        return if (hasSetIndexFor(c.property, c.operator, value)) idsFromSet(c.property, c.operator, value)
        else idsFromZSet(c.property, c.operator, value)
    }

    /**
     * Whether a Set index key exists for this equality condition, i.e. whether the property was written as a
     * filterable one. Used only to disambiguate numeric values, which both index types could serve.
     *
     * @return true if at least one Set index key backing this condition exists.
     * @author K
     * @since 1.0.0
     */
    private fun hasSetIndexFor(property: String, operator: OperatorEnum, value: Any): Boolean = when (operator) {
        OperatorEnum.EQ, OperatorEnum.IEQ -> redisTemplate.hasKey(setKey(property, value.toString()))
        OperatorEnum.IN -> inValuesOf(value).any { redisTemplate.hasKey(setKey(property, it)) }
        else -> false
    }

    /** Normalizes the many accepted shapes of an IN value into the list of its string members. */
    private fun inValuesOf(value: Any): List<String> = when (value) {
        is Collection<*> -> value.map { it.toString() }
        is Array<*> -> value.map { it.toString() }
        is String -> value.split(",").map { it.trim() }
        else -> listOf(value.toString())
    }

    /**
     * Determines whether value can be used as a ZSet score (a number or a string parseable as a number).
     *
     * @param value Value to test.
     * @return true if the ZSet index can be used.
     * @author K
     * @since 1.0.0
     */
    private fun isNumericValue(value: Any): Boolean {
        if (value is Number) return true
        if (value is String) return value.toDoubleOrNull() != null
        return false
    }

    /**
     * Whether the operator is a range-type operator (must use ZSet to do range queries).
     *
     * @param op Operator.
     * @return true if it belongs to GT/GE/LT/LE/BETWEEN/NOT_BETWEEN.
     * @author K
     * @since 1.0.0
     */
    private fun isRangeOperator(op: OperatorEnum): Boolean =
        op in setOf(OperatorEnum.GT, OperatorEnum.GE, OperatorEnum.LT, OperatorEnum.LE, OperatorEnum.BETWEEN, OperatorEnum.NOT_BETWEEN)

    /**
     * Converts the value to a Double for use as a ZSet score; non-numeric strings and other types fall back to [SCORE_MIN] (the most negative value).
     *
     * @param value Any value.
     * @return Double score.
     * @author K
     * @since 1.0.0
     */
    private fun toDouble(value: Any): Double = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: SCORE_MIN
        else -> SCORE_MIN
    }

    /**
     * Queries ids via the Set index for equality / IN operators.
     * EQ/IEQ: directly read set members; IN: split value and union multiple sets.
     *
     * @param property Property name.
     * @param operator Operator (only EQ/IEQ/IN are supported).
     * @param value Query value.
     * @return Set of ids; returns null for unsupported operators.
     * @author K
     * @since 1.0.0
     */
    private fun idsFromSet(property: String, operator: OperatorEnum, value: Any): Set<String>? = when (operator) {
        OperatorEnum.EQ, OperatorEnum.IEQ -> setMembers(setKey(property, value.toString()))
        OperatorEnum.IN -> {
            val values = inValuesOf(value)
            if (values.isEmpty()) emptySet()
            else {
                val keys = values.map { setKey(property, it) }
                redisTemplate.opsForSet().union(keys[0], keys.drop(1))
                    ?.mapNotNullTo(mutableSetOf()) { it.toString() } ?: emptySet()
            }
        }
        else -> null
    }

    /**
     * Queries ids via the ZSet index for numeric or range operators.
     *
     * Strict bounds (GT/LT/NOT_BETWEEN) are expressed by moving the endpoint one ULP with [Math.nextUp] /
     * [Math.nextDown], which is exact at every magnitude. A fixed epsilon must NOT be used here: for scores
     * around 1e12 (millisecond timestamps) any epsilon below the local ULP (~2.4e-4) is absorbed by rounding
     * and GT silently degenerates into GE.
     *
     * @param property Property name.
     * @param operator Operator.
     * @param value Query value (numeric or range).
     * @return Set of ids; returns null for unsupported operators.
     * @author K
     * @since 1.0.0
     */
    private fun idsFromZSet(property: String, operator: OperatorEnum, value: Any): Set<String>? {
        val key = zsetKey(property)
        return when (operator) {
            OperatorEnum.EQ -> toDouble(value).let { zsetRange(key, it, it) }
            OperatorEnum.IN -> {
                val values = when (value) {
                    is Collection<*> -> value.mapNotNull { it?.let(::toDouble) }
                    is Array<*> -> value.mapNotNull { it?.let(::toDouble) }
                    else -> listOf(toDouble(value))
                }
                values.fold(emptySet()) { acc, v -> acc.union(zsetRange(key, v, v)) }
            }
            OperatorEnum.GT -> zsetRange(key, Math.nextUp(toDouble(value)), SCORE_MAX)
            OperatorEnum.GE -> zsetRange(key, toDouble(value), SCORE_MAX)
            OperatorEnum.LT -> zsetRange(key, SCORE_MIN, Math.nextDown(toDouble(value)))
            OperatorEnum.LE -> zsetRange(key, SCORE_MIN, toDouble(value))
            OperatorEnum.BETWEEN -> rangeMinMax(value)?.let { (min, max) -> zsetRange(key, min, max) } ?: emptySet()
            OperatorEnum.NOT_BETWEEN -> rangeMinMax(value)?.let { (min, max) ->
                zsetRange(key, SCORE_MIN, Math.nextDown(min)).union(zsetRange(key, Math.nextUp(max), SCORE_MAX))
            } ?: emptySet()
            else -> null
        }
    }

    /** Small helper to fetch Set members: converts to a String set; returns empty set if empty. */
    private fun setMembers(key: String): Set<String> =
        redisTemplate.opsForSet().members(key)?.mapNotNullTo(mutableSetOf()) { it.toString() } ?: emptySet()

    /** Small helper to fetch ZSet members within a score range: converts to a String set; returns empty set if empty. */
    private fun zsetRange(key: String, min: Double, max: Double): Set<String> =
        redisTemplate.opsForZSet().rangeByScore(key, min, max)
            ?.mapNotNullTo(mutableSetOf()) { it?.toString() } ?: emptySet()

    /** Builds the full Redis key for the Set index. */
    private fun setKey(property: String, value: String): String =
        CacheKey.getCacheKey(indexKeyPrefix, "set", property, value)

    /** Builds the full Redis key for the ZSet index. */
    private fun zsetKey(property: String): String =
        CacheKey.getCacheKey(indexKeyPrefix, "zset", property)

    /**
     * Parses (min, max) from the value of BETWEEN / NOT_BETWEEN.
     * Supports three shapes: [ClosedFloatingPointRange], two-element [Array], and two-element [List];
     * returns null if the length is insufficient or contains null (the caller treats this as an "empty result").
     *
     * @param value Range value.
     * @return (min, max), or null if it cannot be parsed.
     * @author K
     * @since 1.0.0
     */
    private fun rangeMinMax(value: Any): Pair<Double, Double>? = when (value) {
        is ClosedFloatingPointRange<*> -> (value.start as Number).toDouble() to (value.endInclusive as Number).toDouble()
        is Array<*> -> value.takeIf { it.size >= 2 }?.let { arr ->
            val a = arr[0]
            val b = arr[1]
            if (a == null || b == null) null else toDouble(a) to toDouble(b)
        }
        is List<*> -> value.takeIf { it.size >= 2 }?.let { list ->
            val a = list[0]
            val b = list[1]
            if (a == null || b == null) null else toDouble(a) to toDouble(b)
        }
        else -> null
    }
}
