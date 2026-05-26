package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.Each
import io.kudos.base.bean.validation.constraint.validator.ConstraintsValidator

/**
 * Converter from the @Each constraint annotation to a terminal constraint.
 *
 * @author K
 * @since 1.0.0
 */
class EachConstraintConvertor(annotation: Annotation) : DefaultConstraintConvertor(annotation) {

    override fun getRule(constraintAnnotation: Annotation): LinkedHashMap<String, Any> {
        val map = linkedMapOf<String, Any>()
        require(constraintAnnotation is Each) { "EachConstraintConvertor only supports the Each annotation" }
        val annotations = ConstraintsValidator.getAnnotations(constraintAnnotation.value)
        annotations.forEach {
            val constraintName = requireNotNull(it.annotationClass.simpleName) { "Cannot resolve annotation name: ${it.annotationClass}" }
            map[constraintName] = super.getRule(it)
        }
        return map
    }

}
