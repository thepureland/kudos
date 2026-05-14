package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.BeanKit
import io.kudos.base.bean.validation.constraint.annotations.Compare
import io.kudos.base.bean.validation.support.DependsValidator
import io.kudos.base.bean.validation.support.ValidationContext
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/**
 * Compare约束的验证器
 *
 * @author K
 * @since 1.0.0
 */
class CompareValidator : ConstraintValidator<Compare, Any?> {
    private lateinit var compare: Compare

    override fun initialize(compare: Compare) {
        this.compare = compare
    }

    override fun isValid(value: Any?, constraintValidatorContext: ConstraintValidatorContext): Boolean {
        val bean = requireNotNull(ValidationContext.get(constraintValidatorContext)) {
            "CompareValidator 需要 ValidationContext 中存在 bean"
        }

        // 依赖的前提条件不成立时，代表无须校验比较约束，直接放行
        val depends = compare.depends
        if (depends.properties.isNotEmpty()) {
            if (!DependsValidator.validate(depends, bean)) {
                return true
            }
        }

        // 比较
        val anotherValue = BeanKit.getProperty(bean, compare.anotherProperty)
        if (value == null && anotherValue == null) {
            return true
        }
        if (value == null || anotherValue == null) {
            return false
        }
        if (value::class != anotherValue::class) {
            throw IllegalArgumentException(
                "【Compare】约束注解校验的两个属性类型必须相同！" +
                        "(${compare.anotherProperty}: ${anotherValue::class.qualifiedName}, " +
                        "当前属性: ${value::class.qualifiedName})"
            )
        }
        if (value is Array<*> && anotherValue is Array<*>) {
            // 数组长度不一致属于运行时数据问题（如用户两次输入的密码组长度不同），
            // 按校验失败处理，让上层得到正常的 ConstraintViolation 而非异常。
            if (value.size != anotherValue.size) {
                return false
            }
            value.forEachIndexed { index, v ->
                if (v !is Comparable<*> || anotherValue[index] !is Comparable<*>) {
                    throw IllegalArgumentException(
                        "【Compare】约束注解校验的两个数组中的每个元素的类型必须都实现【Comparable】接口！"
                    )
                }
                val result = compare.logic.compare(v, anotherValue[index])
                if (!result) { // 只要数组中一对元素校验不通过，就当整个校验不过
                    return false
                }
            }
            return true
        } else {
            // 处理值不是数组的情况
            if (value !is Comparable<*>) {
                throw IllegalArgumentException(
                    "【Compare】约束注解校验的两个属性类型必须都实现【Comparable】接口！" +
                            "(实际类型: ${value::class.qualifiedName})"
                )
            }
            return compare.logic.compare(value, anotherValue)
        }
    }
}
