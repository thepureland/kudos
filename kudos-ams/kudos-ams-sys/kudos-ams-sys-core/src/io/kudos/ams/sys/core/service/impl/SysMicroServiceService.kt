package io.kudos.ams.sys.core.service.impl

import io.kudos.ams.sys.core.service.iservice.ISysMicroServiceService
import io.kudos.ams.sys.core.service.iservice.ISysMicroServiceAtomicServiceService
import io.kudos.ams.sys.core.service.iservice.ISysAtomicServiceService
import io.kudos.ams.sys.core.model.po.SysMicroService
import io.kudos.ams.sys.core.model.po.SysAtomicService
import io.kudos.ams.sys.core.dao.SysMicroServiceDao
import io.kudos.ams.sys.core.dao.SysAtomicServiceDao
import io.kudos.ams.sys.core.cache.MicroServiceByCodeCacheHandler
import io.kudos.ams.sys.common.vo.microservice.SysMicroServiceCacheItem
import io.kudos.ams.sys.common.vo.atomicservice.SysAtomicServiceRecord
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 微服务业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysMicroServiceService : BaseCrudService<String, SysMicroService, SysMicroServiceDao>(), ISysMicroServiceService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    @Autowired
    private lateinit var microServiceByCodeCacheHandler: MicroServiceByCodeCacheHandler

    @Autowired
    private lateinit var sysMicroServiceAtomicServiceService: ISysMicroServiceAtomicServiceService

    @Autowired
    private lateinit var sysAtomicServiceService: ISysAtomicServiceService

    @Autowired
    private lateinit var sysAtomicServiceDao: SysAtomicServiceDao

    override fun getMicroServiceByCode(code: String): SysMicroServiceCacheItem? {
        return microServiceByCodeCacheHandler.getMicroServiceByCode(code)
    }

    @Transactional
    override fun updateActive(code: String, active: Boolean): Boolean {
        val microService = SysMicroService {
            this.code = code
            this.active = active
        }
        val success = dao.update(microService)
        if (success) {
            log.debug("更新编码为${code}的微服务的启用状态为${active}。")
            microServiceByCodeCacheHandler.syncOnUpdate(code)
        } else {
            log.error("更新编码为${code}的微服务的启用状态为${active}失败！")
        }
        return success
    }

    override fun getAtomicServicesByMicroServiceCode(microServiceCode: String): List<SysAtomicServiceRecord> {
        val atomicServiceCodes = sysMicroServiceAtomicServiceService.getAtomicServiceCodesByMicroServiceCode(microServiceCode)
        if (atomicServiceCodes.isEmpty()) {
            return emptyList()
        }
        val criteria = Criteria.of(SysAtomicService::code.name, OperatorEnum.IN, atomicServiceCodes)
        val pos = sysAtomicServiceDao.search(criteria)
        return pos.map { po ->
            SysAtomicServiceRecord().apply {
                BeanKit.copyProperties(po, this)
            }
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val code = super.insert(any)
        log.debug("新增编码为${code}的微服务。")
        microServiceByCodeCacheHandler.syncOnInsert(code)
        return code
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val code = BeanKit.getProperty(any, SysMicroService::code.name) as String
        if (success) {
            log.debug("更新编码为${code}的微服务。")
            microServiceByCodeCacheHandler.syncOnUpdate(code)
        } else {
            log.error("更新编码为${code}的微服务失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除编码为${id}的微服务。")
            microServiceByCodeCacheHandler.syncOnDelete(id)
        } else {
            log.error("删除编码为${id}的微服务失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除微服务，期望删除${ids.size}条，实际删除${count}条。")
        microServiceByCodeCacheHandler.syncOnBatchDelete(ids)
        return count
    }

    //endregion your codes 2

}