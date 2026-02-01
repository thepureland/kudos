package io.kudos.ams.sys.core.service.impl

import io.kudos.ams.sys.core.service.iservice.ISysPortalService
import io.kudos.ams.sys.core.service.iservice.ISysSubSystemService
import io.kudos.ams.sys.core.model.po.SysPortal
import io.kudos.ams.sys.core.dao.SysPortalDao
import io.kudos.ams.sys.core.cache.PortalByCodeCacheHandler
import io.kudos.ams.sys.common.vo.portal.SysPortalCacheItem
import io.kudos.ams.sys.common.vo.portal.SysPortalRecord
import io.kudos.ams.sys.common.vo.portal.SysPortalSearchPayload
import io.kudos.ams.sys.common.vo.subsystem.SysSubSystemRecord
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 门户业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysPortalService : BaseCrudService<String, SysPortal, SysPortalDao>(), ISysPortalService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    @Autowired
    private lateinit var portalByCodeCacheHandler: PortalByCodeCacheHandler

    @Autowired
    private lateinit var sysSubSystemService: ISysSubSystemService

    override fun getPortalByCode(code: String): SysPortalCacheItem? {
        return portalByCodeCacheHandler.getPortalByCode(code)
    }

    override fun getAllActivePortals(): List<SysPortalRecord> {
        val searchPayload = SysPortalSearchPayload().apply {
            active = true
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload) as List<SysPortalRecord>
    }

    @Transactional
    override fun updateActive(code: String, active: Boolean): Boolean {
        val portal = SysPortal {
            this.code = code
            this.active = active
        }
        val success = dao.update(portal)
        if (success) {
            log.debug("更新编码为${code}的门户的启用状态为${active}。")
            portalByCodeCacheHandler.syncOnUpdate(code)
        } else {
            log.error("更新编码为${code}的门户的启用状态为${active}失败！")
        }
        return success
    }

    override fun getSubSystemsByPortalCode(portalCode: String): List<SysSubSystemRecord> {
        return sysSubSystemService.getSubSystemsByPortalCode(portalCode)
    }

    @Transactional
    override fun insert(any: Any): String {
        val code = super.insert(any)
        log.debug("新增编码为${code}的门户。")
        portalByCodeCacheHandler.syncOnInsert(code)
        return code
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val code = BeanKit.getProperty(any, SysPortal::code.name) as String
        if (success) {
            log.debug("更新编码为${code}的门户。")
            portalByCodeCacheHandler.syncOnUpdate(code)
        } else {
            log.error("更新编码为${code}的门户失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除编码为${id}的门户。")
            portalByCodeCacheHandler.syncOnDelete(id)
        } else {
            log.error("删除编码为${id}的门户失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除门户，期望删除${ids.size}条，实际删除${count}条。")
        portalByCodeCacheHandler.syncOnBatchDelete(ids)
        return count
    }

    //endregion your codes 2

}