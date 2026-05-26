package io.kudos.ms.sys.core.tenant.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.core.tenant.model.po.SysTenantLocale
import io.kudos.ms.sys.core.tenant.model.table.SysTenantLocales
import org.springframework.stereotype.Repository


/**
 * Tenant-locale relationship data access object.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class SysTenantLocaleDao : BaseCrudDao<String, SysTenantLocale, SysTenantLocales>() {


    /**
     * Find the corresponding locale codes by tenant id.
     *
     * @param tenantId tenant id
     * @return Set of locale codes
     */
    fun searchLocaleCodesByTenantId(tenantId: String): Set<String> {
        val criteria = Criteria(SysTenantLocale::tenantId eq tenantId)
        return searchProperty(criteria, SysTenantLocale::localeCode).toSet()
    }

    /**
     * Find the corresponding tenant ids by locale code.
     *
     * @param localeCode locale code
     * @return Set of tenant ids
     */
    fun searchTenantIdsByLocaleCode(localeCode: String): Set<String> {
        val criteria = Criteria(SysTenantLocale::localeCode eq localeCode)
        return searchProperty(criteria, SysTenantLocale::tenantId).toSet()
    }

    /**
     * Check whether the relationship exists.
     *
     * @param tenantId tenant id
     * @param localeCode locale code
     * @return whether it exists
     */
    fun exists(tenantId: String, localeCode: String): Boolean {
        val criteria = Criteria.and(
            SysTenantLocale::tenantId eq tenantId,
            SysTenantLocale::localeCode eq localeCode
        )
        return count(criteria) > 0
    }

    /**
     * Delete the relationship by tenant id and locale code.
     *
     * @param tenantId tenant id
     * @param localeCode locale code
     * @return number of deleted rows
     */
    fun deleteByTenantIdAndLocaleCode(tenantId: String, localeCode: String): Int {
        val criteria = Criteria.and(
            SysTenantLocale::tenantId eq tenantId,
            SysTenantLocale::localeCode eq localeCode
        )
        return batchDeleteCriteria(criteria)
    }


}