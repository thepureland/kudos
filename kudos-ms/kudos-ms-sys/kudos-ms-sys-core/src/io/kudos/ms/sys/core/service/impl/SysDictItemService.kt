package io.kudos.ms.sys.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheEntry
import io.kudos.ms.sys.core.cache.SysDictItemHashCache
import io.kudos.ms.sys.core.dao.SysDictItemDao
import io.kudos.ms.sys.core.model.po.SysDictItem
import io.kudos.ms.sys.core.service.iservice.ISysDictItemService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 字典项业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysDictItemService : BaseCrudService<String, SysDictItem, SysDictItemDao>(), ISysDictItemService {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var sysDictItemHashCache: SysDictItemHashCache

    private val log = LogFactory.getLog(this)

    override fun getItems(dictType: String, atomicServiceCode: String): List<SysDictItemCacheEntry> {
        return sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
    }

    override fun batchGetDictItems(
        dictTypesByAtomicServiceCode: Map<String, Collection<String>>
    ): Map<String, Map<String, List<SysDictItemCacheEntry>>> {
        return dictTypesByAtomicServiceCode.mapValues { (atomicServiceCode, dictTypes) ->
            dictTypes.associateWith { dictType ->
                sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
            }
        }
    }

    override fun getItemMap(dictType: String, atomicServiceCode: String): LinkedHashMap<String, String> {
        val items = sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
        return items.associate { it.itemCode to it.itemName }.toMap(LinkedHashMap())
    }

    override fun batchGetDictItemMap(
        dictTypesByAtomicServiceCode: Map<String, Collection<String>>,
    ): Map<String, Map<String, LinkedHashMap<String, String>>> {
        return dictTypesByAtomicServiceCode.mapValues { (atomicServiceCode, dictTypes) ->
            dictTypes.associateWith { dictType ->
                val cacheItems = sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
                cacheItems.associate { it.itemCode to it.itemName }.toMap(LinkedHashMap())
            }
        }
    }

    override fun transDictCode(dictType: String, itemCode: String, atomicServiceCode: String): String? {
        val items = sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
        return items.firstOrNull { it.itemCode == itemCode }?.itemName
    }

    override fun fetchAllParentIds(itemId: String): List<String> {
        val results = mutableListOf<String>()
        recursionFindAllParentId(itemId, results)
        results.reverse()
        return results
    }

    @Transactional
    override fun cascadeDeleteChildren(id: String): Boolean {
        val cacheEntry = sysDictItemHashCache.getDictItemById(id)
        val childItemIds = mutableListOf<String>()
        recursionFindAllChildId(id, childItemIds)
        if (childItemIds.isNotEmpty()) {
            dao.batchDelete(childItemIds)
        }
        val success = dao.deleteById(id)
        if (success) {
            sysDictItemHashCache.syncOnDelete(
                id,
                cacheEntry?.atomicServiceCode ?: "",
                cacheEntry?.dictType,
                cacheEntry?.itemCode
            ) // 同步缓存
        } else {
            log.error("删除id为${id}的字典项失败！")
        }
        return success
    }

    @Transactional
    override fun updateActive(dictItemId: String, active: Boolean): Boolean {
        val dictItem = SysDictItem {
            this.id = dictItemId
            this.active = active
        }
        val success = dao.update(dictItem)
        if (success) {
            log.debug("更新id为${dictItemId}的字典项的启用状态为${active}。")
            sysDictItemHashCache.syncOnUpdate(dictItemId)
        } else {
            log.error("更新id为${dictItemId}的字典项的启用状态为${active}失败！")
        }
        return success
    }

    private fun recursionFindAllParentId(itemId: String, results: MutableList<String>) {
        val list = dao.oneSearchProperty(SysDictItem::id, itemId, SysDictItem::parentId)
        if (list.isNotEmpty()) {
            val parentId = list.first() as String
            results.add(parentId)
            recursionFindAllParentId(parentId, results)
        }
    }

    private fun recursionFindAllChildId(itemId: String, results: MutableList<String>) {
        val itemIds = dao.oneSearchProperty(SysDictItem::parentId, itemId, SysDictItem::id)
        itemIds.forEach { id ->
            results.add(id)
            recursionFindAllChildId(id, results)
        }
    }

    override fun getDictItemsByAtomicServiceAndType(
        dictType: String,
        atomicServiceCode: String
    ): List<SysDictItemCacheEntry> {
        return getItems(dictType, atomicServiceCode)
    }

    @Transactional
    override fun moveItem(id: String, newParentId: String?, newOrderNum: Int?): Boolean {
        val dictItem = SysDictItem {
            this.id = id
            this.parentId = newParentId
            this.orderNum = newOrderNum
        }
        val success = dao.update(dictItem)
        if (success) {
            log.debug("移动字典项${id}到父节点${newParentId}，排序号${newOrderNum}。")
            sysDictItemHashCache.syncOnUpdate(id)
        } else {
            log.error("移动字典项${id}失败！")
        }
        return success
    }

    override fun getDirectChildrenOfDict(
        atomicServiceCode: String,
        dictType: String,
        activeOnly: Boolean
    ): List<SysDictItemCacheEntry> {
        var items = sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
        if (activeOnly) {
            items = items.filter { it.active }
        }
        return items.filter { it.parentId == null }
    }

    override fun getDirectChildrenOfItem(
        atomicServiceCode: String,
        dictType: String,
        itemCode: String,
        activeOnly: Boolean
    ): List<SysDictItemCacheEntry> {
        val item = sysDictItemHashCache.getDictItem(atomicServiceCode, dictType, itemCode)
        return if (item != null) {
            getDirectChildrenOfItem(item.id, activeOnly)
        } else emptyList()
    }

    override fun getDirectChildrenOfItem(parentId: String, activeOnly: Boolean): List<SysDictItemCacheEntry> {
        var items = sysDictItemHashCache.getDictItems(parentId)
        if (activeOnly) {
            items = items.filter { it.active }
        }
        return items
    }

    //endregion your codes 2

}