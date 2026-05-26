package io.kudos.base.bean.validation.terminal.convert.converter.impl

/**
 * Converter from the Remote constraint annotation to a terminal constraint.
 *
 * @author K
 * @since 1.0.0
 */
class RemoteConstraintConvertor(annotation: Annotation) : DefaultConstraintConvertor(annotation) {

    override fun getRule(constraintAnnotation: Annotation): LinkedHashMap<String, Any> {
        val map = super.getRule(constraintAnnotation)
        map.remove("checkClass")
        return map
    }

}