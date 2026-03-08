package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.DictEnumItemCode
import io.kudos.base.lang.EnumKit
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/**
 * DictEnumCode约束验证器
 *
 * @author K
 * @since 1.0.0
 */
class DictEnumCodeValidator : ConstraintValidator<DictEnumItemCode, CharSequence?> {
    private lateinit var dictEnumItemCode: DictEnumItemCode

    override fun initialize(dictEnumItemCode: DictEnumItemCode) {
        this.dictEnumItemCode = dictEnumItemCode
    }

    override fun isValid(value: CharSequence?, constraintValidatorContext: ConstraintValidatorContext?): Boolean {
        if (value == null) {
            return true
        }
        val dictMap = EnumKit.getCodeMap(dictEnumItemCode.enumClass)
        return dictMap.containsKey(value)
    }
}
