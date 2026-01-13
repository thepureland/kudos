package io.kudos.base.query

import io.kudos.base.query.enums.OperatorEnum
import java.io.Serializable

/**
 * 单个查询条件封装类
 * 
 * 用于封装一个查询条件，由属性名、操作符和属性值三部分组成。
 * 
 * 核心组成：
 * - property：要查询的属性名（字段名）
 * - operator：逻辑操作符枚举（如等于、大于、LIKE等）
 * - value：属性值（查询条件的目标值）
 * 
 * 扩展属性：
 * - alias：别名，用于同一属性名多个条件时的区分
 * - encrypt：标识条件值是否已加密
 * 
 * 使用场景：
 * - 动态构建查询条件
 * - ORM框架的查询构建
 * - 复杂查询条件的封装
 * 
 * 注意事项：
 * - 属性值可以为null（取决于操作符是否acceptNull）
 * - 别名用于区分同一属性的多个条件
 * - 加密标识用于标记敏感数据的查询条件
 * - toString方法仅用于调试，不能直接作为SQL执行
 * 
 * @since 1.0.0
 */
class Criterion : Serializable {

    /**
     * 查询条件逻辑操作符枚举
     */
    var operator: OperatorEnum

    /**
     * 要查询的属性名
     */
    var property: String

    /**
     * 要查询的属性名对应的值
     */
    var value: Any? = null

    /**
     * 别名，用于同一属性名多个条件时
     */
    var alias: String? = null

    /**
     * 条件是否已经加密过了.
     */
    var encrypt = false

    /**
     * 封装查询条件
     *
     * @param property 要查询的属性名
     * @param operator 查询条件逻辑操作符枚举
     * @param value 要查询的属性名对应的值
     */
    constructor(property: String, operator: OperatorEnum, value: Any? = null) {
        this.property = property
        this.operator = operator
        this.value = value
    }

    /**
     * 封装查询条件
     *
     * @param property 要查询的属性名
     * @param operator 查询条件逻辑操作符枚举
     * @param value 要查询的属性名对应的值
     * @param alias 别名，用于同一属性名多个条件时
     */
    constructor(property: String, operator: OperatorEnum, value: Any?, alias: String?) : this(property, operator, value) {
        this.alias = alias
    }

    var operatorCode: String
        get() = operator.code
        set(operatorCode) {
            operator = OperatorEnum.enumOf(operatorCode)
        }

//    fun getValue(): Any? {
//        return if (value == null || "" == value) {
//            value
//        } else when (operator) {
//            Operator.LIKE, Operator.ILIKE -> "%$value%"
//            Operator.LIKE_S, Operator.ILIKE_S -> value.toString() + "%"
//            Operator.LIKE_E, Operator.ILIKE_E -> "%$value"
//            else -> value
//        }
//        return value
//    }
//
//    fun setValue(fieldValue: Any?) {
//        value = fieldValue
//    }

    /**
     * 输出查询条件 <br></br>
     * 注：输出内容仅作为查询条件的确认，并非真正执行的sql条件表达式！
     *
     * @return　查询条件字符串
     */
    override fun toString(): String {
        val op = operator.code
        val v = (if (value == null) "" else value)!!
        return "$property $op $v".trim { it <= ' ' }
    }

    companion object {
        private const val serialVersionUID = -8988087738348496878L
    }
}