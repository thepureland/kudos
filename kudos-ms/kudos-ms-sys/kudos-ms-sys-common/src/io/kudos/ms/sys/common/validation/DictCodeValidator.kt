package io.kudos.ms.sys.common.validation

import io.kudos.base.bean.validation.constraint.annotations.DictCode
import io.kudos.context.kit.SpringKit
import io.kudos.ms.sys.common.api.ISysDictApi
import io.kudos.ms.sys.common.vo.dict.DictTypeAndASCodePayload
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext


/**
 * DictCode约束验证器
 *
 * @author K
 * @since 1.0.0
 */
class DictCodeValidator : ConstraintValidator<DictCode, CharSequence?> {

    private lateinit var dictCode: DictCode

    private lateinit var dictApi: ISysDictApi

    override fun initialize(dictCode: DictCode) {
        this.dictCode = dictCode
        dictApi = SpringKit.getBean<ISysDictApi>()
    }

    override fun isValid(value: CharSequence?, context: ConstraintValidatorContext): Boolean {
        if (value == null) {
            return true
        }
        val dictItems = dictApi.getDictItems(DictTypeAndASCodePayload(dictCode.dictType, dictCode.atomicServiceCode))
        val itemCodes = dictItems.map { it.itemCode!! }
        return itemCodes.contains(value)
    }

}