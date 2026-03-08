package io.kudos.base.bean.validation.teminal.convert.converter

/**
 * 字典项编码查找器接口
 *
 * @author K
 * @since 1.0.0
 */
interface IDictItemCodeFinder {

    /**
     * @param atomicServiceCode 微服务编码
     * @param dictType 字典类型
     */
    fun getDictItemCodes(atomicServiceCode: String, dictType: String): Set<String>

}
