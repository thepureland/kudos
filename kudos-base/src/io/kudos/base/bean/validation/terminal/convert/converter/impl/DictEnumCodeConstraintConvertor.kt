package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.DictEnumItemCode
import io.kudos.base.lang.EnumKit

/**
 * Converter from the DictEnumCode constraint annotation to a terminal constraint.
 *
 * @author K
 * @since 1.0.0
 */
class DictEnumCodeConstraintConvertor(annotation: Annotation) : DefaultConstraintConvertor(annotation) {

    override fun getRule(constraintAnnotation: Annotation): LinkedHashMap<String, Any> {
        val map = super.getRule(constraintAnnotation)
        require(constraintAnnotation is DictEnumItemCode) { "DictEnumCodeConstraintConvertor only supports the DictEnumCode annotation" }
        map.remove("enumClass")
        val codeMap = EnumKit.getCodeMap(constraintAnnotation.enumClass)
        map["values"] = codeMap.keys
        return map
    }

}