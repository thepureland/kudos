package io.kudos.ms.sys.core.dict.service.impl
import io.kudos.base.support.service.impl.BaseReadOnlyService
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.core.dict.cache.SysDictItemHashCache
import io.kudos.ms.sys.core.dict.dao.VSysDictItemDao
import io.kudos.ms.sys.core.dict.model.po.VSysDictItem
import io.kudos.ms.sys.core.dict.service.iservice.IVSysDictItemService
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
    dao: VSysDictItemDao,
    private val sysDictItemHashCache: SysDictItemHashCache,
) : BaseReadOnlyService<String, VSysDictItem, VSysDictItemDao>(dao),
    IVSysDictItemService {

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
