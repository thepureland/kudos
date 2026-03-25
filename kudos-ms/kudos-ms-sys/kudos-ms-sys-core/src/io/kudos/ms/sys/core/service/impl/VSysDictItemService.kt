package io.kudos.ms.sys.core.service.impl

import io.kudos.base.support.service.impl.BaseReadOnlyService
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheEntry
import io.kudos.ms.sys.core.cache.SysDictItemHashCache
import io.kudos.ms.sys.core.dao.VSysDictItemDao
import io.kudos.ms.sys.core.model.po.VSysDictItem
import io.kudos.ms.sys.core.service.iservice.IVSysDictItemService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service

/**
 * 字典项视图（v_sys_dict_item）只读服务实现。
 *
 * 数据来源：视图 v_sys_dict_item，仅提供只读查询，无写操作。
 *
 * @author K
 * @since 1.0.0
 */
@Service
open class VSysDictItemService(
    dao: VSysDictItemDao
) : BaseReadOnlyService<String, VSysDictItem, VSysDictItemDao>(dao),
    IVSysDictItemService {

    @Resource
    private lateinit var sysDictItemHashCache: SysDictItemHashCache

    override fun fetchByAtomicServiceCodeAndDictTypeAndItemCode(
        atomicServiceCode: String,
        dictType: String,
        itemCode: String
    ): SysDictItemCacheEntry? {
        return sysDictItemHashCache.getDictItem(atomicServiceCode, dictType, itemCode)
    }

    override fun searchByAtomicServiceCodeAndDictType(
        atomicServiceCode: String,
        dictType: String
    ): List<SysDictItemCacheEntry> {
        return sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
    }

    override fun searchByParentId(parentId: String): List<SysDictItemCacheEntry> {
        return sysDictItemHashCache.getDictItems(parentId)
    }

    override fun getFromCache(id: String): SysDictItemCacheEntry? {
        return sysDictItemHashCache.getDictItemById(id)
    }

    override fun searchByIds(ids: Set<String>): Map<String, SysDictItemCacheEntry> {
        return sysDictItemHashCache.getDictItemsByIds(ids)
    }

}
