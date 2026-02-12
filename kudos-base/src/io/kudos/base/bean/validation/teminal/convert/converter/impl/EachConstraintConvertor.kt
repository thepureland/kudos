package io.kudos.base.bean.validation.teminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.Each
import io.kudos.base.bean.validation.constraint.validator.ConstraintsValidator

/**
 * Each注解约束->终端约束的转换器
 *
 * @author K
 * @since 1.0.0
 */
class EachConstraintConvertor(annotation: Annotation) : DefaultConstraintConvertor(annotation) {

    override fun getRule(constraintAnnotation: Annotation): LinkedHashMap<String, Any> {
        val map = linkedMapOf<String, Any>()
        constraintAnnotation as Each
        val annotations = ConstraintsValidator.getAnnotations(constraintAnnotation.value)
        annotations.forEach {
            val constraintName = requireNotNull(it.annotationClass.simpleName) { "无法解析注解名: ${it.annotationClass}" }
            map[constraintName] = super.getRule(it)
        }
        return map
    }

}