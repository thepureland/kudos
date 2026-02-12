package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.Custom
import io.kudos.base.bean.validation.support.IBeanValidator
import io.kudos.base.bean.validation.support.ValidationContext
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import kotlin.reflect.KClass

/**
 * Custom约束验证器
 *
 * @author K
 * @since 1.0.0
 */
class CustomValidator : ConstraintValidator<Custom, Any?> {
    private lateinit var custom: Custom

    override fun initialize(custom: Custom) {
        this.custom = custom
    }

    override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
        return validate(custom.checkClass, value, context)
    }

    companion object {
        fun validate(
            checkClass: KClass<out IBeanValidator<*>>, value: Any?, context: ConstraintValidatorContext
        ): Boolean {
            if (value == null) {
                return true
            }

            val bean = requireNotNull(ValidationContext.get(context)) {
                "CustomValidator 需要 ValidationContext 中存在 bean"
            }
            val validator = checkClass.java.getDeclaredConstructor().newInstance()
            val validateMethod = validator.javaClass.methods.firstOrNull {
                it.name == "validate" && it.parameterCount == 1
            } ?: error("校验器【${checkClass.qualifiedName}】缺少 validate(bean) 方法")
            val result = validateMethod.invoke(validator, bean)
            return result as? Boolean
                ?: error("校验器【${checkClass.qualifiedName}】validate 返回值必须为 Boolean")
        }
    }

}
