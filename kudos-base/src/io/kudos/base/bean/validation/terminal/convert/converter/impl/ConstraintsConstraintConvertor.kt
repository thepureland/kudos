package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.Constraints
import io.kudos.base.bean.validation.constraint.validator.ConstraintsValidator
import io.kudos.base.support.logic.AndOrEnum


/**
 * Converter from the Constraints constraint annotation to terminal constraints.
 *
 * @author K
 * @since 1.0.0
 */
class ConstraintsConstraintConvertor(annotation: Annotation) : DefaultConstraintConvertor(annotation) {

    override fun getRule(constraintAnnotation: Annotation): LinkedHashMap<String, Any> {
        val map = linkedMapOf<String, Any>()
        require(constraintAnnotation is Constraints) { "ConstraintsConstraintConvertor only supports the Constraints annotation" }
        val annotations = ConstraintsValidator.getAnnotations(constraintAnnotation)
        annotations.forEach {
            val constraintName = requireNotNull(it.annotationClass.simpleName) { "Unable to resolve annotation name: ${it.annotationClass}" }
            val ruleMap = super.getRule(it)
            if (constraintAnnotation.andOr == AndOrEnum.OR) {
                ruleMap.remove("message") // When sub-constraints are combined with OR, each sub-constraint's message is meaningless; the message comes from the Constraints annotation
            }
            map[constraintName] = ruleMap
        }
        if (constraintAnnotation.andOr == AndOrEnum.OR) {
            map["andOr"] = AndOrEnum.OR // default is AND
            // The Constraints message is only meaningful when sub-constraints are combined with OR (with AND, the message comes from each sub-constraint)
            map["message"] = constraintAnnotation.message
        }
        return map
    }

}