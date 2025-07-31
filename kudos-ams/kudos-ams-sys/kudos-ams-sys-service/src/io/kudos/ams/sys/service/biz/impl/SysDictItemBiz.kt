package io.kudos.ams.sys.service.biz.impl

import io.kudos.ams.sys.common.vo.dict.SysDictPayload
import io.kudos.ams.sys.common.vo.dictitem.SysDictItemCacheItem
import io.kudos.ams.sys.service.cache.DictItemsByModuleAndTypeCacheHandler
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import kotlin.text.insert
import io.kudos.ams.sys.service.biz.ibiz.ISysDictItemBiz
import io.kudos.ams.sys.service.model.po.SysDictItem
import io.kudos.ams.sys.service.dao.SysDictItemDao
import io.kudos.ability.data.rdb.ktorm.biz.BaseCrudBiz
import org.springframework.stereotype.Service


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


    override fun getItemsFromCache(module: String, type: String): List<SysDictItemCacheItem> {
        return dictItemCacheHandler.getItemsFromCache(module, type)
    }

    override fun transDictCode(module: String, type: String, code: String): String? {
        val items = dictItemCacheHandler.getItemsFromCache(module, type)
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
            dictItemCacheHandler.syncOnInsert(sysDictItem) // 同步缓存
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
                dictItemCacheHandler.syncOnUpdate(sysDictItem) // 同步缓存
            } else {
                log.error("新增id为${sysDictItem.id}的字典项失败！")
            }
            sysDictItem.id!!
        }
    }

    override fun fetchAllParentIds(itemId: String): List<String> {
        val results = mutableListOf<String>()
        recursionFindAllParentId(itemId, results)
        results.reverse()
        return results
    }

    @Transactional
    override fun cascadeDeleteChildren(id: String): Boolean {
        val dictIds = dao.oneSearchProperty(SysDictItem::id.name, id, SysDictItem::dictId.name)
        val childItemIds = mutableListOf<String>()
        recursionFindAllChildId(id, childItemIds)
        if (childItemIds.isNotEmpty()) {
            dao.batchDelete(childItemIds)
        }
        val success = dao.deleteById(id)
        if (success) {
            dictItemCacheHandler.syncOnDelete(id, dictIds.first() as String) // 同步缓存
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