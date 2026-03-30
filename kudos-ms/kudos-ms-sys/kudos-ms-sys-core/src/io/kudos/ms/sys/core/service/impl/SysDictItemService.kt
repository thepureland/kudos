package io.kudos.ms.sys.core.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheEntry
import io.kudos.ms.sys.core.cache.SysDictItemHashCache
import io.kudos.ms.sys.core.dao.SysDictItemDao
import io.kudos.ms.sys.core.model.po.SysDictItem
import io.kudos.ms.sys.core.service.iservice.ISysDictItemService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.LinkedHashMap
import kotlin.reflect.KClass


/**
 * 字典项业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class SysDictItemService(
    dao: SysDictItemDao
) : BaseCrudService<String, SysDictItem, SysDictItemDao>(dao), ISysDictItemService {

    @Autowired
    private lateinit var sysDictItemHashCache: SysDictItemHashCache

    private val log = LogFactory.getLog(this::class)

    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        return if (returnType == SysDictItemCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            sysDictItemHashCache.getDictItemById(id) as R?
        } else {
            super.get(id, returnType)
        }
    }

    override fun getDictItemFromCache(id: String): SysDictItemCacheEntry? {
        return sysDictItemHashCache.getDictItemById(id)
    }

    override fun getDictItemsFromCache(dictType: String, atomicServiceCode: String): List<SysDictItemCacheEntry> {
        return sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
    }

    override fun batchGetDictItemsFromCache(
        dictTypesByAtomicServiceCode: Map<String, Collection<String>>
    ): Map<String, Map<String, List<SysDictItemCacheEntry>>> {
        return dictTypesByAtomicServiceCode.mapValues { (atomicServiceCode, dictTypes) ->
            dictTypes.associateWith { dictType ->
                sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
            }
        }
    }

    override fun getDictItemMapFromCache(dictType: String, atomicServiceCode: String): LinkedHashMap<String, String> {
        val items = sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
        return items.associate { it.itemCode to it.itemName }.toMap(LinkedHashMap())
    }

    override fun batchGetDictItemMapFromCache(
        dictTypesByAtomicServiceCode: Map<String, Collection<String>>,
    ): Map<String, Map<String, LinkedHashMap<String, String>>> {
        return dictTypesByAtomicServiceCode.mapValues { (atomicServiceCode, dictTypes) ->
            dictTypes.associateWith { dictType ->
                val cacheItems = sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
                cacheItems.associate { it.itemCode to it.itemName }.toMap(LinkedHashMap())
            }
        }
    }

    override fun transDictItemNameFromCache(
        dictType: String,
        itemCode: String,
        atomicServiceCode: String
    ): String? {
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
            sysDictItemHashCache.syncOnBatchDelete(childItemIds)
        }
        val success = dao.deleteById(id)
        if (success) {
            sysDictItemHashCache.syncOnDelete(
                id,
                cacheEntry?.atomicServiceCode ?: "",
                cacheEntry?.dictType,
                cacheEntry?.itemCode
            )
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

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的字典项。")
        sysDictItemHashCache.syncOnInsert(any, id)
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, SysDictItem::id.name) as String
        if (success) {
            log.debug("更新id为${id}的字典项。")
            sysDictItemHashCache.syncOnUpdate(id)
        } else {
            log.error("更新id为${id}的字典项失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        if (dao.get(id) == null) {
            log.warn("删除id为${id}的字典项时，发现其已不存在！")
            return false
        }
        val entry = sysDictItemHashCache.getDictItemById(id)
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除id为${id}的字典项。")
            sysDictItemHashCache.syncOnDelete(
                id,
                entry?.atomicServiceCode ?: "",
                entry?.dictType,
                entry?.itemCode
            )
        } else {
            log.error("删除id为${id}的字典项失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除字典项，期望删除${ids.size}条，实际删除${count}条。")
        sysDictItemHashCache.syncOnBatchDelete(ids)
        return count
    }

    private fun recursionFindAllParentId(itemId: String, results: MutableList<String>) {
        val list = dao.oneSearchProperty(SysDictItem::id, itemId, SysDictItem::parentId)
        if (list.isEmpty()) return
        val parentId = list.first() as? String ?: return
        if (parentId.isBlank()) return
        results.add(parentId)
        recursionFindAllParentId(parentId, results)
    }

    private fun recursionFindAllChildId(itemId: String, results: MutableList<String>) {
        val itemIds = dao.oneSearchProperty(SysDictItem::parentId, itemId, SysDictItem::id)
        itemIds.forEach { childId ->
            results.add(childId)
            recursionFindAllChildId(childId, results)
        }
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

    override fun getDirectChildrenOfDictFromCache(
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

    override fun getDirectChildrenOfItemFromCache(
        atomicServiceCode: String,
        dictType: String,
        itemCode: String,
        activeOnly: Boolean
    ): List<SysDictItemCacheEntry> {
        val item = sysDictItemHashCache.getDictItem(atomicServiceCode, dictType, itemCode)
        return if (item != null) {
            getDirectChildrenOfItemFromCache(item.id, activeOnly)
        } else {
            emptyList()
        }
    }

    override fun getDirectChildrenOfItemFromCache(parentId: String, activeOnly: Boolean): List<SysDictItemCacheEntry> {
        var items = sysDictItemHashCache.getDictItems(parentId)
        if (activeOnly) {
            items = items.filter { it.active }
        }
        return items
    }

}
