package io.kudos.ms.sys.core.support

import io.kudos.base.bean.validation.terminal.convert.converter.IDictItemCodeFinder
import io.kudos.context.kit.SpringKit
import io.kudos.ms.sys.core.cache.SysDictItemHashCache

/**
 * 字典项编码查找器
 *
 * @author K
 * @since 1.0.0
 */
class DictItemCodeFinder : IDictItemCodeFinder {

    // 该类通过java ServiceLoader 加载，所以不能用bean注入
    private var sysDictItemHashCache: SysDictItemHashCache? = null

    override fun getDictItemCodes(
        atomicServiceCode: String,
        dictType: String
    ): Set<String> {
        if (sysDictItemHashCache == null) {
            sysDictItemHashCache = SpringKit.getBean<SysDictItemHashCache>()
        }
        val cache = requireNotNull(sysDictItemHashCache) { "SysDictItemHashCache 未初始化" }
        val items = cache.getDictItems(atomicServiceCode, dictType)
        return items.map { it.itemCode }.toSet()
    }

}