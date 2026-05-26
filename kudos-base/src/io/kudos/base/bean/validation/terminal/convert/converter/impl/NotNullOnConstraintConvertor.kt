package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.NotNullOn


/**
 * Converter from the NotNullOn annotation constraint to a terminal constraint.
 *
 * @author K
 * @since 1.0.0
 */
class NotNullOnConstraintConvertor(annotation: Annotation) : DefaultConstraintConvertor(annotation) {

    override fun getRule(constraintAnnotation: Annotation): LinkedHashMap<String, Any> {
        val map = super.getRule(constraintAnnotation)
        require(constraintAnnotation is NotNullOn) { "NotNullOnConstraintConvertor only supports the NotNullOn annotation" }
        val depends = constraintAnnotation.depends
        map["depends"] = super.getRule(depends)
        return map
    }

}