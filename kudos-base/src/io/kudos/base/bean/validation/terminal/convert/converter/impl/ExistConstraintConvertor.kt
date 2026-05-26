package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.Exist
import io.kudos.base.bean.validation.constraint.validator.ConstraintsValidator


/**
 * Converter from the Exist annotation constraint to a terminal constraint.
 *
 * @author K
 * @since 1.0.0
 */
class ExistConstraintConvertor(annotation: Annotation) : DefaultConstraintConvertor(annotation) {

    override fun getRule(constraintAnnotation: Annotation): LinkedHashMap<String, Any> {
        val map = linkedMapOf<String, Any>()
        require(constraintAnnotation is Exist) { "ExistConstraintConvertor only supports the Exist annotation" }
        val annotations = ConstraintsValidator.getAnnotations(constraintAnnotation.value)
        annotations.forEach {
            val constraintName = requireNotNull(it.annotationClass.simpleName) { "Cannot resolve annotation name: ${it.annotationClass}" }
            val ruleMap = super.getRule(it)
            ruleMap.remove("message") // For the Exist constraint, sub-constraint messages are meaningless.
            map[constraintName] = ruleMap
        }
        map["message"] = constraintAnnotation.message
        return map
    }

}