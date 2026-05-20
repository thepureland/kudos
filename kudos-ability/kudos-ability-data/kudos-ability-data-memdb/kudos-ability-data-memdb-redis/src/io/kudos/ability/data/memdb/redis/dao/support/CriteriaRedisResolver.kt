package io.kudos.ability.data.memdb.redis.dao.support

import io.kudos.ability.data.memdb.redis.consts.CacheKey
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.data.redis.core.RedisTemplate

/**
 * 将 Criteria 转为 Redis Set/ZSet 查询，得到满足条件的 id 集合。
 * - 属性值为数值型（Number）或范围类操作符：用 ZSet 索引（zset:property）
 * - 其他：用 Set 索引（set:property:value）
 * 逻辑关系：组间 AND，组内数组为 OR。
 *
 * @param indexKeyPrefix 二级索引 key 前缀
 * @param redisTemplate Redis 操作模板
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
internal class CriteriaRedisResolver(
    private val indexKeyPrefix: String,
    private val redisTemplate: RedisTemplate<Any, Any?>
) {

    private companion object {
        /**
         * ZSet 分数下界。**不要写 [Double.MIN_VALUE]**——Java/Kotlin 里它是最小**正**double，
         * 用作下界会让所有负 score 的成员被排除。`-Double.MAX_VALUE` 才是负方向最远。
         */
        private const val SCORE_MIN: Double = -Double.MAX_VALUE
        private const val SCORE_MAX: Double = Double.MAX_VALUE

        /** ZSet score 的等值匹配偏移，规避浮点近似导致的等值匹配漏命中。 */
        private const val SCORE_EPSILON: Double = 1e-10
    }

    /**
     * 解析 Criteria，返回满足条件的 id 集合。
     * @return 满足条件的 id 集合；若 criteria 为空则返回 null（表示“全部”，由调用方用 HKEYS 等处理）
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

    private fun idsForCriterion(c: Criterion): Set<String>? {
        val value = c.value ?: run {
            if (c.operator.acceptNull) return null
            return emptySet()
        }
        val useZSet = isNumericValue(value) || isRangeOperator(c.operator)
        return if (useZSet) idsFromZSet(c.property, c.operator, value) else idsFromSet(c.property, c.operator, value)
    }

    private fun isNumericValue(value: Any): Boolean {
        if (value is Number) return true
        if (value is String) return value.toDoubleOrNull() != null
        return false
    }

    private fun isRangeOperator(op: OperatorEnum): Boolean =
        op in setOf(OperatorEnum.GT, OperatorEnum.GE, OperatorEnum.LT, OperatorEnum.LE, OperatorEnum.BETWEEN, OperatorEnum.NOT_BETWEEN)

    private fun toDouble(value: Any): Double = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: SCORE_MIN
        else -> SCORE_MIN
    }

    private fun idsFromSet(property: String, operator: OperatorEnum, value: Any): Set<String>? = when (operator) {
        OperatorEnum.EQ, OperatorEnum.IEQ -> setMembers(setKey(property, value.toString()))
        OperatorEnum.IN -> {
            val values = when (value) {
                is Collection<*> -> value.map { it.toString() }
                is Array<*> -> value.map { it.toString() }
                is String -> value.split(",").map { it.trim() }
                else -> listOf(value.toString())
            }
            if (values.isEmpty()) emptySet()
            else {
                val keys = values.map { setKey(property, it) }
                redisTemplate.opsForSet().union(keys[0], keys.drop(1))
                    ?.mapNotNullTo(mutableSetOf()) { it.toString() } ?: emptySet()
            }
        }
        else -> null
    }

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
            OperatorEnum.GT -> zsetRange(key, toDouble(value) + SCORE_EPSILON, SCORE_MAX)
            OperatorEnum.GE -> zsetRange(key, toDouble(value), SCORE_MAX)
            OperatorEnum.LT -> zsetRange(key, SCORE_MIN, toDouble(value) - SCORE_EPSILON)
            OperatorEnum.LE -> zsetRange(key, SCORE_MIN, toDouble(value))
            OperatorEnum.BETWEEN -> rangeMinMax(value)?.let { (min, max) -> zsetRange(key, min, max) } ?: emptySet()
            OperatorEnum.NOT_BETWEEN -> rangeMinMax(value)?.let { (min, max) ->
                zsetRange(key, SCORE_MIN, min - SCORE_EPSILON).union(zsetRange(key, max + SCORE_EPSILON, SCORE_MAX))
            } ?: emptySet()
            else -> null
        }
    }

    private fun setMembers(key: String): Set<String> =
        redisTemplate.opsForSet().members(key)?.mapNotNullTo(mutableSetOf()) { it.toString() } ?: emptySet()

    private fun zsetRange(key: String, min: Double, max: Double): Set<String> =
        redisTemplate.opsForZSet().rangeByScore(key, min, max)
            ?.mapNotNullTo(mutableSetOf()) { it?.toString() } ?: emptySet()

    private fun setKey(property: String, value: String): String =
        CacheKey.getCacheKey(indexKeyPrefix, "set", property, value)

    private fun zsetKey(property: String): String =
        CacheKey.getCacheKey(indexKeyPrefix, "zset", property)

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
