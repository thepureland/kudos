package io.kudos.ms.sys.core.locale.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.sort.Order
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.sys.common.locale.vo.SysLocaleCacheEntry
import io.kudos.ms.sys.core.locale.cache.LocaleByCodeCache
import io.kudos.ms.sys.core.locale.dao.SysLocaleDao
import io.kudos.ms.sys.core.locale.event.SysLocaleBatchDeleted
import io.kudos.ms.sys.core.locale.event.SysLocaleDeleted
import io.kudos.ms.sys.core.locale.event.SysLocaleInserted
import io.kudos.ms.sys.core.locale.event.SysLocaleUpdated
import io.kudos.ms.sys.core.locale.model.po.SysLocale
import io.kudos.ms.sys.core.locale.service.iservice.ISysLocaleService
import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 语言/区域字典业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
@Transactional
open class SysLocaleService(
    dao: SysLocaleDao,
    private val localeByCodeCache: LocaleByCodeCache,
    private val eventPublisher: ApplicationEventPublisher,
) : BaseCrudService<String, SysLocale, SysLocaleDao>(dao), ISysLocaleService {

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getLocaleByCode(code: String): SysLocaleCacheEntry? =
        localeByCodeCache.getLocale(code)

    @Transactional(readOnly = true)
    override fun listActiveLocales(): List<SysLocaleCacheEntry> =
        dao.searchAs(Criteria(SysLocale::active eq true), Order.asc(SysLocale::sortNo.name))

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val locale = SysLocale {
            this.id = id
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(locale),
            log = log,
            successMessage = "更新id为${id}的语言的启用状态为${active}。",
            failureMessage = "更新id为${id}的语言的启用状态为${active}失败！",
        ) {
            eventPublisher.publishEvent(SysLocaleUpdated(id = id))
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "新增id为${id}的语言。") {
            eventPublisher.publishEvent(SysLocaleInserted(id = id))
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireLocaleId(any)
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "更新id为${id}的语言。",
            failureMessage = "更新id为${id}的语言失败！",
        ) {
            eventPublisher.publishEvent(SysLocaleUpdated(id = id))
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val po = dao.get(id) ?: run {
            log.warn("删除id为${id}的语言时，发现其已不存在！")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "删除id为${id}的语言。",
            failureMessage = "删除id为${id}的语言失败！",
        ) {
            eventPublisher.publishEvent(SysLocaleDeleted(id = id, code = po.code))
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val codes = dao.inSearchById(ids).map { it.code }.toSet()
        val count = super.batchDelete(ids)
        log.debug("批量删除语言，期望删除${ids.size}条，实际删除${count}条。")
        if (count > 0) {
            eventPublisher.publishEvent(SysLocaleBatchDeleted(ids = ids, codes = codes))
        }
        return count
    }

    /**
     * 从 update 入参抽 id；要求实现 [IIdEntity] 且 id 是 String。
     *
     * @param any 更新入参
     * @return 语言 id
     * @throws IllegalStateException 入参类型不被支持
     * @author K
     * @since 1.0.0
     */
    private fun requireLocaleId(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新语言时不支持的入参类型: ${any::class.qualifiedName}")
}
