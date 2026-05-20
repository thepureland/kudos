package io.kudos.ms.sys.core.dict.service.impl

import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.platform.service.impl.requireStringId
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.common.dict.vo.response.SysDictItemNode
import io.kudos.ms.sys.core.dict.cache.SysDictItemHashCache
import io.kudos.ms.sys.core.dict.dao.SysDictItemDao
import io.kudos.ms.sys.core.dict.event.SysDictItemBatchDeleted
import io.kudos.ms.sys.core.dict.event.SysDictItemDeleted
import io.kudos.ms.sys.core.dict.event.SysDictItemInserted
import io.kudos.ms.sys.core.dict.event.SysDictItemUpdated
import io.kudos.ms.sys.core.dict.model.po.SysDictItem
import io.kudos.ms.sys.core.dict.service.iservice.ISysDictItemService
import org.springframework.context.ApplicationEventPublisher
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
    private val eventPublisher: ApplicationEventPublisher,
) : BaseCrudService<String, SysDictItem, SysDictItemDao>(dao), ISysDictItemService {

    private val log = LogFactory.getLog(this::class)

    @Suppress("UNCHECKED_CAST")
    @Transactional(readOnly = true)
    override fun <R : Any> get(id: String, returnType: KClass<R>): R? =
        if (returnType == SysDictItemCacheEntry::class) sysDictItemHashCache.getDictItemById(id) as R?
        else super.get(id, returnType)

    @Transactional(readOnly = true)
    override fun getDictItemFromCache(id: String): SysDictItemCacheEntry? = sysDictItemHashCache.getDictItemById(id)

    @Transactional(readOnly = true)
    override fun getDictItemsFromCache(dictType: String, atomicServiceCode: String): List<SysDictItemCacheEntry> =
        sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)

    @Transactional(readOnly = true)
    override fun batchGetDictItemsFromCache(
        dictTypesByAtomicServiceCode: Map<String, Collection<String>>
    ): Map<String, Map<String, List<SysDictItemCacheEntry>>> =
        dictTypesByAtomicServiceCode.mapValues { (atomicServiceCode, dictTypes) ->
            dictTypes.associateWith { dictType ->
                sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
            }
        }

    @Transactional(readOnly = true)
    override fun getDictItemMapFromCache(dictType: String, atomicServiceCode: String): LinkedHashMap<String, String> =
        sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
            .associateTo(LinkedHashMap()) { it.itemCode to it.itemName }

    @Transactional(readOnly = true)
    override fun batchGetDictItemMapFromCache(
        dictTypesByAtomicServiceCode: Map<String, Collection<String>>,
    ): Map<String, Map<String, LinkedHashMap<String, String>>> =
        dictTypesByAtomicServiceCode.mapValues { (atomicServiceCode, dictTypes) ->
            dictTypes.associateWith { dictType ->
                sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
                    .associateTo(LinkedHashMap()) { it.itemCode to it.itemName }
            }
        }

    @Transactional(readOnly = true)
    override fun transDictItemNameFromCache(
        dictType: String,
        itemCode: String,
        atomicServiceCode: String
    ): String? =
        sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
            .firstOrNull { it.itemCode == itemCode }?.itemName

    @Transactional(readOnly = true)
    override fun fetchAllParentIds(itemId: String): List<String> {
        val results = mutableListOf<String>()
        recursionFindAllParentId(itemId, results)
        return results.apply { reverse() }
    }

    @Transactional
    override fun cascadeDeleteChildren(id: String): Boolean {
        val cacheEntry = sysDictItemHashCache.getDictItemById(id)
        val childItemIds = mutableListOf<String>()
        recursionFindAllChildId(id, childItemIds)
        if (childItemIds.isNotEmpty()) {
            dao.batchDelete(childItemIds)
            eventPublisher.publishEvent(SysDictItemBatchDeleted(ids = childItemIds))
        }
        val success = dao.deleteById(id)
        if (success) {
            eventPublisher.publishEvent(
                SysDictItemDeleted(
                    id = id,
                    atomicServiceCode = cacheEntry?.atomicServiceCode ?: "",
                    dictType = cacheEntry?.dictType,
                    itemCode = cacheEntry?.itemCode,
                )
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
            eventPublisher.publishEvent(SysDictItemUpdated(id = dictItemId))
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "新增id为${id}的字典项。") {
            eventPublisher.publishEvent(SysDictItemInserted(id = id))
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireStringId(any, "字典项")
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "更新id为${id}的字典项。",
            failureMessage = "更新id为${id}的字典项失败！",
        ) {
            eventPublisher.publishEvent(SysDictItemUpdated(id = id))
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
            eventPublisher.publishEvent(
                SysDictItemDeleted(
                    id = id,
                    atomicServiceCode = entry?.atomicServiceCode ?: "",
                    dictType = entry?.dictType,
                    itemCode = entry?.itemCode,
                )
            )
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除字典项，期望删除${ids.size}条，实际删除${count}条。")
        if (count > 0) {
            eventPublisher.publishEvent(SysDictItemBatchDeleted(ids = ids))
        }
        return count
    }

    /**
     * 沿 parentId 自下而上递归收集所有祖先 id；链上某节点 parentId 为空/blank 即终止。
     *
     * **N+1 警告**：每层都走一次 DB 查询；当前业务字典树通常很浅 (<5 层)，可接受。
     * 若后续遇到深树，应一次性 load 整张字典再在内存里构图。
     *
     * @param itemId 起点字典项 id
     * @param results 累积容器（追加 parentId）
     * @author K
     * @since 1.0.0
     */
    private fun recursionFindAllParentId(itemId: String, results: MutableList<String>) {
        val list = dao.oneSearchProperty(SysDictItem::id, itemId, SysDictItem::parentId)
        val parentId = list.firstOrNull()?.takeIf { it.isNotBlank() } ?: return
        results.add(parentId)
        recursionFindAllParentId(parentId, results)
    }

    /**
     * 沿 parentId 自上而下递归收集所有后代 id；同样有 N+1 风险，详见 [recursionFindAllParentId]。
     *
     * @param itemId 起点字典项 id
     * @param results 累积容器（追加 childId）
     * @author K
     * @since 1.0.0
     */
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
            eventPublisher.publishEvent(SysDictItemUpdated(id = id))
        }
    }

    @Transactional(readOnly = true)
    override fun getDirectChildrenOfDictFromCache(
        atomicServiceCode: String,
        dictType: String,
        activeOnly: Boolean
    ): List<SysDictItemCacheEntry> =
        sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
            .let { items -> if (activeOnly) items.filter { it.active } else items }
            .filter { it.parentId == null }

    @Transactional(readOnly = true)
    override fun getDirectChildrenOfItemFromCache(
        atomicServiceCode: String,
        dictType: String,
        itemCode: String,
        activeOnly: Boolean
    ): List<SysDictItemCacheEntry> =
        sysDictItemHashCache.getDictItem(atomicServiceCode, dictType, itemCode)
            ?.let { getDirectChildrenOfItemFromCache(it.id, activeOnly) }
            ?: emptyList()

    @Transactional(readOnly = true)
    override fun getDirectChildrenOfItemFromCache(parentId: String, activeOnly: Boolean): List<SysDictItemCacheEntry> =
        sysDictItemHashCache.getDictItems(parentId).let { items -> if (activeOnly) items.filter { it.active } else items }

    @Transactional(readOnly = true)
    override fun getDirectChildrenOfDictAsNodes(
        atomicServiceCode: String,
        dictType: String,
        activeOnly: Boolean,
    ): List<SysDictItemNode> =
        getDirectChildrenOfDictFromCache(atomicServiceCode, dictType, activeOnly)
            .map { SysDictItemNode(it.id, it.itemCode, it.itemName) }

    @Transactional(readOnly = true)
    override fun getDirectChildrenOfItemAsNodes(
        atomicServiceCode: String,
        dictType: String,
        itemCode: String,
        activeOnly: Boolean,
    ): List<SysDictItemNode> =
        getDirectChildrenOfItemFromCache(atomicServiceCode, dictType, itemCode, activeOnly)
            .map { SysDictItemNode(it.id, it.itemCode, it.itemName) }

}
