package io.kudos.ms.sys.core.dict.support

import io.kudos.base.bean.validation.terminal.convert.converter.IDictItemCodeFinder
import io.kudos.context.kit.SpringKit
import io.kudos.ms.sys.core.dict.cache.SysDictItemHashCache

/**
 * Dictionary item code finder.
 *
 * @author K
 * @since 1.0.0
 */
class DictItemCodeFinder : IDictItemCodeFinder {

    // This class is loaded via Java ServiceLoader, so it cannot use bean injection.
    private var sysDictItemHashCache: SysDictItemHashCache? = null

    override fun getDictItemCodes(
        atomicServiceCode: String,
        dictType: String
    ): Set<String> {
        if (sysDictItemHashCache == null) {
            sysDictItemHashCache = SpringKit.getBean<SysDictItemHashCache>()
        }
        val cache = requireNotNull(sysDictItemHashCache) { "SysDictItemHashCache is not initialized" }
        val items = cache.getDictItems(atomicServiceCode, dictType)
        return items.map { it.itemCode }.toSet()
    }

}