package io.kudos.base.bean.validation.teminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.DictEnumCode
import io.kudos.base.lang.EnumKit

/**
 * DictEnumCode约束注解->终端约束转换器
 *
 * @author K
 * @since 1.0.0
 */
class DictEnumCodeConstraintConvertor(annotation: Annotation) : DefaultConstraintConvertor(annotation) {

    override fun getRule(constraintAnnotation: Annotation): LinkedHashMap<String, Any> {
        val map = super.getRule(constraintAnnotation)
        constraintAnnotation as DictEnumCode
        map.remove("enumClass")
        val codeMap = EnumKit.getCodeMap(constraintAnnotation.enumClass)
        map["values"] = codeMap.keys
        return map
    }

}