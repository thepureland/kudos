package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.bean.validation.terminal.convert.converter.IDictItemCodeFinder
import java.util.LinkedHashMap
import java.util.ServiceLoader

/**
 * Dictionary-code constraint converter.
 * Converts dictionary-code annotations into front-end validation rules; supports loading dictionary data dynamically to generate the rules.
 */
class DictCodeConstraintConvertor(annotation: Annotation) : DefaultConstraintConvertor(annotation) {

    override fun getRule(constraintAnnotation: Annotation): LinkedHashMap<String, Any> {
        val map = super.getRule(constraintAnnotation)
        require(constraintAnnotation is DictItemCode) { "DictCodeConstraintConvertor only supports the DictCode annotation" }
        val dictCode = constraintAnnotation
        val codes = dictCodeConvertor(dictCode.atomicServiceCode, dictCode.dictType)
        map["values"] = codes
        return map
    }

    /**
     * Loads [IDictItemCodeFinder] via the [ServiceLoader] SPI and returns the dictionary code set from the first implementation.
     * Same reasoning as [DictItemCodeValidator.dictCodeConvertor]: use the SPI rather than a Spring bean — this class is a
     * bean-validation constraint converter and is instantiated reflectively by the framework.
     *
     * @param module the atomic-service code
     * @param dictType the dictionary type
     * @return the dictionary code set; an empty set if no implementation is available
     * @author K
     * @since 1.0.0
     */
    private fun dictCodeConvertor(module: String, dictType: String): Set<String> =
        ServiceLoader.load(IDictItemCodeFinder::class.java).firstOrNull()
            ?.getDictItemCodes(module, dictType) ?: emptySet()
}
