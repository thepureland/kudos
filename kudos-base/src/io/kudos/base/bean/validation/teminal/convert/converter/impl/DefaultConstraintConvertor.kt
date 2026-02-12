package io.kudos.base.bean.validation.teminal.convert.converter.impl

import io.kudos.base.bean.validation.teminal.convert.converter.AbstractConstraintConvertor
import kotlin.reflect.full.memberProperties

/**
 * 默认的约束注解->终端约束转换器
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
                    "约束属性值为空: ${constraintAnnotation.annotationClass}.${it.name}"
                }
            }
        }
        return rules
    }

}