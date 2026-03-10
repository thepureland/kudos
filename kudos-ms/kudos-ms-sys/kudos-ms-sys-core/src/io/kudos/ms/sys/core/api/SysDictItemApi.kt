package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysDictItemApi
import io.kudos.ms.sys.common.vo.dict.SysDictForm
import io.kudos.ms.sys.common.vo.dict.SysDictTreeNode
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheEntry
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemRow
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemQuery
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemTreeRow
import io.kudos.ms.sys.core.service.iservice.ISysDictItemService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service


/**
 * 字典项 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@Service
open class SysDictItemApi : ISysDictItemApi {
//endregion your codes 1

    //region your codes 2

    @Resource
    protected lateinit var sysDictItemService: ISysDictItemService

    override fun get(id: String, fetchAllParentIds: Boolean): SysDictItemRow? {
        return sysDictItemService.get(id, fetchAllParentIds)
    }

    override fun getItemsFromCache(type: String, atomicServiceCode: String): List<SysDictItemCacheEntry> {
        return sysDictItemService.getItems(type, atomicServiceCode)
    }

    override fun transDictCode(dictType: String, itemCode: String, atomicServiceCode: String): String? {
        return sysDictItemService.transDictCode(dictType, itemCode, atomicServiceCode)
    }

    override fun saveOrUpdate(payload: SysDictForm): String {
        return sysDictItemService.saveOrUpdate(payload)
    }

    override fun fetchAllParentIds(itemId: String): List<String> {
        return sysDictItemService.fetchAllParentIds(itemId)
    }

    override fun cascadeDeleteChildren(id: String): Boolean {
        return sysDictItemService.cascadeDeleteChildren(id)
    }

    override fun loadDirectChildrenForTree(parent: String?, isModule: Boolean, activeOnly: Boolean): List<SysDictTreeNode> {
        return sysDictItemService.loadDirectChildrenForTree(parent, isModule, activeOnly)
    }

    override fun loadDirectChildrenForList(searchPayload: SysDictItemQuery): Pair<List<SysDictItemRow>, Int> {
        return sysDictItemService.loadDirectChildrenForList(searchPayload)
    }

    override fun updateActive(dictItemId: String, active: Boolean): Boolean {
        return sysDictItemService.updateActive(dictItemId, active)
    }

    override fun getDictItemsByDictId(dictId: String): List<SysDictItemRow> {
        return sysDictItemService.getDictItemsByDictId(dictId)
    }

    override fun getDictItemsByAtomicServiceAndType(dictType: String, atomicServiceCode: String): List<SysDictItemCacheEntry> {
        return sysDictItemService.getDictItemsByAtomicServiceAndType(dictType, atomicServiceCode)
    }

    override fun getDictItemTree(dictId: String, parentId: String?): List<SysDictItemTreeRow> {
        return sysDictItemService.getDictItemTree(dictId, parentId)
    }

    override fun getChildItems(parentId: String): List<SysDictItemRow> {
        return sysDictItemService.getChildItems(parentId)
    }

    override fun moveItem(id: String, newParentId: String?, newOrderNum: Int?): Boolean {
        return sysDictItemService.moveItem(id, newParentId, newOrderNum)
    }

    //endregion your codes 2

}