package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.terminal.convert.converter.IDictItemCodeFinder

/**
 * Test-only [IDictItemCodeFinder] SPI implementation, registered via
 * META-INF/services in test-resources, so that DictItemCode validation
 * can be tested deterministically without a real dictionary service.
 *
 * @author K
 * @since 1.0.0
 */
class TestDictItemCodeFinder : IDictItemCodeFinder {

    override fun getDictItemCodes(atomicServiceCode: String, dictType: String): Set<String> =
        if (atomicServiceCode == "test" && dictType == "test") {
            setOf("VALID_CODE", "ANOTHER_VALID_CODE")
        } else {
            emptySet()
        }

}
