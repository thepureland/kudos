package io.kudos.base.bean.validation.teminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.DictCode
import io.kudos.base.bean.validation.teminal.convert.converter.IDictCodeFinder
import java.util.*

/**
 * 字典码约束转换器
 * 用于将字典码注解转换为前端校验规则，支持动态获取字典数据并生成校验规则
 */
class DictCodeConstraintConvertor(annotation: Annotation) : DefaultConstraintConvertor(annotation) {

    override fun getRule(constraintAnnotation: Annotation): LinkedHashMap<String, Any> {
        val map = super.getRule(constraintAnnotation)
        val dictCode = constraintAnnotation as DictCode
        val codeMap = dictCodeConvertor(dictCode.module, dictCode.dictType)
        map.put("values", codeMap.keys)
        return map
    }

    private fun dictCodeConvertor(module: String?, dictType: String?): MutableMap<String?, String?> {
        val dictCodeFinders = ServiceLoader.load(IDictCodeFinder::class.java)
        for (dictCodeFinder in dictCodeFinders) {
            return dictCodeFinder.getDictData(module, dictType)
        }
        return HashMap<String?, String?>()
    }
}
