package io.kudos.base.bean.validation.teminal.convert.converter

/**
 * 字典查找器
 */
interface IDictCodeFinder {
    /**
     * @param module
     * @param dictType
     */
    fun getDictData(module: String?, dictType: String?): MutableMap<String?, String?>
}
