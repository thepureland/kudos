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
        val bean = ValidationContext.get(constraintValidatorContext)!!

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
            error("【Compare】约束注解校验的两个属性类型必须相同！")
        }
        if (value is Array<*> && anotherValue is Array<*>) {
            // 处理值是数组的情况
            if (value.size != anotherValue.size) {
                error("【Compare】约束注解校验的两个数组的大小必须相等！")
            }
            value.forEachIndexed { index, v ->
                if (v !is Comparable<*> || anotherValue[index] !is Comparable<*>) {
                    error("【Compare】约束注解校验的两个数组中的每个元素的类型必须都实现【Comparable】接口！")
                }
                val result = compare.logic.compare(v, anotherValue[index] as Comparable<*>)
                if (!result) { // 只要数组中一对元素校验不通过，就当整个校验不过
                    return false
                }
            }
            return true
        } else {
            // 处理值不是数组的情况
            if (value !is Comparable<*>) {
                error("【Compare】约束注解校验的两个属性类型必须都实现【Comparable】接口！")
            }
            return compare.logic.compare(value, anotherValue as Comparable<*>)
        }
    }
}
