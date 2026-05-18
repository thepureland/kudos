package io.kudos.ms.sys.core.outline.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.sys.common.outline.vo.SysOutLineCacheEntry
import io.kudos.ms.sys.core.outline.cache.OutLineBySystemAndTenantCache
import io.kudos.ms.sys.core.outline.dao.SysOutLineDao
import io.kudos.ms.sys.core.outline.event.SysOutLineBatchDeleted
import io.kudos.ms.sys.core.outline.event.SysOutLineDeleted
import io.kudos.ms.sys.core.outline.event.SysOutLineInserted
import io.kudos.ms.sys.core.outline.event.SysOutLineUpdated
import io.kudos.ms.sys.core.outline.model.po.SysOutLine
import io.kudos.ms.sys.core.outline.service.iservice.ISysOutLineService
import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 出网白名单业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
@Transactional
open class SysOutLineService(
    dao: SysOutLineDao,
    private val outLineBySystemAndTenantCache: OutLineBySystemAndTenantCache,
    private val eventPublisher: ApplicationEventPublisher,
) : BaseCrudService<String, SysOutLine, SysOutLineDao>(dao), ISysOutLineService {

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun listActiveOutLines(systemCode: String, tenantId: String?): List<SysOutLineCacheEntry> =
        outLineBySystemAndTenantCache.listOutLines(systemCode, tenantId)

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val outLine = SysOutLine {
            this.id = id
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(outLine),
            log = log,
            successMessage = "更新id为${id}的出网白名单的启用状态为${active}。",
            failureMessage = "更新id为${id}的出网白名单的启用状态为${active}失败！",
        ) {
            eventPublisher.publishEvent(SysOutLineUpdated(id = id))
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "新增id为${id}的出网白名单。") {
            eventPublisher.publishEvent(SysOutLineInserted(id = id))
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireOutLineId(any)
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "更新id为${id}的出网白名单。",
            failureMessage = "更新id为${id}的出网白名单失败！",
        ) {
            eventPublisher.publishEvent(SysOutLineUpdated(id = id))
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val po = dao.get(id)
        if (po == null) {
            log.warn("删除id为${id}的出网白名单时，发现其已不存在！")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "删除id为${id}的出网白名单。",
            failureMessage = "删除id为${id}的出网白名单失败！",
        ) {
            eventPublisher.publishEvent(
                SysOutLineDeleted(id = id, systemCode = po.systemCode, tenantId = po.tenantId)
            )
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val pos = dao.inSearchById(ids)
        val dimensions = pos.map { it.systemCode to it.tenantId }.toSet()
        val count = super.batchDelete(ids)
        log.debug("批量删除出网白名单，期望删除${ids.size}条，实际删除${count}条。")
        if (count > 0) {
            eventPublisher.publishEvent(SysOutLineBatchDeleted(ids = ids, dimensions = dimensions))
        }
        return count
    }

    private fun requireOutLineId(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新出网白名单时不支持的入参类型: ${any::class.qualifiedName}")
}
