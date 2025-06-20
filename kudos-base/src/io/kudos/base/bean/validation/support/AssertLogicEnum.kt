package io.kudos.base.bean.validation.support

import io.kudos.base.query.enums.OperatorEnum

/**
 * 断言逻辑枚举
 *
 * @author K
 * @since 1.0.0
 */
enum class AssertLogicEnum {
    /**
     * null断言
     */
    IS_NULL,

    /**
     * 非null断言
     */
    IS_NOT_NULL,

    /**
     * 空断言。字符串判断是否为空串，数组、集合、Map判断是否为空，其他对象toString()后判断是否为空串。null返回false
     */
    IS_EMPTY,

    /**
     * 非空断言。字符串判断是否不为空串，数组、集合、Map判断是否不为空，其他对象toString()后判断是否不为空串。null返回false
     */
    IS_NOT_EMPTY,

    /**
     * null或空或空白。字符串判断是否为空串或空白，数组、集合、Map判断是否为空，其他对象toString()后判断是否为空串或空白。null返回true
     */
    IS_BLANK,

    /**
     * 非null和空和空白。字符串判断是否不为空串和空白，数组、集合、Map判断是否不为空，其他对象toString()后判断是否不为空串和空白。null返回false
     */
    IS_NOT_BLANK;

    /**
     * 按当前逻辑对指定的值进行断言
     *
     * @param value 待检测的值
     * @return 断言结果，true:通过, false:不通过
     * @author K
     * @since 1.0.0
     */
    fun compare(value: Any?): Boolean {
        when (this) {
            IS_NULL -> return OperatorEnum.IS_NULL.compare(value, null)
            IS_NOT_NULL -> return OperatorEnum.IS_NOT_NULL.compare(value, null)
            IS_EMPTY -> return OperatorEnum.IS_EMPTY.compare(value, null)
            IS_NOT_EMPTY -> return OperatorEnum.IS_NOT_EMPTY.compare(value, null)
            IS_BLANK -> {
                if (value == null) {
                    return true
                }
                if (value is String) {
                    return value.isBlank()
                }
                return OperatorEnum.IS_NULL.compare(value, null) || OperatorEnum.IS_EMPTY.compare(value, null)
            }

            IS_NOT_BLANK -> {
                if (value == null) {
                    return false
                }
                if (value is String) {
                    return value.isNotBlank()
                }
                return OperatorEnum.IS_NOT_NULL.compare(value, null) && OperatorEnum.IS_NOT_EMPTY.compare(value, null)
            }

        }
    }
}
