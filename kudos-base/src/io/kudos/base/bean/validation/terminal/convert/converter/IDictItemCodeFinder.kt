package io.kudos.base.bean.validation.terminal.convert.converter

/**
 * Dictionary item code finder interface.
 *
 * @author K
 * @since 1.0.0
 */
interface IDictItemCodeFinder {

    /**
     * @param atomicServiceCode the microservice code
     * @param dictType the dictionary type
     */
    fun getDictItemCodes(atomicServiceCode: String, dictType: String): Set<String>

}
