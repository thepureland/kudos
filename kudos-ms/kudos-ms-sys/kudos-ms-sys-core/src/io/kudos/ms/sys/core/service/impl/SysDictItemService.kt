package io.kudos.ms.sys.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.sort.Order
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.base.support.query.ReadQuery
import io.kudos.ms.sys.common.vo.dict.SysDictPayload
import io.kudos.ms.sys.common.vo.dict.SysDictTreeNode
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheItem
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemRecord
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemSearchPayload
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemTreeRecord
import io.kudos.ms.sys.core.cache.DictItemsByModuleAndTypeCache
import io.kudos.ms.sys.core.dao.SysDictItemDao
import io.kudos.ms.sys.core.model.po.SysDict
import io.kudos.ms.sys.core.model.po.SysDictItem
import io.kudos.ms.sys.core.model.table.SysDictItems
import io.kudos.ms.sys.core.model.table.SysDicts
import io.kudos.ms.sys.core.service.iservice.ISysDictItemService
import org.ktorm.dsl.asc
import org.ktorm.dsl.map
import org.ktorm.dsl.orderBy
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
    private lateinit var dictItemCacheHandler: DictItemsByModuleAndTypeCache

    private val log = LogFactory.getLog(this)

    override fun get(id: String, fetchAllParentIds: Boolean): SysDictItemRecord? {
        val searchPayload = SysDictItemSearchPayload().apply {
            this.id = id
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


    override fun getItemsFromCache(module: String, type: String): List<SysDictItemCacheItem> {
        return dictItemCacheHandler.getDictItems(module, type)
    }

    override fun transDictCode(module: String, type: String, code: String): String? {
        val items = dictItemCacheHandler.getDictItems(module, type)
        return items.firstOrNull { it.itemCode == code }?.itemName
    }

    @Transactional
    override fun saveOrUpdate(payload: SysDictPayload): String {
        return if (payload.id.isNullOrBlank()) { // 新增
            val sysDictItem = SysDictItem().apply {
                dictId = payload.id!!
                parentId = payload.parentId
                itemCode = payload.code!!
                itemName = payload.name!!
                orderNum = payload.seqNo
                remark = payload.remark
            }
            val id = dao.insert(sysDictItem)
            dictItemCacheHandler.syncOnInsert(sysDictItem, id) // 同步缓存
            id
        } else { // 更新
            val sysDictItem = SysDictItem {
                id = payload.id
                dictId = payload.id!!
                parentId = payload.parentId
                itemCode = payload.code!!
                itemName = payload.name!!
                orderNum = payload.seqNo
                remark = payload.remark
            }
            val success = dao.update(sysDictItem)
            if (success) {
                dictItemCacheHandler.syncOnUpdate(sysDictItem, sysDictItem.id!!) // 同步缓存
            } else {
                log.error("新增id为${sysDictItem.id}的字典项失败！")
            }
            sysDictItem.id!!
        }
    }

    override fun pagingSearch(listSearchPayload: ListSearchPayload): Pair<List<SysDictItemRecord>, Int> {
        val dictItems = dao.pagingSearch(listSearchPayload as SysDictItemSearchPayload)
        val totalCount = if (dictItems.isNotEmpty()) {
            // 查询parentCode
            val parentIds = dictItems.filter { !it.parentId.isNullOrBlank() }.mapNotNull { it.parentId }.toSet()
            val criteria = Criteria.of(SysDictItem::id.name, OperatorEnum.IN, parentIds)
            val idAndCodeMaps = dao.searchPropertiesBy(criteria, listOf(SysDictItem::id, SysDictItem::itemCode))
            dictItems.forEach { dictItem ->
                val idAndCodeMap = idAndCodeMaps.singleOrNull { it[SysDictItem::id.name] == dictItem.parentId }
                if (idAndCodeMap != null) {
                    dictItem.parentCode = idAndCodeMap[SysDictItem::itemCode.name] as String?
                }
            }
            dao.count(listSearchPayload)
        } else 0
        return Pair(dictItems, totalCount)
    }

    override fun fetchAllParentIds(itemId: String): List<String> {
        val results = mutableListOf<String>()
        recursionFindAllParentId(itemId, results)
        results.reverse()
        return results
    }

    @Transactional
    override fun cascadeDeleteChildren(id: String): Boolean {
        val dictItem = dao.get(id)!!
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
                val items = getItemsFromCache("kuark:sys", "module")
                items.map {
                    SysDictTreeNode().apply {
                        code = it.itemCode
                        id = code
                    }
                }
            }
            isModule -> { // 加载SysDict数据
                val criteria = Criteria.of(SysDicts.atomicServiceCode.name, OperatorEnum.EQ, parent)
                val results = dao.search(
                    ReadQuery(
                        criteria = criteria,
                        orders = listOf(Order.asc(SysDicts.dictType.name))
                    )
                )
                results.map {
                    val treeNode = BeanKit.copyProperties(
                        SysDictTreeNode::class, it, mapOf(
                            SysDict::id.name to SysDictTreeNode::id.name,
                            SysDict::dictType.name to SysDictTreeNode::code.name,
                        )
                    )
                    treeNode
                }
            }
            else -> { // 加载SysDictItem数据
                val searchPayload = SysDictItemSearchPayload().apply {
                    this.parentId = parent
                    this.active = if (activeOnly) true else null
                }
                dao.leftJoinSearch(searchPayload)
                    .orderBy(SysDictItems.orderNum.asc())
                    .map { row ->
                        SysDictTreeNode().apply {
                            id = row[SysDictItems.id]
                            code = row[SysDictItems.itemCode]
                        }
                    }
            }
        }
    }

    override fun loadDirectChildrenForList(searchPayload: SysDictItemSearchPayload): Pair<List<SysDictItemRecord>, Int> {
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
            results.add(id as String)
            recursionFindAllChildId(id, results)
        }
    }

    override fun getDictItemsByDictId(dictId: String): List<SysDictItemRecord> {
        val searchPayload = SysDictItemSearchPayload().apply {
            this.dictId = dictId
        }
        return dao.pagingSearch(searchPayload)
    }

    override fun getDictItemsByAtomicServiceAndType(atomicServiceCode: String, dictType: String): List<SysDictItemCacheItem> {
        return getItemsFromCache(atomicServiceCode, dictType)
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
    override fun getDictItemTree(dictId: String, parentId: String?): List<SysDictItemTreeRecord> {
        val searchPayload = SysDictItemSearchPayload().apply {
            this.dictId = dictId
            this.parentId = parentId
        }
        val records = dao.pagingSearch(searchPayload)
        
        // 转换为树节点
        val treeNodes = records.map { record ->
            SysDictItemTreeRecord().apply {
                id = record.itemId
                itemCode = record.itemCode
                itemName = record.itemName
                this.parentId = record.parentId
                orderNum = record.orderNum
                active = record.active
                remark = record.remark
                children = mutableListOf()
            }
        }
        
        // 构建树形结构
        val nodeMap = treeNodes.associateBy { it.id }
        val rootNodes = mutableListOf<SysDictItemTreeRecord>()
        
        treeNodes.forEach { node ->
            if (node.parentId == null) {
                rootNodes.add(node)
            } else {
                val parent = nodeMap[node.parentId]
                parent?.children?.add(node)
            }
        }
        
        // 按 orderNum 排序
        fun sortTree(nodes: List<SysDictItemTreeRecord>) {
            nodes.sortedBy { it.orderNum ?: Int.MAX_VALUE }.forEach { node ->
                node.children?.let { sortTree(it) }
            }
        }
        sortTree(rootNodes)
        
        return rootNodes.sortedBy { it.orderNum ?: Int.MAX_VALUE }
    }

    override fun getChildItems(parentId: String): List<SysDictItemRecord> {
        val searchPayload = SysDictItemSearchPayload().apply {
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