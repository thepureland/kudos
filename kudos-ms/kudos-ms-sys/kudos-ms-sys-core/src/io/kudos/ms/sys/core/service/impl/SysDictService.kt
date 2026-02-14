package io.kudos.ms.sys.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.vo.dict.SysDictCacheItem
import io.kudos.ms.sys.common.vo.dict.SysDictPayload
import io.kudos.ms.sys.common.vo.dict.SysDictRecord
import io.kudos.ms.sys.common.vo.dict.SysDictSearchPayload
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheItem
import io.kudos.ms.sys.core.cache.DictByIdCache
import io.kudos.ms.sys.core.dao.SysDictDao
import io.kudos.ms.sys.core.model.po.SysDict
import io.kudos.ms.sys.core.model.table.SysDictItems
import io.kudos.ms.sys.core.service.iservice.ISysDictItemService
import io.kudos.ms.sys.core.service.iservice.ISysDictService
import org.ktorm.dsl.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 字典业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysDictService : BaseCrudService<String, SysDict, SysDictDao>(), ISysDictService {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var sysDictItemService: ISysDictItemService

    @Autowired
    private lateinit var dictCacheHandler: DictByIdCache

    private val log = LogFactory.getLog(this)

    override fun getDictFromCache(dictId: String): SysDictCacheItem? {
        return dictCacheHandler.getDictById(dictId)
    }


    override fun getRecord(id: String): SysDictRecord? {
        val dict = dao.get(id) ?: return null
        val sysDictRecord = SysDictRecord()
        BeanKit.copyProperties(dict, sysDictRecord)
        return sysDictRecord
    }

    @Transactional
    override fun saveOrUpdate(payload: SysDictPayload): String {
        return if (payload.id.isBlank()) { // 新增
            if (!payload.parentId.isNullOrBlank()) { // 添加SysDict
                val atomicServiceCode = requireNotNull(payload.atomicServiceCode) { "新增字典时，atomicServiceCode不能为空。" }
                val dictType = requireNotNull(payload.dictType) { "新增字典时，dictType不能为空。" }
                val dictName = requireNotNull(payload.dictName) { "新增字典时，dictName不能为空。" }
                val sysDict = SysDict().apply {
                    this.atomicServiceCode = atomicServiceCode
                    this.dictType = dictType
                    this.dictName = dictName
                    remark = payload.remark
                }
                val id = dao.insert(sysDict)
                dictCacheHandler.syncOnInsert(id) // 同步缓存
                id
            } else { // 添加SysDictItem
                sysDictItemService.saveOrUpdate(payload)
            }
        } else { // 更新
            if (payload.parentId.isNullOrBlank()) { // SysDict
                val atomicServiceCode = requireNotNull(payload.atomicServiceCode) { "更新字典时，atomicServiceCode不能为空。" }
                val dictType = requireNotNull(payload.dictType) { "更新字典时，dictType不能为空。" }
                val dictName = requireNotNull(payload.dictName) { "更新字典时，dictName不能为空。" }
                val sysDict = SysDict {
                    id = payload.id
                    this.atomicServiceCode = atomicServiceCode
                    this.dictType = dictType
                    this.dictName = dictName
                    remark = payload.remark
                }
                val success = dao.update(sysDict)
                if (success) {
                    dictCacheHandler.syncOnUpdate(sysDict.id) // 同步缓存
                } else {
                    log.error("删除id为${sysDict.id}的字典失败！")
                }

            } else { // SysDictItem
                sysDictItemService.saveOrUpdate(payload)
            }
            payload.id
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
            sysDictItemService.cascadeDeleteChildren(id)
        }
    }

    /**
     * 获取模块的所有字典
     *
     * @param atomicServiceCode 原子服务编码
     * @return 字典记录列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    override fun getDictsByAtomicServiceCode(atomicServiceCode: String): List<SysDictRecord> {
        val searchPayload = SysDictSearchPayload().apply {
            this.atomicServiceCode = atomicServiceCode
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload, SysDictRecord::class)
    }

    /**
     * 根据原子服务编码和字典类型获取字典
     *
     * @param atomicServiceCode 原子服务编码
     * @param dictType 字典类型
     * @return 字典记录，找不到返回null
     * @author AI: Cursor
     * @since 1.0.0
     */
    override fun getDictByAtomicServiceAndType(atomicServiceCode: String, dictType: String): SysDictRecord? {
        val searchPayload = SysDictSearchPayload().apply {
            this.atomicServiceCode = atomicServiceCode
            this.dictType = dictType
        }
        @Suppress("UNCHECKED_CAST")
        val records = dao.search(searchPayload, SysDictRecord::class)
        return records.firstOrNull()
    }

    /**
     * 更新启用状态，并同步缓存
     *
     * @param id 字典id
     * @param active 是否启用
     * @return 是否更新成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val dict = SysDict {
            this.id = id
            this.active = active
        }
        val success = dao.update(dict)
        if (success) {
            log.debug("更新id为${id}的字典的启用状态为${active}。")
            dictCacheHandler.syncOnUpdate(id)
        } else {
            log.error("更新id为${id}的字典的启用状态为${active}失败！")
        }
        return success
    }

    /**
     * 新增字典
     *
     * @param any 字典对象
     * @return 主键
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的字典。")
        dictCacheHandler.syncOnInsert(id)
        return id
    }

    /**
     * 更新字典
     *
     * @param any 字典对象
     * @return 是否更新成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, SysDict::id.name) as String
        if (success) {
            log.debug("更新id为${id}的字典。")
            dictCacheHandler.syncOnUpdate(id)
        } else {
            log.error("更新id为${id}的字典失败！")
        }
        return success
    }

    /**
     * 删除字典
     *
     * @param id 主键
     * @return 是否删除成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun deleteById(id: String): Boolean {
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除id为${id}的字典。")
            dictCacheHandler.syncOnDelete(id)
        } else {
            log.error("删除id为${id}的字典失败！")
        }
        return success
    }

    /**
     * 批量删除字典
     *
     * @param ids 主键集合
     * @return 删除的数量
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除字典，期望删除${ids.size}条，实际删除${count}条。")
        dictCacheHandler.syncOnBatchDelete(ids)
        return count
    }

    override fun getActiveDictItems(
        dictType: String,
        atomicServiceCode: String?
    ): List<SysDictItemCacheItem> {
        return sysDictItemService.getItemsFromCache(atomicServiceCode, dictType)
    }

    override fun getActiveDictItemMap(
        dictType: String,
        atomicServiceCode: String?
    ): LinkedHashMap<String, String> {
        val items = sysDictItemService.getItemsFromCache(atomicServiceCode, dictType)
        return LinkedHashMap<String, String>().apply {
            items.filter { it.itemCode != null }.forEach { put(it.itemCode!!, it.itemName ?: "") }
        }
    }

    override fun batchGetActiveDictItems(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, List<SysDictItemCacheItem>> {
        return dictTypeAndASCodePairs.associate { (dictType, atomicServiceCode) ->
            Pair(atomicServiceCode, dictType) to sysDictItemService.getItemsFromCache(atomicServiceCode, dictType)
        }
    }

    override fun batchGetActiveDictItemMap(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, LinkedHashMap<String, String>> {
        return dictTypeAndASCodePairs.associate { (dictType, atomicServiceCode) ->
            val items = sysDictItemService.getItemsFromCache(atomicServiceCode, dictType)
            val map = LinkedHashMap<String, String>().apply {
                items.filter { it.itemCode != null }.forEach { put(it.itemCode!!, it.itemName ?: "") }
            }
            Pair(atomicServiceCode, dictType) to map
        }
    }

    //endregion your codes 2

}