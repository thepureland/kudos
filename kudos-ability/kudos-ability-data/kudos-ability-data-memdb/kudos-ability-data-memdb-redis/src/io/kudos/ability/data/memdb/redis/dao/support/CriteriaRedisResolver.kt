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

    /**
     * 处理"组内 OR"语义：数组中每个元素的 id 集合做并集。
     *
     * @param array 同组的 [Criterion] 或嵌套 [Criteria] 数组
     * @return 并集；空并集返回 null（让上层视作"无约束"）
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
     * 把单个 [Criterion] 解析为 id 集合。
     * value 为 null 时按 operator 是否接受 null 决定结果（不接受 → 空集；接受 → null 表示"无约束"）。
     * 是否走 ZSet 索引：数值型 value 或范围类操作符。
     *
     * @param c 单条条件
     * @return 该条件命中的 id 集合
     * @author K
     * @since 1.0.0
     */
    private fun idsForCriterion(c: Criterion): Set<String>? {
        val value = c.value ?: run {
            if (c.operator.acceptNull) return null
            return emptySet()
        }
        val useZSet = isNumericValue(value) || isRangeOperator(c.operator)
        return if (useZSet) idsFromZSet(c.property, c.operator, value) else idsFromSet(c.property, c.operator, value)
    }

    /**
     * 判定 value 是否能作为 ZSet score（数值或可解析为数值的字符串）。
     *
     * @param value 待判定值
     * @return true 表示可走 ZSet 索引
     * @author K
     * @since 1.0.0
     */
    private fun isNumericValue(value: Any): Boolean {
        if (value is Number) return true
        if (value is String) return value.toDoubleOrNull() != null
        return false
    }

    /**
     * 是否为范围类操作符（必须走 ZSet 才能 range 查询）。
     *
     * @param op 操作符
     * @return true 表示属于 GT/GE/LT/LE/BETWEEN/NOT_BETWEEN
     * @author K
     * @since 1.0.0
     */
    private fun isRangeOperator(op: OperatorEnum): Boolean =
        op in setOf(OperatorEnum.GT, OperatorEnum.GE, OperatorEnum.LT, OperatorEnum.LE, OperatorEnum.BETWEEN, OperatorEnum.NOT_BETWEEN)

    /**
     * 把值转为 Double 作为 ZSet score；非数值字符串和其它类型回落到 [SCORE_MIN]（负方向最远）。
     *
     * @param value 任意值
     * @return Double score
     * @author K
     * @since 1.0.0
     */
    private fun toDouble(value: Any): Double = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: SCORE_MIN
        else -> SCORE_MIN
    }

    /**
     * 通过 Set 索引按等值/IN 操作符查询 id。
     * EQ/IEQ：直接读 set 成员；IN：把 value 拆分后多 set 做 union。
     *
     * @param property 属性名
     * @param operator 操作符（仅支持 EQ/IEQ/IN）
     * @param value 查询值
     * @return id 集合；不支持的操作符返回 null
     * @author K
     * @since 1.0.0
     */
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

    /**
     * 通过 ZSet 索引按数值或范围操作符查询 id。
     *
     * 范围端点用 [SCORE_EPSILON] 处理"严格大于/小于"——浮点等值匹配不可靠，
     * 用一个极小偏移把开区间转闭区间，再让 `rangeByScore` 处理。
     *
     * @param property 属性名
     * @param operator 操作符
     * @param value 查询值（数值或范围）
     * @return id 集合；不支持的操作符返回 null
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

    /** 取 Set 成员的小辅助：转换为 String 集合，空时返回空集 */
    private fun setMembers(key: String): Set<String> =
        redisTemplate.opsForSet().members(key)?.mapNotNullTo(mutableSetOf()) { it.toString() } ?: emptySet()

    /** 取 ZSet score 范围内成员的小辅助：转换为 String 集合，空时返回空集 */
    private fun zsetRange(key: String, min: Double, max: Double): Set<String> =
        redisTemplate.opsForZSet().rangeByScore(key, min, max)
            ?.mapNotNullTo(mutableSetOf()) { it?.toString() } ?: emptySet()

    /** 构造 Set 索引的完整 Redis key */
    private fun setKey(property: String, value: String): String =
        CacheKey.getCacheKey(indexKeyPrefix, "set", property, value)

    /** 构造 ZSet 索引的完整 Redis key */
    private fun zsetKey(property: String): String =
        CacheKey.getCacheKey(indexKeyPrefix, "zset", property)

    /**
     * 从 BETWEEN/NOT_BETWEEN 的 value 中解析 (min, max)。
     * 支持 [ClosedFloatingPointRange] / [Array] 双元素 / [List] 双元素三种形态；
     * 长度不足或含 null 返回 null（调用方按"空结果"处理）。
     *
     * @param value 范围值
     * @return (min, max)，无法解析时 null
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
