package io.kudos.base.bean.validation.support

import io.kudos.base.bean.BeanKit
import io.kudos.base.support.logic.AndOrEnum
import io.kudos.base.support.logic.LogicOperatorEnum

/**
 * Depends约束验证器，由其他一级约束Validator调用
 *
 * @author K
 * @since 1.0.0
 */
object DependsValidator {

    /**
     * 此方法返回值为depends里面的表达式是否成立，true为成立，false为不成立
     *
     * @param depends 依赖注解
     * @param bean 待校验的Bean
     * @return 是否校验通过
     * @author K
     * @since 1.0.0
     */
    fun validate(depends: Depends, bean: Any): Boolean {
        val leftValues = depends.properties.map { BeanKit.getProperty(bean, it) }.toTypedArray()
        return validate(leftValues, depends.values, depends.logics, depends.andOrEnum)
    }

    /**
     * 校验属性逻辑是否成立，三个数组的大小必须相等
     *
     * @param leftValues 左值数组
     * @param rightValues 右值数组
     * @param operators 操作符数组
     * @param andOrEnum 多组值间的逻辑关系，默认为”与“
     * @return 逻辑是否成立
     * @author K
     * @since 1.0.0
     */
    fun validate(
        leftValues: Array<Any?>, rightValues: Array<String>, operators: Array<LogicOperatorEnum>, andOrEnum: AndOrEnum = AndOrEnum.AND
    ): Boolean {
        if (leftValues.size != rightValues.size || rightValues.size != operators.size) {
            error("左值数组、右值数组、操作符数组的大小必须一致！")
        }

        var result = true
        leftValues.forEachIndexed { index, leftValue ->
            var rightValue = rightValues[index]

            // 数组处理
            if (rightValue.startsWith("[") && rightValue.endsWith("]")) {
                rightValue = rightValue.replaceFirst("\\['".toRegex(), "")
                    .replaceFirst("']".toRegex(), "")
                    .replace("',\\s*'".toRegex(), ",")
            }

            val compare: Boolean = operators[index].compare(leftValue?.toString(), rightValue)
            if (andOrEnum === AndOrEnum.AND) {
                if (!compare) {
                    return false
                }
            } else {
                if (compare) {
                    return true
                } else {
                    result = false
                }
            }
        }
        return result
    }

}
