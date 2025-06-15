package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.BeanKit.getProperty
import io.kudos.base.bean.validation.constraint.annotations.AtLeast
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/**
 * AtLeast约束验证器
 *
 * @author K
 * @since 1.0.0
 */
class AtLeastValidator : ConstraintValidator<AtLeast, Any> {
    private lateinit var atLeast: AtLeast

    override fun initialize(atLeast: AtLeast) {
        this.atLeast = atLeast
        check(atLeast.count >= 0) { "@AtLeast约束指定的count值不能为负数！" }
        check(atLeast.count <= atLeast.properties.size) {
            "@AtLeast约束指定的count值【${atLeast.count}】不能比property的个数【${atLeast.properties.size}】大！"
        }
    }

    override fun isValid(bean: Any, constraintValidatorContext: ConstraintValidatorContext?): Boolean {
        var count = 0
        for (prop in atLeast.properties) {
            val value = getProperty(bean, prop)
            if (atLeast.logic.compare(value)) {
                count++
            }
        }
        return count >= atLeast.count
    }
}
