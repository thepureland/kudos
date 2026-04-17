package io.kudos.ms.sys.common.platform.validation

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.context.kit.SpringKit
import io.kudos.ms.sys.common.dict.api.ISysDictApi
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext


/**
 * DictCode约束验证器
 *
 * @author K
 * @since 1.0.0
 */
class DictCodeValidator : ConstraintValidator<DictItemCode, CharSequence?> {

    private lateinit var dictItemCode: DictItemCode

    private lateinit var dictApi: ISysDictApi

    override fun initialize(dictItemCode: DictItemCode) {
        this.dictItemCode = dictItemCode
        dictApi = SpringKit.getBean<ISysDictApi>()
    }

    override fun isValid(value: CharSequence?, context: ConstraintValidatorContext): Boolean {
        if (value == null) {
            return true
        }
        val dictItems = dictApi.getActiveDictItems(dictItemCode.dictType, dictItemCode.atomicServiceCode)
        val itemCodes = dictItems.map { it.itemCode }
        return itemCodes.contains(value)
    }

}