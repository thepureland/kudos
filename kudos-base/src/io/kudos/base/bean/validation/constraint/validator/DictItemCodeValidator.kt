package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.bean.validation.teminal.convert.converter.IDictItemCodeFinder
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.util.*

/**
 * 字典项编码校验器
 * 用于校验字段值是否为指定模块和类型的有效字典码
 *
 * @author K
 * @since 1.0.0
 */
class DictItemCodeValidator : ConstraintValidator<DictItemCode, CharSequence?> {
    private lateinit var dictItemCode: DictItemCode

    override fun initialize(dictItemCode: DictItemCode) {
        this.dictItemCode = dictItemCode
    }

    override fun isValid(value: CharSequence?, constraintValidatorContext: ConstraintValidatorContext): Boolean {
        if (value.isNullOrBlank()) {
            return true
        }
        val codes = dictCodeConvertor(dictItemCode.atomicServiceCode, dictItemCode.dictType)
        return codes.contains(value)
    }

    private fun dictCodeConvertor(module: String, dictType: String): Set<String> {
        val dictCodeFinders = ServiceLoader.load(IDictItemCodeFinder::class.java)
        for (dictCodeFinder in dictCodeFinders) {
            return dictCodeFinder.getDictItemCodes(module, dictType)
        }
        return emptySet()
    }

}
