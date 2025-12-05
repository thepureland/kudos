package io.kudos.ams.sys.provider.service.impl

import io.kudos.ams.sys.common.vo.dict.SysDictCacheItem
import io.kudos.ams.sys.common.vo.dict.SysDictPayload
import io.kudos.ams.sys.common.vo.dict.SysDictRecord
import io.kudos.ams.sys.provider.service.iservice.ISysDictItemService
import io.kudos.ams.sys.provider.cache.DictByIdCacheHandler
import io.kudos.ams.sys.provider.model.table.SysDictItems
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import org.ktorm.dsl.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import io.kudos.ams.sys.provider.service.iservice.ISysDictService
import io.kudos.ams.sys.provider.model.po.SysDict
import io.kudos.ams.sys.provider.dao.SysDictDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


/**
 * 字典业务
 *
 * @author K
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
                    moduleCode = payload.moduleCode!!
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
                    moduleCode = payload.moduleCode!!
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

    //endregion your codes 2

}