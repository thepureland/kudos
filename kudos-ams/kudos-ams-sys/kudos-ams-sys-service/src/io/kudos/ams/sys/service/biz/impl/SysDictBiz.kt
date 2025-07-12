package io.kudos.ams.sys.service.biz.impl

import io.kudos.ams.sys.service.biz.ibiz.ISysDictBiz
import io.kudos.ams.sys.service.model.po.SysDict
import io.kudos.ams.sys.service.dao.SysDictDao
import io.kudos.ability.data.rdb.ktorm.biz.BaseCrudBiz
import io.kudos.ams.sys.common.vo.dict.SysDictCacheItem
import io.kudos.ams.sys.common.vo.dict.SysDictPayload
import io.kudos.ams.sys.common.vo.dict.SysDictRecord
import io.kudos.ams.sys.common.vo.dict.SysDictSearchPayload
import io.kudos.ams.sys.common.vo.dict.SysDictTreeNode
import io.kudos.ams.sys.service.biz.ibiz.ISysDictItemBiz
import io.kudos.ams.sys.service.cache.DictByIdCacheHandler
import io.kudos.ams.sys.service.model.table.SysDictItems
import io.kudos.ams.sys.service.model.table.SysDicts
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.sort.Order
import io.kudos.base.support.payload.ListSearchPayload
import org.ktorm.dsl.asc
import org.ktorm.dsl.eq
import org.ktorm.dsl.map
import org.ktorm.dsl.orderBy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.text.get
import kotlin.text.insert


/**
 * 字典业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysDictBiz : BaseCrudBiz<String, SysDict, SysDictDao>(), ISysDictBiz {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var sysDictItemBiz: ISysDictItemBiz

    @Autowired
    private lateinit var dictCacheHandler: DictByIdCacheHandler

    private val log = LogFactory.getLog(this)

    override fun getDictFromCache(dictId: String): SysDictCacheItem? {
        return dictCacheHandler.getDictById(dictId)
    }

    override fun getDictIdByModuleAndType(module: String, type: String): String? {
        return dao.getDictIdByModuleAndType(module, type)
    }

    override fun pagingSearch(listSearchPayload: ListSearchPayload): Pair<List<SysDictRecord>, Int> {
        val dictItems = dao.pagingSearch(listSearchPayload as SysDictSearchPayload)
        val totalCount = if (dictItems.isNotEmpty()) {
            // 查询parentCode
            val parentIds = dictItems.filter { !it.parentId.isNullOrBlank() }.map { it.parentId }.toSet()
            val returnProperties = listOf(SysDictItems.id.name, SysDictItems.itemCode.name)
            val idAndCodeMaps = sysDictItemBiz.inSearchProperties(SysDictItems.id.name, parentIds, returnProperties)
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

    override fun loadDirectChildrenForTree(
        parent: String?,
        isModule: Boolean,
        activeOnly: Boolean
    ): List<SysDictTreeNode> {
        return when {
            parent.isNullOrBlank() -> { // 加载模块列表
                val items = sysDictItemBiz.getItemsFromCache("kuark:sys", "module")
                items.map {
                    SysDictTreeNode().apply {
                        code = it.itemCode
                        id = code
                    }
                }
            }
            isModule -> { // 加载RegDict数据
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
                val searchPayload = SysDictSearchPayload().apply {
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

    override fun loadDirectChildrenForList(searchPayload: SysDictSearchPayload): Pair<List<SysDictRecord>, Int> {
        val activeOnly = searchPayload.active ?: false // 是否只加载启用状态的数据, 默认为是
        searchPayload.active = if (activeOnly) true else null
        val isModule = searchPayload.firstLevel ?: false // 是否parent代表模块名
        if (isModule) {
            searchPayload.module = searchPayload.parentId
            searchPayload.parentId = null
        }
        val records = dao.pagingSearch(searchPayload)
        val totalCount = dao.count(searchPayload)
        return Pair(records, totalCount)
    }

    override fun get(id: String, isDict: Boolean?, fetchAllParentIds: Boolean): SysDictRecord? {
        return if (isDict == true) {
            val dict = dao.get(id) ?: return null
            val sysDictRecord = SysDictRecord()
            BeanKit.copyProperties(dict, sysDictRecord)
            sysDictRecord
        } else {
            val searchPayload = SysDictSearchPayload().apply {
                this.id = id
                pageSize = 1
            }
            val result = dao.pagingSearch(searchPayload).firstOrNull()
            if (result != null && fetchAllParentIds) {
                val parentId = result.parentId
                if (!parentId.isNullOrBlank()) {
                    var parentIds = sysDictItemBiz.fetchAllParentIds(parentId!!)
                    parentIds = parentIds.toMutableList()
                    parentIds.add(parentId)
                    result.parentIds = parentIds
                }
            }
            result
        }
    }

    @Transactional
    override fun saveOrUpdate(payload: SysDictPayload): String {
        return if (payload.id.isNullOrBlank()) { // 新增
            if (!payload.parentId.isNullOrBlank()) { // 添加RegDict
                val sysDict = SysDict().apply {
                    moduleCode = payload.moduleCode!!
                    dictType = payload.code!!
                    dictName = payload.name!!
                    remark = payload.remark
                }
                val id = dao.insert(sysDict)
                dictCacheHandler.syncOnInsert(id) // 同步缓存
                id
            } else { // 添加RegDictItem
                sysDictItemBiz.saveOrUpdate(payload)
            }
        } else { // 更新
            if (payload.parentId.isNullOrBlank()) { // RegDict
                val sysDict = SysDict {
                    id = payload.id
                    moduleCode = payload.moduleCode!!
                    dictType = payload.code!!
                    dictName = payload.name!!
                    remark = payload.remark
                }
                val success = dao.update(sysDict)
                if (success) {
                    dictCacheHandler.syncOnUpdate(sysDict.id!!) // 同步缓存
                } else {
                    log.error("删除id为${sysDict.id}的字典失败！")
                }

            } else { // SysDictItem
                sysDictItemBiz.saveOrUpdate(payload)
            }
            payload.id!!
        }
    }

    @Transactional
    override fun delete(id: String, isDict: Boolean): Boolean {
        return if (isDict) {
            dao.batchDeleteWhen { column, _ ->
                if (column.name == SysDictItems.dictId.name) {
                    column.eq(id)
                } else null
            }
            val success = dao.deleteById(id)
            if (success) {
                dictCacheHandler.syncOnDelete(id) // 同步缓存
            } else {
                log.error("删除id为${id}的字典失败！")
            }
            success
        } else {
            sysDictItemBiz.cascadeDeleteChildren(id)
        }
    }

    //endregion your codes 2

}