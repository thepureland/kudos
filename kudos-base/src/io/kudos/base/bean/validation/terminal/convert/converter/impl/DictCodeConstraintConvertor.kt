package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.bean.validation.terminal.convert.converter.IDictItemCodeFinder
import java.util.LinkedHashMap
import java.util.ServiceLoader

/**
 * 字典码约束转换器
 * 用于将字典码注解转换为前端校验规则，支持动态获取字典数据并生成校验规则
 */
class DictCodeConstraintConvertor(annotation: Annotation) : DefaultConstraintConvertor(annotation) {

    override fun getRule(constraintAnnotation: Annotation): LinkedHashMap<String, Any> {
        val map = super.getRule(constraintAnnotation)
        require(constraintAnnotation is DictItemCode) { "DictCodeConstraintConvertor 仅支持 DictCode 注解" }
        val dictCode = constraintAnnotation
        val codes = dictCodeConvertor(dictCode.atomicServiceCode, dictCode.dictType)
        map["values"] = codes
        return map
    }

    private fun dictCodeConvertor(module: String, dictType: String): Set<String> =
        ServiceLoader.load(IDictItemCodeFinder::class.java).firstOrNull()
            ?.getDictItemCodes(module, dictType) ?: emptySet()
}
