package io.kudos.ms.sys.core.tenant.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.core.tenant.model.po.SysTenantLocale


/**
 * Tenant-locale relationship service interface.
 *
 * @author K
 * @since 1.0.0
 */
interface ISysTenantLocaleService : IBaseCrudService<String, SysTenantLocale> {


    /**
     * Returns the locale code list for the given tenant.
     *
     * @param tenantId tenant id
     * @return locale code set
     * @author K
     * @since 1.0.0
     */
    fun getLocaleCodesByTenantId(tenantId: String): Set<String>

    /**
     * Returns the tenant id list for the given locale.
     *
     * @param localeCode locale code
     * @return tenant id set
     * @author K
     * @since 1.0.0
     */
    fun getTenantIdsByLocaleCode(localeCode: String): Set<String>

    /**
     * Batch binds the given locales to the tenant.
     *
     * @param tenantId tenant id
     * @param localeCodes locale code collection
     * @return number of bindings successfully created
     * @author K
     * @since 1.0.0
     */
    fun batchBind(tenantId: String, localeCodes: Collection<String>): Int

    /**
     * Unbinds the relationship between the tenant and the locale.
     *
     * @param tenantId tenant id
     * @param localeCode locale code
     * @return whether the unbind succeeded
     * @author K
     * @since 1.0.0
     */
    fun unbind(tenantId: String, localeCode: String): Boolean

    /**
     * Checks whether the relationship exists.
     *
     * @param tenantId tenant id
     * @param localeCode locale code
     * @return whether it exists
     * @author K
     * @since 1.0.0
     */
    fun exists(tenantId: String, localeCode: String): Boolean


}