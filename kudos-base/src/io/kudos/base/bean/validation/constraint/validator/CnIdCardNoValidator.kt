package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.CnIdCardNo
import io.kudos.base.cn.IdCardNoKit
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/**
 * CnIdCardNo约束验证器
 *
 * @author K
 * @since 1.0.0
 */
class CnIdCardNoValidator : ConstraintValidator<CnIdCardNo, CharSequence?> {
    private lateinit var cnIdCardNo: CnIdCardNo

    override fun initialize(cnIdCardNo: CnIdCardNo) {
        this.cnIdCardNo = cnIdCardNo
    }

    override fun isValid(value: CharSequence?, constraintValidatorContext: ConstraintValidatorContext?): Boolean {
        if (value == null) {
            return true
        }
        return if (cnIdCardNo.support15) IdCardNoKit.isIdCardNo(value) else IdCardNoKit.isIdCardNo18(value)
    }
}