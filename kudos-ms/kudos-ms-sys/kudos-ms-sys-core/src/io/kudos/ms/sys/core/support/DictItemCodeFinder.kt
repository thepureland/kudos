package io.kudos.ms.sys.core.support

import io.kudos.base.bean.validation.teminal.convert.converter.IDictItemCodeFinder
import io.kudos.context.kit.SpringKit
import io.kudos.ms.sys.core.cache.DictItemsByMsCodeAndTypeCache

/**
 * 字典项编码查找器
 *
 * @author K
 * @since 1.0.0
 */
class DictItemCodeFinder : IDictItemCodeFinder {

    // 该类通过java ServiceLoader 加载，所以不能用bean注入
    private var dictItemsByMsCodeAndTypeCache: DictItemsByMsCodeAndTypeCache? = null

    override fun getDictItemCodes(
        atomicServiceCode: String,
        dictType: String
    ): Set<String> {
        if (dictItemsByMsCodeAndTypeCache == null) {
            dictItemsByMsCodeAndTypeCache = SpringKit.getBean<DictItemsByMsCodeAndTypeCache>()
        }
        val items = dictItemsByMsCodeAndTypeCache!!.getDictItems(atomicServiceCode, dictType)
        return items.map { it.itemCode }.toSet()
    }

}