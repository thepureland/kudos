package io.kudos.ams.sys.provider.service.impl

import io.kudos.ams.sys.provider.service.iservice.ISysModuleService
import io.kudos.ams.sys.provider.model.po.SysModule
import io.kudos.ams.sys.provider.dao.SysModuleDao
import io.kudos.ams.sys.provider.cache.ModuleByCodeCacheHandler
import io.kudos.ams.sys.common.vo.module.SysModuleCacheItem
import io.kudos.ams.sys.common.vo.module.SysModuleRecord
import io.kudos.ams.sys.common.vo.module.SysModuleSearchPayload
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 模块业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysModuleService : BaseCrudService<String, SysModule, SysModuleDao>(), ISysModuleService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    @Autowired
    private lateinit var moduleByCodeCacheHandler: ModuleByCodeCacheHandler

    override fun getModuleByCode(code: String): SysModuleCacheItem? {
        return moduleByCodeCacheHandler.getModuleByCode(code)
    }

    override fun getModulesByAtomicServiceCode(atomicServiceCode: String): List<SysModuleRecord> {
        val searchPayload = SysModuleSearchPayload().apply {
            this.atomicServiceCode = atomicServiceCode
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload) as List<SysModuleRecord>
    }

    @Transactional
    override fun updateActive(code: String, active: Boolean): Boolean {
        val module = SysModule {
            this.code = code
            this.active = active
        }
        val success = dao.update(module)
        if (success) {
            log.debug("更新编码为${code}的模块的启用状态为${active}。")
            moduleByCodeCacheHandler.syncOnUpdate(code)
        } else {
            log.error("更新编码为${code}的模块的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        val code = super.insert(any)
        log.debug("新增编码为${code}的模块。")
        moduleByCodeCacheHandler.syncOnInsert(code)
        return code
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val code = BeanKit.getProperty(any, SysModule::code.name) as String
        if (success) {
            log.debug("更新编码为${code}的模块。")
            moduleByCodeCacheHandler.syncOnUpdate(code)
        } else {
            log.error("更新编码为${code}的模块失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除编码为${id}的模块。")
            moduleByCodeCacheHandler.syncOnDelete(id)
        } else {
            log.error("删除编码为${id}的模块失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除模块，期望删除${ids.size}条，实际删除${count}条。")
        moduleByCodeCacheHandler.syncOnBatchDelete(ids)
        return count
    }

    //endregion your codes 2

}