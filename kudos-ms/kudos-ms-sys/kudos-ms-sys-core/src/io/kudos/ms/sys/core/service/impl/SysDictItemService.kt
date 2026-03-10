package io.kudos.ms.sys.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.PagingSearchResult
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.ms.sys.common.vo.dict.SysDictForm
import io.kudos.ms.sys.common.vo.dict.SysDictTreeNode
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheEntry
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemRow
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemQuery
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemTreeRow
import io.kudos.ms.sys.core.cache.DictItemsByMsCodeAndTypeCache
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
    private lateinit var dictItemCacheHandler: DictItemsByMsCodeAndTypeCache

    private val log = LogFactory.getLog(this)

    override fun get(id: String, fetchAllParentIds: Boolean): SysDictItemRow? {
        val searchPayload = SysDictItemQuery(id = id).apply {
            pageSize = 1
        }
        val result = dao.pagingSearch(searchPayload).firstOrNull()
        if (result != null && fetchAllParentIds) {
            val parentId = result.parentId
            if (!parentId.isNullOrBlank()) {
                var parentIds = fetchAllParentIds(parentId)
                parentIds = parentIds.toMutableList()
                parentIds.add(parentId)
                result.parentIds = parentIds
            }
        }
        return result
    }


    override fun getItems(dictType: String, atomicServiceCode: String): List<SysDictItemCacheEntry> {
        return dictItemCacheHandler.getDictItems(dictType, atomicServiceCode)
    }

    override fun batchGetDictItems(
        dictTypesByAtomicServiceCode: Map<String, Collection<String>>
    ): Map<String, Map<String, List<SysDictItemCacheEntry>>> {
        return dictTypesByAtomicServiceCode.mapValues { (atomicServiceCode, dictTypes) ->
            dictTypes.associateWith { dictType ->
                dictItemCacheHandler.getDictItems(dictType, atomicServiceCode)
            }
        }
    }

    override fun getItemMap(dictType: String, atomicServiceCode: String): LinkedHashMap<String, String> {
        val items = dictItemCacheHandler.getDictItems(dictType, atomicServiceCode)
        return items.associate { it.itemCode to it.itemName }.toMap(LinkedHashMap())
    }

    override fun batchGetDictItemMap(
        dictTypesByAtomicServiceCode: Map<String, Collection<String>>,
    ): Map<String, Map<String, LinkedHashMap<String, String>>> {
        return dictTypesByAtomicServiceCode.mapValues { (atomicServiceCode, dictTypes) ->
            dictTypes.associateWith { dictType ->
                val cacheItems = dictItemCacheHandler.getDictItems(atomicServiceCode, dictType)
                cacheItems.associate { it.itemCode to it.itemName }.toMap(LinkedHashMap())
            }
        }
    }

    override fun transDictCode(dictType: String, itemCode: String, atomicServiceCode: String): String? {
        val items = dictItemCacheHandler.getDictItems(dictType, atomicServiceCode)
        return items.firstOrNull { it.itemCode == itemCode }?.itemName
    }

    @Transactional
    override fun saveOrUpdate(payload: SysDictForm): String {
        return if (payload.id.isBlank()) { // 新增
            val payloadCode = requireNotNull(payload.code) { "新增字典项时，code不能为空。" }
            val payloadName = requireNotNull(payload.name) { "新增字典项时，name不能为空。" }
            val sysDictItem = SysDictItem().apply {
                dictId = payload.id
                parentId = payload.parentId
                itemCode = payloadCode
                itemName = payloadName
                orderNum = payload.seqNo
                remark = payload.remark
            }
            val id = dao.insert(sysDictItem)
            dictItemCacheHandler.syncOnInsert(sysDictItem, id) // 同步缓存
            id
        } else { // 更新
            val payloadCode = requireNotNull(payload.code) { "更新字典项时，code不能为空。" }
            val payloadName = requireNotNull(payload.name) { "更新字典项时，name不能为空。" }
            val sysDictItem = SysDictItem {
                id = payload.id
                dictId = payload.id
                parentId = payload.parentId
                itemCode = payloadCode
                itemName = payloadName
                orderNum = payload.seqNo
                remark = payload.remark
            }
            val success = dao.update(sysDictItem)
            if (success) {
                dictItemCacheHandler.syncOnUpdate(sysDictItem, sysDictItem.id) // 同步缓存
            } else {
                log.error("新增id为${sysDictItem.id}的字典项失败！")
            }
            sysDictItem.id
        }
    }

    override fun pagingSearch(listSearchPayload: ListSearchPayload): PagingSearchResult<SysDictItemRow> {
        val dictItems = dao.pagingSearch(listSearchPayload as SysDictItemQuery)
        val totalCount = if (dictItems.isNotEmpty()) {
            // 查询parentCode
            val parentIds = dictItems.filter { !it.parentId.isNullOrBlank() }.mapNotNull { it.parentId }.toSet()
            val criteria = Criteria.of(SysDictItem::id.name, OperatorEnum.IN, parentIds)
            val idAndCodeMaps = dao.searchProperties(criteria, listOf(SysDictItem::id, SysDictItem::itemCode))
            dictItems.forEach { dictItem ->
                val idAndCodeMap = idAndCodeMaps.singleOrNull { it[SysDictItem::id.name] == dictItem.parentId }
                if (idAndCodeMap != null) {
                    dictItem.parentCode = idAndCodeMap[SysDictItem::itemCode.name] as String?
                }
            }
            dao.count(listSearchPayload)
        } else 0
        return PagingSearchResult(dictItems, totalCount)
    }

    override fun fetchAllParentIds(itemId: String): List<String> {
        val results = mutableListOf<String>()
        recursionFindAllParentId(itemId, results)
        results.reverse()
        return results
    }

    @Transactional
    override fun cascadeDeleteChildren(id: String): Boolean {
        val dictItem = requireNotNull(dao.get(id)) { "删除字典项失败：id=${id}不存在。" }
        val childItemIds = mutableListOf<String>()
        recursionFindAllChildId(id, childItemIds)
        if (childItemIds.isNotEmpty()) {
            dao.batchDelete(childItemIds)
        }
        val success = dao.deleteById(id)
        if (success) {
            dictItemCacheHandler.syncOnDelete(id, dictItem.dictId) // 同步缓存
        } else {
            log.error("删除id为${id}的字典项失败！")
        }
        return success
    }

    override fun loadDirectChildrenForTree(
        parent: String?,
        isModule: Boolean,
        activeOnly: Boolean
    ): List<SysDictTreeNode> {
        return when {
            parent.isNullOrBlank() -> { // 加载模块列表
                val items = getItems("kuark:sys", "module")
                items.map {
                    SysDictTreeNode().apply {
                        code = it.itemCode
                        id = code ?: ""
                    }
                }
            }

            isModule -> { // 加载SysDict数据
                dao.searchDictNodesByAtomicServiceCode(parent)
            }

            else -> { // 加载SysDictItem数据
                dao.searchDirectChildrenNodes(parent, activeOnly)
            }
        }
    }

    override fun loadDirectChildrenForList(searchPayload: SysDictItemQuery): Pair<List<SysDictItemRow>, Int> {
        val activeOnly = searchPayload.active ?: false // 是否只加载启用状态的数据, 默认为是
        searchPayload.active = if (activeOnly) true else null
        val isModule = searchPayload.firstLevel ?: false // 是否parent代表模块名
        if (isModule) {
            searchPayload.atomicServiceCode = searchPayload.parentId
            searchPayload.parentId = null
        }
        val records = dao.pagingSearch(searchPayload)
        val totalCount = dao.count(searchPayload)
        return Pair(records, totalCount)
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
            dictItemCacheHandler.syncOnUpdateActive(dictItemId)
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

    override fun getDictItemsByDictId(dictId: String): List<SysDictItemRow> {
        val searchPayload = SysDictItemQuery(dictId = dictId)
        return dao.pagingSearch(searchPayload)
    }

    override fun getDictItemsByAtomicServiceAndType(
        dictType: String,
        atomicServiceCode: String
    ): List<SysDictItemCacheEntry> {
        return getItems(dictType, atomicServiceCode)
    }

    /**
     * 获取字典项树（递归结构）
     *
     * @param dictId 字典id
     * @param parentId 父字典项id，为null时返回顶级字典项
     * @return 字典项树节点列表（树形结构，包含children字段）
     * @author AI: Cursor
     * @since 1.0.0
     */
    override fun getDictItemTree(dictId: String, parentId: String?): List<SysDictItemTreeRow> {
        val searchPayload = SysDictItemQuery(
            dictId = dictId,
            parentId = parentId,
        )
        val records = dao.pagingSearch(searchPayload)

        // 转换为树节点
        val treeNodes = records.map { record ->
            SysDictItemTreeRow(
                id = record.itemId,
                itemCode = record.itemCode,
                itemName = record.itemName,
                parentId = record.parentId,
                orderNum = record.orderNum,
                active = record.active,
                remark = record.remark,
                children = mutableListOf()
            )
        }

        // 构建树形结构
        val nodeMap = treeNodes.associateBy { it.id }
        val rootNodes = mutableListOf<SysDictItemTreeRow>()

        treeNodes.forEach { node ->
            if (node.parentId == null) {
                rootNodes.add(node)
            } else {
                val parent = nodeMap[node.parentId]
                parent?.children?.add(node)
            }
        }

        // 按 orderNum 排序
        fun sortTree(nodes: List<SysDictItemTreeRow>) {
            nodes.sortedBy { it.orderNum ?: Int.MAX_VALUE }.forEach { node ->
                node.children?.let { sortTree(it) }
            }
        }
        sortTree(rootNodes)

        return rootNodes.sortedBy { it.orderNum ?: Int.MAX_VALUE }
    }

    override fun getChildItems(parentId: String): List<SysDictItemRow> {
        val searchPayload = SysDictItemQuery().apply {
            this.parentId = parentId
        }
        return dao.pagingSearch(searchPayload)
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
            dictItemCacheHandler.syncOnUpdate(dictItem, id)
        } else {
            log.error("移动字典项${id}失败！")
        }
        return success
    }

    //endregion your codes 2

}