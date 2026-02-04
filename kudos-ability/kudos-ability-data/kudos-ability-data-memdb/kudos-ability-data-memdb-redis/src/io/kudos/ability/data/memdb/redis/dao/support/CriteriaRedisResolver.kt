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
 * @param indexKeyPrefix 二级索引 key 前缀的返回值）
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
internal class CriteriaRedisResolver(
    private val indexKeyPrefix: String,
    private val redisTemplate: RedisTemplate<Any, Any?>
) {

    /**
     * 解析 Criteria，返回满足条件的 id 集合。
     * @return 满足条件的 id 集合；若 criteria 为空则返回 null（表示“全部”，由调用方用 HKEYS 等处理）
     */
    fun resolveToIds(criteria: Criteria?): Set<String>? {
        if (criteria ==null || criteria.isEmpty()) return null
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
        is String -> value.toDoubleOrNull() ?: Double.MIN_VALUE
        else -> Double.MIN_VALUE
    }

    private fun idsFromSet(property: String, operator: OperatorEnum, value: Any): Set<String>? {
        when (operator) {
            OperatorEnum.EQ, OperatorEnum.IEQ -> {
                val key = setKey(property, value.toString())
                return redisTemplate.opsForSet().members(key)?.mapNotNull { it.toString() }?.toSet() ?: emptySet()
            }
            OperatorEnum.IN -> {
                val values = when (value) {
                    is Collection<*> -> value.map { it.toString() }
                    is Array<*> -> value.map { it.toString() }
                    is String -> value.split(",").map { it.trim() }
                    else -> listOf(value.toString())
                }
                if (values.isEmpty()) return emptySet()
                val keys = values.map { setKey(property, it) }
                val union = redisTemplate.opsForSet().union(keys[0], keys.drop(1))
                return union?.mapNotNull { it.toString() }?.toSet() ?: emptySet()
            }
            else -> return null
        }
    }

    private fun idsFromZSet(property: String, operator: OperatorEnum, value: Any): Set<String>? {
        val key = zsetKey(property)
        when (operator) {
            OperatorEnum.EQ -> {
                val v = toDouble(value)
                val ids = redisTemplate.opsForZSet().rangeByScore(key, v, v)
                return ids?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
            }
            OperatorEnum.IN -> {
                val values = when (value) {
                    is Collection<*> -> value.map { toDouble(it!!) }
                    is Array<*> -> value.map { toDouble(it!!) }
                    else -> listOf(toDouble(value))
                }
                var union = emptySet<String>()
                for (v in values) {
                    val ids = redisTemplate.opsForZSet().rangeByScore(key, v, v)?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
                    union = union.union(ids)
                }
                return union
            }
            OperatorEnum.GT -> {
                val v = toDouble(value)
                return redisTemplate.opsForZSet().rangeByScore(key, v + 1e-10, Double.MAX_VALUE)?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
            }
            OperatorEnum.GE -> {
                val v = toDouble(value)
                return redisTemplate.opsForZSet().rangeByScore(key, v, Double.MAX_VALUE)?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
            }
            OperatorEnum.LT -> {
                val v = toDouble(value)
                return redisTemplate.opsForZSet().rangeByScore(key, Double.MIN_VALUE, v - 1e-10)?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
            }
            OperatorEnum.LE -> {
                val v = toDouble(value)
                return redisTemplate.opsForZSet().rangeByScore(key, Double.MIN_VALUE, v)?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
            }
            OperatorEnum.BETWEEN -> {
                val (min, max) = rangeMinMax(value) ?: return emptySet()
                return redisTemplate.opsForZSet().rangeByScore(key, min, max)?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
            }
            OperatorEnum.NOT_BETWEEN -> {
                val (min, max) = rangeMinMax(value) ?: return emptySet()
                val below = redisTemplate.opsForZSet().rangeByScore(key, Double.MIN_VALUE, min - 1e-10)?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
                val above = redisTemplate.opsForZSet().rangeByScore(key, max + 1e-10, Double.MAX_VALUE)?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
                return below.union(above)
            }
            else -> return null
        }
    }

    private fun setKey(property: String, value: String): String =
        CacheKey.getCacheKey(indexKeyPrefix, "set", property, value)

    private fun zsetKey(property: String): String =
        CacheKey.getCacheKey(indexKeyPrefix, "zset", property)

    private fun rangeMinMax(value: Any): Pair<Double, Double>? = when (value) {
        is ClosedFloatingPointRange<*> -> (value.start as Number).toDouble() to (value.endInclusive as Number).toDouble()
        is Array<*> -> if (value.size >= 2) toDouble(value[0]!!) to toDouble(value[1]!!) else null
        is List<*> -> if (value.size >= 2) toDouble(value[0]!!) to toDouble(value[1]!!) else null
        else -> null
    }
}
