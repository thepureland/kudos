package io.kudos.ms.sys.core.dict.service.impl

import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.core.dict.cache.SysDictItemHashCache
import io.kudos.ms.sys.core.dict.dao.SysDictItemDao
import io.kudos.ms.sys.core.dict.model.po.SysDictItem
import io.kudos.ms.sys.core.dict.service.iservice.ISysDictItemService
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
    dao: SysDictItemDao,
    private val sysDictItemHashCache: SysDictItemHashCache,
) : BaseCrudService<String, SysDictItem, SysDictItemDao>(dao), ISysDictItemService {

    private val log = LogFactory.getLog(this::class)

    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        return if (returnType == SysDictItemCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            sysDictItemHashCache.getDictItemById(id) as R?
        } else {
            super.get(id, returnType)
        }
    }

    override fun getDictItemFromCache(id: String): SysDictItemCacheEntry? = sysDictItemHashCache.getDictItemById(id)

    override fun getDictItemsFromCache(dictType: String, atomicServiceCode: String): List<SysDictItemCacheEntry> =
        sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)

    override fun batchGetDictItemsFromCache(
        dictTypesByAtomicServiceCode: Map<String, Collection<String>>
    ): Map<String, Map<String, List<SysDictItemCacheEntry>>> {
        return dictTypesByAtomicServiceCode.mapValues { (atomicServiceCode, dictTypes) ->
            dictTypes.associateWith { dictType ->
                sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
            }
        }
    }

    override fun getDictItemMapFromCache(dictType: String, atomicServiceCode: String): LinkedHashMap<String, String> =
        sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
            .associateTo(LinkedHashMap()) { it.itemCode to it.itemName }

    override fun batchGetDictItemMapFromCache(
        dictTypesByAtomicServiceCode: Map<String, Collection<String>>,
    ): Map<String, Map<String, LinkedHashMap<String, String>>> {
        return dictTypesByAtomicServiceCode.mapValues { (atomicServiceCode, dictTypes) ->
            dictTypes.associateWith { dictType ->
                sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
                    .associateTo(LinkedHashMap()) { it.itemCode to it.itemName }
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
        return completeCrudUpdate(
            success = dao.update(dictItem),
            log = log,
            successMessage = "更新id为${dictItemId}的字典项的启用状态为${active}。",
            failureMessage = "更新id为${dictItemId}的字典项的启用状态为${active}失败！",
        ) {
            sysDictItemHashCache.syncOnUpdate(dictItemId)
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "新增id为${id}的字典项。") {
            sysDictItemHashCache.syncOnInsert(any, id)
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireDictItemId(any)
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "更新id为${id}的字典项。",
            failureMessage = "更新id为${id}的字典项失败！",
        ) {
            sysDictItemHashCache.syncOnUpdate(id)
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        if (dao.get(id) == null) {
            log.warn("删除id为${id}的字典项时，发现其已不存在！")
            return false
        }
        val entry = sysDictItemHashCache.getDictItemById(id)
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "删除id为${id}的字典项。",
            failureMessage = "删除id为${id}的字典项失败！",
        ) {
            sysDictItemHashCache.syncOnDelete(
                id,
                entry?.atomicServiceCode ?: "",
                entry?.dictType,
                entry?.itemCode
            )
        }
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
        val parentId = list.firstOrNull()?.takeIf { it.isNotBlank() } ?: return
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
        return completeCrudUpdate(
            success = dao.update(dictItem),
            log = log,
            successMessage = "移动字典项${id}到父节点${newParentId}，排序号${newOrderNum}。",
            failureMessage = "移动字典项${id}失败！",
        ) {
            sysDictItemHashCache.syncOnUpdate(id)
        }
    }

    override fun getDirectChildrenOfDictFromCache(
        atomicServiceCode: String,
        dictType: String,
        activeOnly: Boolean
    ): List<SysDictItemCacheEntry> =
        sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
            .let { items -> if (activeOnly) items.filter { it.active } else items }
            .filter { it.parentId == null }

    override fun getDirectChildrenOfItemFromCache(
        atomicServiceCode: String,
        dictType: String,
        itemCode: String,
        activeOnly: Boolean
    ): List<SysDictItemCacheEntry> =
        sysDictItemHashCache.getDictItem(atomicServiceCode, dictType, itemCode)
            ?.let { getDirectChildrenOfItemFromCache(it.id, activeOnly) }
            ?: emptyList()

    override fun getDirectChildrenOfItemFromCache(parentId: String, activeOnly: Boolean): List<SysDictItemCacheEntry> =
        sysDictItemHashCache.getDictItems(parentId).let { items -> if (activeOnly) items.filter { it.active } else items }

    private fun requireDictItemId(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新字典项时不支持的入参类型: ${any::class.qualifiedName}")
}
