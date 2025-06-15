package io.kudos.base.bean.validation.teminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.NotNullOn


/**
 * NotNullOn注解约束->终端约束的转换器
 *
 * @author K
 * @since 1.0.0
 */
class NotNullOnConstraintConvertor(annotation: Annotation) : DefaultConstraintConvertor(annotation) {

    override fun getRule(constraintAnnotation: Annotation): LinkedHashMap<String, Any> {
        val map = super.getRule(constraintAnnotation)
        constraintAnnotation as NotNullOn
        val depends = constraintAnnotation.depends
        map["depends"] = super.getRule(depends)
        return map
    }

}