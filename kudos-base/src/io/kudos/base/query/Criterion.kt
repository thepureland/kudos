package io.kudos.base.query

import io.kudos.base.query.enums.OperatorEnum
import java.io.Serializable

/**
 * 单个查询条件的封装类, 由查询属性名，属性值、逻辑操作符三部分组成
 *
 * @author K
 * @since 1.0.0
 */
class Criterion : Serializable {

    /**
     * 查询条件逻辑操作符枚举
     */
    lateinit var operator: OperatorEnum

    /**
     * 要查询的属性名
     */
    lateinit var property: String

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