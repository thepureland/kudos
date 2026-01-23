package io.kudos.ams.sys.provider.service.impl

import io.kudos.ams.sys.provider.service.iservice.ISysAtomicServiceService
import io.kudos.ams.sys.provider.service.iservice.ISysModuleService
import io.kudos.ams.sys.provider.model.po.SysAtomicService
import io.kudos.ams.sys.provider.dao.SysAtomicServiceDao
import io.kudos.ams.sys.provider.cache.AtomicServiceByCodeCacheHandler
import io.kudos.ams.sys.common.vo.atomicservice.SysAtomicServiceCacheItem
import io.kudos.ams.sys.common.vo.module.SysModuleRecord
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 原子服务业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysAtomicServiceService : BaseCrudService<String, SysAtomicService, SysAtomicServiceDao>(), ISysAtomicServiceService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    @Autowired
    private lateinit var atomicServiceByCodeCacheHandler: AtomicServiceByCodeCacheHandler

    @Autowired
    private lateinit var sysModuleService: ISysModuleService

    override fun getAtomicServiceByCode(code: String): SysAtomicServiceCacheItem? {
        return atomicServiceByCodeCacheHandler.getAtomicServiceByCode(code)
    }

    @Transactional
    override fun updateActive(code: String, active: Boolean): Boolean {
        val atomicService = SysAtomicService {
            this.code = code
            this.active = active
        }
        val success = dao.update(atomicService)
        if (success) {
            log.debug("更新编码为${code}的原子服务的启用状态为${active}。")
            atomicServiceByCodeCacheHandler.syncOnUpdate(code)
        } else {
            log.error("更新编码为${code}的原子服务的启用状态为${active}失败！")
        }
        return success
    }

    override fun getModulesByAtomicServiceCode(atomicServiceCode: String): List<SysModuleRecord> {
        return sysModuleService.getModulesByAtomicServiceCode(atomicServiceCode)
    }

    @Transactional
    override fun insert(any: Any): String {
        val code = super.insert(any)
        log.debug("新增编码为${code}的原子服务。")
        atomicServiceByCodeCacheHandler.syncOnInsert(code)
        return code
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val code = BeanKit.getProperty(any, SysAtomicService::code.name) as String
        if (success) {
            log.debug("更新编码为${code}的原子服务。")
            atomicServiceByCodeCacheHandler.syncOnUpdate(code)
        } else {
            log.error("更新编码为${code}的原子服务失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除编码为${id}的原子服务。")
            atomicServiceByCodeCacheHandler.syncOnDelete(id)
        } else {
            log.error("删除编码为${id}的原子服务失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除原子服务，期望删除${ids.size}条，实际删除${count}条。")
        atomicServiceByCodeCacheHandler.syncOnBatchDelete(ids)
        return count
    }

    //endregion your codes 2

}