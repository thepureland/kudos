package io.kudos.base.bean.validation.teminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.Constraints
import io.kudos.base.bean.validation.constraint.validator.ConstraintsValidator
import io.kudos.base.support.logic.AndOrEnum


/**
 * Constraints约束注解->终端约束转换器
 *
 * @author K
 * @since 1.0.0
 */
class ConstraintsConstraintConvertor(annotation: Annotation) : DefaultConstraintConvertor(annotation) {

    override fun getRule(constraintAnnotation: Annotation): LinkedHashMap<String, Any> {
        val map = linkedMapOf<String, Any>()
        require(constraintAnnotation is Constraints) { "ConstraintsConstraintConvertor 仅支持 Constraints 注解" }
        val annotations = ConstraintsValidator.getAnnotations(constraintAnnotation)
        annotations.forEach {
            val constraintName = requireNotNull(it.annotationClass.simpleName) { "无法解析注解名: ${it.annotationClass}" }
            val ruleMap = super.getRule(it)
            if (constraintAnnotation.andOr == AndOrEnum.OR) {
                ruleMap.remove("message") // 当子约束间的校验逻辑为OR时，子约束的message无意义，提示信息取Constraints约束的message
            }
            map[constraintName] = ruleMap
        }
        if (constraintAnnotation.andOr == AndOrEnum.OR) {
            map["andOr"] = AndOrEnum.OR // 缺省为AND
            // 当子约束间的校验逻辑为OR时，Constraints的message才有意义(为AND时提示信息取子约束的message)
            map["message"] = constraintAnnotation.message
        }
        return map
    }

}