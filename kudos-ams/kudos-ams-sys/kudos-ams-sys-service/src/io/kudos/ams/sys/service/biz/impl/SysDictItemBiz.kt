package io.kudos.ams.sys.service.biz.impl

import io.kudos.ability.data.rdb.ktorm.biz.BaseCrudBiz
import io.kudos.ams.sys.common.vo.dict.SysDictPayload
import io.kudos.ams.sys.common.vo.dict.SysDictTreeNode
import io.kudos.ams.sys.common.vo.dictitem.SysDictItemCacheItem
import io.kudos.ams.sys.common.vo.dictitem.SysDictItemRecord
import io.kudos.ams.sys.common.vo.dictitem.SysDictItemSearchPayload
import io.kudos.ams.sys.service.biz.ibiz.ISysDictItemBiz
import io.kudos.ams.sys.service.cache.DictItemsByModuleAndTypeCacheHandler
import io.kudos.ams.sys.service.dao.SysDictItemDao
import io.kudos.ams.sys.service.model.po.SysDict
import io.kudos.ams.sys.service.model.po.SysDictItem
import io.kudos.ams.sys.service.model.table.SysDictItems
import io.kudos.ams.sys.service.model.table.SysDicts
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.sort.Order
import io.kudos.base.support.payload.ListSearchPayload
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
open class SysDictItemBiz : BaseCrudBiz<String, SysDictItem, SysDictItemDao>(), ISysDictItemBiz {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var dictItemCacheHandler: DictItemsByModuleAndTypeCacheHandler

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
                var parentIds = fetchAllParentIds(parentId!!)
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
            val parentIds = dictItems.filter { !it.parentId.isNullOrBlank() }.map { it.parentId }.toSet()
            val returnProperties = listOf(SysDictItems.id.name, SysDictItems.itemCode.name)
            val idAndCodeMaps = inSearchProperties(SysDictItems.id.name, parentIds, returnProperties)
            dictItems.forEach { dictItem ->
                val idAndCodeMap = idAndCodeMaps.singleOrNull { it[SysDictItems.id.name] == dictItem.parentId }
                if (idAndCodeMap != null) {
                    dictItem.parentCode = idAndCodeMap[SysDictItems.itemCode.name] as String?
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
                val results = dao.oneSearch(SysDicts.moduleCode.name, parent, Order.asc(SysDicts.dictType.name))
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
            searchPayload.moduleCode = searchPayload.parentId
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
        val list = dao.oneSearchProperty(SysDictItem::id.name, itemId, SysDictItem::parentId.name)
        if (list.isNotEmpty()) {
            val parentId = list.first() as String
            results.add(parentId)
            recursionFindAllParentId(parentId, results)
        }
    }

    private fun recursionFindAllChildId(itemId: String, results: MutableList<String>) {
        val itemIds = dao.oneSearchProperty(SysDictItem::parentId.name, itemId, SysDictItem::id.name)
        itemIds.forEach { id ->
            results.add(id as String)
            recursionFindAllChildId(id, results)
        }
    }

    //endregion your codes 2

}