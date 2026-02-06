package io.kudos.base.query.enums

import io.kudos.base.enums.ienums.IDictEnum
import io.kudos.base.lang.EnumKit
import io.kudos.base.lang.collections.containsAll


/**
 * 查询逻辑操作符枚举
 *
 * @author K
 * @since 1.0.0
 */
enum class OperatorEnum(
    override val code: String,
    override val trans: String,
    val acceptNull: Boolean = false, // 值是否可接受null
    val stringOnly: Boolean = false  // 操作值只接收字符串类型
) : IDictEnum {

    /**
     * 等于
     */
    EQ("=", "等于"),

    /**
     * 忽略大小写等于
     */
    IEQ("I=", "忽略大小写等于", false, true),

    /**
     * 不等于
     */
    NE("!=", "不等于", false, false),

    /**
     * 小于大于(不等于)
     */
    LG("<>", "小于大于(不等于)"),

    /**
     * 大于等于
     */
    GE(">=", "大于等于"),

    /**
     * 小于等于
     */
    LE("<=", "小于等于"),

    /**
     * 大于
     */
    GT(">", "大于"),

    /**
     * 小于
     */
    LT("<", "小于"),

    /**
     * 等于属性
     */
    EQ_P("P=", "等于属性", false, false),

    /**
     * 不等于属性
     */
    NE_P("P!=", "不等于属性", false, false),

    /**
     * 大于等于属性
     */
    GE_P("P>=", "大于等于属性", false, false),

    /**
     * 小于等于属性
     */
    LE_P("P<=", "小于等于属性", false, false),

    /**
     * 大于属性
     */
    GT_P("P>", "大于属性", false, false),

    /**
     * 小于属性
     */
    LT_P("P<", "小于属性", false, false),

    /**
     * 匹配字符串任意位置
     */
    LIKE("LIKE", "任意位置匹配", false, true),

    /**
     * 匹配字符串前面
     */
    LIKE_S("LIKE_S", "匹配前面", false, true),

    /**
     * 匹配字符串后面
     */
    LIKE_E("LIKE_E", "匹配后面", false, true),

    /**
     * 忽略大小写匹配字符串任意位置
     */
    ILIKE("ILIKE", "忽略大小写任意位置匹配", false, true),

    /**
     * 忽略大小写匹配字符串前面
     */
    ILIKE_S("ILIKE_S", "忽略大小写匹配前面", false, true),

    /**
     * 忽略大小写匹配字符串后面
     */
    ILIKE_E("ILIKE_E", "忽略大小写匹配后面", false, true),

    /**
     * in查询
     */
    IN("IN", "in查询"),

    /**
     * not in查询
     */
    NOT_IN("NOT IN", "not in查询"),

    /**
     * 是否为null
     */
    IS_NULL("IS NULL", "判空", true, false),

    /**
     * 是否不为null
     */
    IS_NOT_NULL("IS NOT NULL", "非空", true, false),

    /**
     * 是否为空
     * 字符串判断是否为空串，数组、集合、Map判断是否为空，其他对象toString()后判断是否为空串
     */
    IS_EMPTY("=''", "等于空串", true, true),

    /**
     * 是否不为空串
     */
    IS_NOT_EMPTY("!=''", "不等于空串", true, true),

    /**
     * 在两者间
     */
    BETWEEN("BETWEEN", "在两者间", false, false),

    /**
     * 不在两者间
     */
    NOT_BETWEEN("NOT BETWEEN", "不在两者间", false, false);

    /**
     * 根据当前操作符比较两个值
     * 
     * 根据操作符类型执行相应的比较逻辑，支持多种数据类型和比较方式。
     * 
     * 支持的操作符：
     * 1. 相等比较：
     *    - EQ：严格相等（==）
     *    - IEQ：忽略大小写相等（仅字符串）
     *    - NE/LG：不相等（!=）
     * 2. 大小比较：
     *    - GE：大于等于（>=）
     *    - LE：小于等于（<=）
     *    - GT：大于（>）
     *    - LT：小于（<）
     * 3. 字符串匹配：
     *    - LIKE：包含（contains）
     *    - LIKE_S：以...开始（startsWith）
     *    - LIKE_E：以...结束（endsWith）
     *    - ILIKE：忽略大小写包含
     *    - ILIKE_S：忽略大小写开始
     *    - ILIKE_E：忽略大小写结束
     * 4. 集合操作：
     *    - IN：包含在集合中
     *    - NOT_IN：不包含在集合中
     * 5. 空值判断：
     *    - IS_NULL：为null
     *    - IS_NOT_NULL：不为null
     *    - IS_EMPTY：为空（字符串、集合、数组、Map）
     *    - IS_NOT_EMPTY：不为空
     * 6. 范围判断：
     *    - BETWEEN：在范围内
     *    - NOT_BETWEEN：不在范围内
     * 
     * 类型处理：
     * - 比较操作符要求值实现Comparable接口
     * - 字符串匹配操作符只支持String类型
     * - 空值判断操作符支持多种类型（String、Array、Collection、Map）
     * - IN操作符支持多种类型转换
     * 
     * 空值处理：
     * - 对于EQ/IEQ：null == null 返回true
     * - 对于NE：null != null 返回false
     * - 对于大小比较：null值通常返回false
     * - 对于IS_EMPTY：null返回false（IS_NOT_EMPTY返回true）
     * 
     * 注意事项：
     * - 类型不匹配时通常返回false
     * - 使用类型转换（as）可能抛出ClassCastException
     * - 字符串匹配会去除首尾空白字符
     * - ILIKE系列操作符会转换为小写进行比较
     * 
     * @param v1 左值，待比较的值
     * @param v2 右值，比较的目标值（对于IS_NULL等操作符无意义）
     * @return true表示满足逻辑关系，false表示不满足
     */
    @Suppress("UNCHECKED_CAST")
    fun compare(v1: Any?, v2: Any?): Boolean {
        return when (this) {
            EQ -> {
                if (v1 == null && v2 == null) {
                    return true
                }
                if (v1 == null || v2 == null) {
                    false
                } else v1 == v2
            }
            IEQ -> {
                if (v1 == null && v2 == null) {
                    return true
                }
                if (v1 == null || v2 == null) {
                    return false
                }
                if (v1 is String && v2 is String) {
                    v1.equals(v2, ignoreCase = true)
                } else v1 == v2
            }
            NE, LG -> {
                if (v1 == null && v2 == null) {
                    return false
                }
                if (v1 == null || v2 == null) {
                    return true
                }
                if (v1 is String && v2 is String) {
                    v1 != v2
                } else v1 != v2
            }
            GE -> {
                if (v1 == null && v2 == null) {
                    return true
                }
                if (v1 is Comparable<*> && v2 is Comparable<*>) {
                    v1 as Comparable<Any> >= v2 as Comparable<Any>
                } else false
            }
            LE -> {
                if (v1 == null && v2 == null) {
                    return true
                }
                if (v1 is Comparable<*> && v2 is Comparable<*>) {
                    v1 as Comparable<Any> <= v2 as Comparable<Any>
                } else false
            }
            GT -> {
                if (v1 is Comparable<*> && v2 is Comparable<*>) {
                    v1 as Comparable<Any> > v2 as Comparable<Any>
                } else false
            }
            LT -> {
                if (v1 is Comparable<*> && v2 is Comparable<*>) {
                    v1 as Comparable<Any> < v2 as Comparable<Any>
                } else false
            }
            LIKE -> {
                if (v1 is String && v2 is String) {
                    v1.contains((v2 as CharSequence?)!!)
                } else false
            }
            LIKE_S -> {
                if (v1 is String && v2 is String) {
                    v1.trim { it <= ' ' }.startsWith((v2 as String?)!!)
                } else false
            }
            LIKE_E -> {
                if (v1 is String && v2 is String) {
                    v1.trim { it <= ' ' }.endsWith((v2 as String?)!!)
                } else false
            }
            ILIKE -> {
                if (v1 is String && v2 is String) {
                    v1.lowercase().contains(v2.lowercase())
                } else false
            }
            ILIKE_S -> {
                if (v1 is String && v2 is String) {
                    v1.trim { it <= ' ' }.lowercase().startsWith(v2.lowercase())
                } else false
            }
            ILIKE_E -> {
                if (v1 is String && v2 is String) {
                    v1.trim { it <= ' ' }.lowercase().endsWith(v2.lowercase())
                } else false
            }
            IN -> inOperation(v1, v2)
            NOT_IN -> !inOperation(v1, v2)
            IS_NULL -> v1 == null
            IS_NOT_NULL -> v1 != null
            IS_NOT_EMPTY -> {
                if (v1 == null) {
                    return true
                }
                if (v1 is String) {
                    return v1.isNotEmpty()
                }
                if (v1 is Array<*>) {
                    return (v1 as Array<Any?>).isNotEmpty()
                }
                if (v1 is Collection<*>) {
                    return !v1.isEmpty()
                }
                if (v1 is Map<*, *>) {
                    (v1 as Map<*, *>?)!!.isNotEmpty()
                } else v1.toString().isEmpty()
            }
            IS_EMPTY -> {
                if (v1 == null) {
                    return false
                }
                if (v1 is String) {
                    return v1.isEmpty()
                }
                if (v1 is Array<*>) {
                    return v1.isEmpty()
                }
                if (v1 is Collection<*>) {
                    return v1.isEmpty()
                }
                if (v1 is Map<*, *>) {
                    v1.isEmpty()
                } else v1.toString().isEmpty()
            }
            BETWEEN -> {
                if (v1 is Comparable<*> && v2 is ClosedFloatingPointRange<*>) {
                    (v1 as Comparable<Any>) >= v2.start && v1 <= v2.endInclusive
                } else false
            }
            NOT_BETWEEN -> {
                if (v1 !is Comparable<*> || v2 !is ClosedFloatingPointRange<*>) {
                    true
                } else {
                    (v1 as Comparable<Any>) < v2.start && v1 > v2.endInclusive
                }
            }
            else -> false
        }
    }

    /**
     * 执行IN操作符的比较逻辑
     * 
     * 判断左值是否包含在右值集合中，支持多种数据类型的转换和比较。
     * 
     * 工作流程：
     * 1. 字符串处理：如果两个值都是String，将右值按逗号分割后判断
     * 2. 数组转换：将Array转换为List，统一处理
     * 3. 集合判断：
     *    - 如果右值是Collection：
     *      * 如果左值也是Collection：判断右值是否包含左值的所有元素（containsAll）
     *      * 如果左值不是Collection：判断右值是否包含左值（contains）
     * 4. Map判断：如果两个值都是Map，判断右值是否包含左值的所有键值对
     * 5. 其他情况：返回false
     * 
     * 字符串分割：
     * - 如果两个值都是String，将右值按逗号分割
     * - 例如："a,b,c"会被分割为["a", "b", "c"]
     * - 判断左值是否在这个数组中
     * 
     * 集合包含判断：
     * - 单值判断：使用contains方法
     * - 集合判断：使用containsAll方法（子集判断）
     * - 支持任意Collection类型
     * 
     * Map包含判断：
     * - 使用containsAll方法判断键值对
     * - 要求右值Map包含左值Map的所有键值对
     * 
     * 类型转换：
     * - Array会自动转换为List，便于统一处理
     * - 转换后的值用于后续判断
     * 
     * 注意事项：
     * - 字符串分割使用逗号作为分隔符
     * - 集合判断使用contains/containsAll方法
     * - 如果类型不匹配，返回false
     * - Map的containsAll判断键值对，不是键
     * 
     * @param v1 左值，待判断的值
     * @param v2 右值，集合或Map
     * @return true表示左值在右值中，false表示不在
     */
    private fun inOperation(v1: Any?, v2: Any?): Boolean {
        var value1 = v1
        var value2 = v2
        if (value1 is String && value2 is String) {
            val elems = value2.split(",").toTypedArray()
            return value1 in elems
        }
        if (value1 is Array<*>) {
            value1 = listOf(*value1)
        }
        if (value2 is Array<*>) {
            value2 = listOf(*value2)
        }
        if (value2 is Collection<*>) {
            return if (value1 is Collection<*>) {
                value2.containsAll(value1)
            } else {
                value2.contains(value1)
            }
        }
        return if (value1 is Map<*, *> && value2 is Map<*, *>) {
            value2.containsAll(value1)
        } else false
    }

    companion object Companion {
        fun enumOf(code: String): OperatorEnum {
            var operatorCode = code
            if (operatorCode.isNotBlank()) {
                operatorCode = operatorCode.uppercase()
            }
            return EnumKit.enumOf(OperatorEnum::class, operatorCode) ?: error("非法的Operator code: $operatorCode")
        }
    }
}