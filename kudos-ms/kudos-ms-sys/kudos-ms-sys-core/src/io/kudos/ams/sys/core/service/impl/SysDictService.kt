package io.kudos.ms.sys.core.service.impl

import io.kudos.ms.sys.common.vo.dict.SysDictCacheItem
import io.kudos.ms.sys.common.vo.dict.SysDictPayload
import io.kudos.ms.sys.common.vo.dict.SysDictRecord
import io.kudos.ms.sys.common.vo.dict.SysDictSearchPayload
import io.kudos.ms.sys.core.service.iservice.ISysDictItemService
import io.kudos.ms.sys.core.cache.DictByIdCacheHandler
import io.kudos.ms.sys.core.model.table.SysDictItems
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import org.ktorm.dsl.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import io.kudos.ms.sys.core.service.iservice.ISysDictService
import io.kudos.ms.sys.core.model.po.SysDict
import io.kudos.ms.sys.core.dao.SysDictDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


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
    private lateinit var sysDictItemBiz: ISysDictItemService

    @Autowired
    private lateinit var dictCacheHandler: DictByIdCacheHandler

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
        return if (payload.id.isNullOrBlank()) { // 新增
            if (!payload.parentId.isNullOrBlank()) { // 添加SysDict
                val sysDict = SysDict().apply {
                    atomicServiceCode = payload.atomicServiceCode!!
                    dictType = payload.dictType!!
                    dictName = payload.dictName!!
                    remark = payload.remark
                }
                val id = dao.insert(sysDict)
                dictCacheHandler.syncOnInsert(id) // 同步缓存
                id
            } else { // 添加SysDictItem
                sysDictItemBiz.saveOrUpdate(payload)
            }
        } else { // 更新
            if (payload.parentId.isNullOrBlank()) { // SysDict
                val sysDict = SysDict {
                    id = payload.id
                    atomicServiceCode = payload.atomicServiceCode!!
                    dictType = payload.dictType!!
                    dictName = payload.dictName!!
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
        return dao.search(searchPayload) as List<SysDictRecord>
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
        val records = dao.search(searchPayload) as List<SysDictRecord>
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

    //endregion your codes 2

}