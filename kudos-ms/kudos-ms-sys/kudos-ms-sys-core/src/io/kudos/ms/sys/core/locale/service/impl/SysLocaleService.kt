package io.kudos.ms.sys.core.locale.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.platform.service.impl.requireStringId
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
 * Business service for the language/locale dictionary.
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
            successMessage = "Updated active status of locale with id $id to $active.",
            failureMessage = "Failed to update active status of locale with id $id to $active!",
        ) {
            eventPublisher.publishEvent(SysLocaleUpdated(id = id))
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "Inserted locale with id $id.") {
            eventPublisher.publishEvent(SysLocaleInserted(id = id))
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireStringId(any, "locale")
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "Updated locale with id $id.",
            failureMessage = "Failed to update locale with id $id!",
        ) {
            eventPublisher.publishEvent(SysLocaleUpdated(id = id))
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val po = dao.get(id) ?: run {
            log.warn("Tried to delete locale with id $id, but it no longer exists!")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "Deleted locale with id $id.",
            failureMessage = "Failed to delete locale with id $id!",
        ) {
            eventPublisher.publishEvent(SysLocaleDeleted(id = id, code = po.code))
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val codes = dao.inSearchById(ids).map { it.code }.toSet()
        val count = super.batchDelete(ids)
        log.debug("Batch delete of locales: expected ${ids.size}, actually deleted $count.")
        if (count > 0) {
            eventPublisher.publishEvent(SysLocaleBatchDeleted(ids = ids, codes = codes))
        }
        return count
    }

}
