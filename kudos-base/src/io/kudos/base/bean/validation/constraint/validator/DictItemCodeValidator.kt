package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.bean.validation.terminal.convert.converter.IDictItemCodeFinder
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.util.ServiceLoader

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

    /**
     * 通过 [ServiceLoader] SPI 加载 [IDictItemCodeFinder] 实现，取首个非空结果。
     *
     * 之所以走 SPI 而非 Spring bean，是因为 [DictItemCodeValidator] 由 bean validation
     * 框架直接 new 出来，未经 Spring 容器；ServiceLoader 是这一层最低依赖的扩展点。
     * 找不到任何实现时返回空集合（让校验直接失败，等同"任何字典值都无效"）。
     *
     * @param module 原子服务编码（字典分区）
     * @param dictType 字典类型
     * @return 该字典的所有合法 code 集合
     * @author K
     * @since 1.0.0
     */
    private fun dictCodeConvertor(module: String, dictType: String): Set<String> {
        val dictCodeFinders = ServiceLoader.load(IDictItemCodeFinder::class.java)
        for (dictCodeFinder in dictCodeFinders) {
            return dictCodeFinder.getDictItemCodes(module, dictType)
        }
        return emptySet()
    }

}
