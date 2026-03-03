package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysDictItemApi
import io.kudos.ms.sys.common.vo.dict.SysDictPayload
import io.kudos.ms.sys.common.vo.dict.SysDictTreeNode
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheItem
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemRecord
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemSearchPayload
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemTreeRecord
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

    override fun get(id: String, fetchAllParentIds: Boolean): SysDictItemRecord? {
        return sysDictItemService.get(id, fetchAllParentIds)
    }

    override fun getItemsFromCache(atomicServiceCode: String?, type: String): List<SysDictItemCacheItem> {
        return sysDictItemService.getItemsFromCache(atomicServiceCode, type)
    }

    override fun transDictCode(module: String, type: String, code: String): String? {
        return sysDictItemService.transDictCode(module, type, code)
    }

    override fun saveOrUpdate(payload: SysDictPayload): String {
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

    override fun loadDirectChildrenForList(searchPayload: SysDictItemSearchPayload): Pair<List<SysDictItemRecord>, Int> {
        return sysDictItemService.loadDirectChildrenForList(searchPayload)
    }

    override fun updateActive(dictItemId: String, active: Boolean): Boolean {
        return sysDictItemService.updateActive(dictItemId, active)
    }

    override fun getDictItemsByDictId(dictId: String): List<SysDictItemRecord> {
        return sysDictItemService.getDictItemsByDictId(dictId)
    }

    override fun getDictItemsByAtomicServiceAndType(atomicServiceCode: String, dictType: String): List<SysDictItemCacheItem> {
        return sysDictItemService.getDictItemsByAtomicServiceAndType(atomicServiceCode, dictType)
    }

    override fun getDictItemTree(dictId: String, parentId: String?): List<SysDictItemTreeRecord> {
        return sysDictItemService.getDictItemTree(dictId, parentId)
    }

    override fun getChildItems(parentId: String): List<SysDictItemRecord> {
        return sysDictItemService.getChildItems(parentId)
    }

    override fun moveItem(id: String, newParentId: String?, newOrderNum: Int?): Boolean {
        return sysDictItemService.moveItem(id, newParentId, newOrderNum)
    }

    //endregion your codes 2

}