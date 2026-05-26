package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.terminal.convert.converter.AbstractConstraintConvertor
import kotlin.reflect.full.memberProperties

/**
 * Default converter from constraint annotation to terminal constraint.
 *
 * @author K
 * @since 1.0.0
 */
open class DefaultConstraintConvertor(annotation: Annotation) : AbstractConstraintConvertor(annotation) {

    public override fun getRule(constraintAnnotation: Annotation): LinkedHashMap<String, Any> {
        val rules = linkedMapOf<String, Any>()
        constraintAnnotation.annotationClass.memberProperties.forEach {
            if (it.name != "groups" && it.name != "payload") {
                rules[it.name] = requireNotNull(it.call(constraintAnnotation)) {
                    "Constraint property value is null: ${constraintAnnotation.annotationClass}.${it.name}"
                }
            }
        }
        return rules
    }

}