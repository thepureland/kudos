package io.kudos.ams.sys.core.service.impl

import io.kudos.ams.sys.core.service.iservice.ISysSubSystemService
import io.kudos.ams.sys.core.service.iservice.ISysSubSystemMicroServiceService
import io.kudos.ams.sys.core.service.iservice.ISysMicroServiceService
import io.kudos.ams.sys.core.model.po.SysSubSystem
import io.kudos.ams.sys.core.model.po.SysMicroService
import io.kudos.ams.sys.core.dao.SysSubSystemDao
import io.kudos.ams.sys.core.dao.SysMicroServiceDao
import io.kudos.ams.sys.core.cache.SubSystemByCodeCacheHandler
import io.kudos.ams.sys.common.vo.subsystem.SysSubSystemCacheItem
import io.kudos.ams.sys.common.vo.subsystem.SysSubSystemRecord
import io.kudos.ams.sys.common.vo.subsystem.SysSubSystemSearchPayload
import io.kudos.ams.sys.common.vo.microservice.SysMicroServiceRecord
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 子系统业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysSubSystemService : BaseCrudService<String, SysSubSystem, SysSubSystemDao>(), ISysSubSystemService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    @Autowired
    private lateinit var subSystemByCodeCacheHandler: SubSystemByCodeCacheHandler

    @Autowired
    private lateinit var sysSubSystemMicroServiceService: ISysSubSystemMicroServiceService

    @Autowired
    private lateinit var sysMicroServiceService: ISysMicroServiceService

    @Autowired
    private lateinit var sysMicroServiceDao: SysMicroServiceDao

    override fun getSubSystemByCode(code: String): SysSubSystemCacheItem? {
        return subSystemByCodeCacheHandler.getSubSystemByCode(code)
    }

    override fun getSubSystemsByPortalCode(portalCode: String): List<SysSubSystemRecord> {
        val searchPayload = SysSubSystemSearchPayload().apply {
            this.portalCode = portalCode
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload) as List<SysSubSystemRecord>
    }

    @Transactional
    override fun updateActive(code: String, active: Boolean): Boolean {
        val subSystem = SysSubSystem {
            this.code = code
            this.active = active
        }
        val success = dao.update(subSystem)
        if (success) {
            log.debug("更新编码为${code}的子系统的启用状态为${active}。")
            subSystemByCodeCacheHandler.syncOnUpdate(code)
        } else {
            log.error("更新编码为${code}的子系统的启用状态为${active}失败！")
        }
        return success
    }

    override fun getMicroServicesBySubSystemCode(subSystemCode: String): List<SysMicroServiceRecord> {
        val microServiceCodes = sysSubSystemMicroServiceService.getMicroServiceCodesBySubSystemCode(subSystemCode)
        if (microServiceCodes.isEmpty()) {
            return emptyList()
        }
        val criteria = Criteria.of(SysMicroService::code.name, OperatorEnum.IN, microServiceCodes)
        val pos = sysMicroServiceDao.search(criteria)
        return pos.map { po ->
            SysMicroServiceRecord().apply {
                BeanKit.copyProperties(po, this)
            }
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val code = super.insert(any)
        log.debug("新增编码为${code}的子系统。")
        subSystemByCodeCacheHandler.syncOnInsert(code)
        return code
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val code = BeanKit.getProperty(any, SysSubSystem::code.name) as String
        if (success) {
            log.debug("更新编码为${code}的子系统。")
            subSystemByCodeCacheHandler.syncOnUpdate(code)
        } else {
            log.error("更新编码为${code}的子系统失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除编码为${id}的子系统。")
            subSystemByCodeCacheHandler.syncOnDelete(id)
        } else {
            log.error("删除编码为${id}的子系统失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除子系统，期望删除${ids.size}条，实际删除${count}条。")
        subSystemByCodeCacheHandler.syncOnBatchDelete(ids)
        return count
    }

    //endregion your codes 2

}