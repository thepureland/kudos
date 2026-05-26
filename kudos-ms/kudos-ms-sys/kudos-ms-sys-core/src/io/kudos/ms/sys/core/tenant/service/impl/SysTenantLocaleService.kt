package io.kudos.ms.sys.core.tenant.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.tenant.dao.SysTenantLocaleDao
import io.kudos.ms.sys.core.tenant.model.po.SysTenantLocale
import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantLocaleService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * Tenant-locale relation service.
 *
 * @author K
 * @since 1.0.0
 */
@Service
@Transactional
open class SysTenantLocaleService(
    dao: SysTenantLocaleDao
) : BaseCrudService<String, SysTenantLocale, SysTenantLocaleDao>(dao), ISysTenantLocaleService {

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getLocaleCodesByTenantId(tenantId: String): Set<String> = dao.searchLocaleCodesByTenantId(tenantId)

    @Transactional(readOnly = true)
    override fun getTenantIdsByLocaleCode(localeCode: String): Set<String> = dao.searchTenantIdsByLocaleCode(localeCode)

    @Transactional
    override fun batchBind(tenantId: String, localeCodes: Collection<String>): Int {
        if (localeCodes.isEmpty()) return 0

        // One SELECT for existing relations, then a single batchInsert for the diff — collapses the original N+1 into 2 statements.
        val existing = dao.searchLocaleCodesByTenantId(tenantId)
        val newLocaleCodes = localeCodes.toSet() - existing
        if (newLocaleCodes.isEmpty()) {
            log.debug("Batch bind tenant=$tenantId to ${localeCodes.size} locales: all already exist, nothing to insert.")
            return 0
        }
        val relations = newLocaleCodes.map {
            SysTenantLocale {
                this.tenantId = tenantId
                this.localeCode = it
            }
        }
        dao.batchInsert(relations)
        log.debug("Batch bind tenant=$tenantId to ${localeCodes.size} locales: successfully inserted ${newLocaleCodes.size}.")
        return newLocaleCodes.size
    }

    @Transactional
    override fun unbind(tenantId: String, localeCode: String): Boolean {
        val count = dao.deleteByTenantIdAndLocaleCode(tenantId, localeCode)
        val success = count > 0
        if (success) {
            log.debug("Unbound tenant=$tenantId from locale=$localeCode.")
        } else {
            log.warn("Failed to unbind tenant=$tenantId from locale=$localeCode: relation does not exist.")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun exists(tenantId: String, localeCode: String): Boolean = dao.exists(tenantId, localeCode)
}
