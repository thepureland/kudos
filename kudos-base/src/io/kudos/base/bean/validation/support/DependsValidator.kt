package io.kudos.base.bean.validation.support

import io.kudos.base.bean.BeanKit
import io.kudos.base.support.logic.AndOrEnum
import io.kudos.base.support.logic.LogicOperatorEnum

/**
 * Depends约束验证器
 * 
 * 用于验证Bean属性之间的依赖关系，支持复杂的逻辑条件判断。
 * 由其他一级约束Validator调用，实现条件验证功能。
 * 
 * 核心功能：
 * 1. 依赖验证：验证多个属性值是否满足指定的逻辑条件
 * 2. 逻辑组合：支持AND和OR两种逻辑关系
 * 3. 操作符支持：支持各种逻辑操作符（等于、大于、LIKE等）
 * 4. 数组值处理：支持数组格式的字符串值（如"['a','b','c']"）
 * 
 * 验证流程：
 * 1. 从Depends注解中提取属性名、值、操作符和逻辑关系
 * 2. 通过反射获取Bean的属性值
 * 3. 调用validate方法进行逻辑判断
 * 4. 返回验证结果（true表示条件成立，false表示不成立）
 * 
 * 逻辑关系：
 * - AND：所有条件都必须满足，任一失败立即返回false（短路求值）
 * - OR：任一条件满足即可，任一成功立即返回true（短路求值）
 * 
 * 数组值格式：
 * - 支持字符串数组格式："['value1','value2','value3']"
 * - 会自动去除方括号和引号，转换为普通字符串
 * - 用于支持IN操作符等需要多个值的场景
 * 
 * 使用场景：
 * - @Depends注解的条件验证
 * - 动态表单的字段依赖验证
 * - 复杂业务规则的验证
 * 
 * 注意事项：
 * - 三个数组（左值、右值、操作符）的大小必须完全一致
 * - 左值会转换为String后再进行比较
 * - 使用短路求值优化性能
 * 
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
     * 校验属性逻辑是否成立
     * 
     * 验证多个属性值是否满足指定的逻辑条件，支持AND和OR两种逻辑关系。
     * 
     * 工作流程：
     * 1. 数组大小检查：三个数组（左值、右值、操作符）的大小必须相等
     * 2. 数组值处理：如果右值是数组格式（如"['a','b','c']"），解析为普通字符串
     * 3. 逐个比较：对每个索引位置的左值和右值使用对应的操作符进行比较
     * 4. 逻辑关系判断：
     *    - AND关系：如果任何一个比较失败，立即返回false
     *    - OR关系：如果任何一个比较成功，立即返回true；全部失败则返回false
     * 
     * 数组值格式：
     * - 支持字符串数组格式："['value1','value2','value3']"
     * - 会自动去除方括号和引号，转换为普通字符串
     * - 支持单引号和逗号分隔
     * 
     * 逻辑关系：
     * - AND：所有条件都必须满足，任一失败立即返回false（短路求值）
     * - OR：任一条件满足即可，任一成功立即返回true（短路求值）
     * 
     * 注意事项：
     * - 三个数组的大小必须完全一致，否则抛出异常
     * - 左值会转换为String后再进行比较
     * - 使用短路求值优化性能，AND遇到失败立即返回，OR遇到成功立即返回
     * 
     * @param leftValues 左值数组，待比较的属性值
     * @param rightValues 右值数组，比较的目标值（支持数组格式字符串）
     * @param operators 操作符数组，每个位置对应的比较操作符
     * @param andOrEnum 多组值间的逻辑关系，AND表示所有条件必须满足，OR表示任一条件满足即可
     * @return true表示逻辑成立，false表示逻辑不成立
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
