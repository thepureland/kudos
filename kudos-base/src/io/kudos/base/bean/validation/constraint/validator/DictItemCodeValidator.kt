package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.bean.validation.terminal.convert.converter.IDictItemCodeFinder
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.util.ServiceLoader

/**
 * Validator for dictionary item codes.
 * Used to validate whether a field value is a valid dictionary code for the specified module and dictionary type.
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
     * Loads [IDictItemCodeFinder] implementations via [ServiceLoader] SPI and returns the first non-empty result.
     *
     * SPI is used instead of Spring beans because [DictItemCodeValidator] is instantiated directly by the
     * bean validation framework without going through the Spring container; ServiceLoader is the lowest-dependency
     * extension point available at this layer. Returns an empty set if no implementation is found
     * (letting validation fail outright, equivalent to "no dictionary value is valid").
     *
     * @param module atomic service code (dictionary partition)
     * @param dictType dictionary type
     * @return the set of all valid codes for this dictionary
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
